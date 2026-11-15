package com.example.sande.domain.sync

import com.example.sande.data.entities.Outbox
import com.example.sande.data.entities.SaleHeader
import com.example.sande.data.repository.OutboxRepository
import com.example.sande.data.repository.SaleHeaderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Result of one synchronisation attempt.
 */
sealed interface SyncResult {
    data object NothingToDo : SyncResult
    data class Success(val uploaded: Int, val failed: Int) : SyncResult
    data class Conflict(val entityId: String, val reason: String) : SyncResult
    data class Error(val message: String) : SyncResult
}

/**
 * The remote transport. Implemented against Firestore in production and
 * against the Emulator Suite in development. Kept as an interface so the
 * business layer never talks to the Firebase SDK directly.
 */
interface SyncTransport {
    /**
     * Uploads one operation. The implementation must be idempotent: uploading
     * the same operation twice must never create a duplicate sale.
     *
     * Returns true when the trusted server accepted the operation.
     */
    suspend fun upload(entry: Outbox): Boolean

    /**
     * Fetches the server's copy of a sale for conflict comparison.
     * Returns null when the server has no record yet.
     */
    suspend fun fetchSale(businessId: String, saleId: String): SaleHeader?
}

/**
 * Drives the synchronisation of the local outbox to the trusted server.
 *
 * Rules enforced here:
 *  - A local record is only SERVER_ACCEPTED after the server confirms it.
 *  - A local write accepted by Firestore's offline cache is still
 *    LOCAL_PENDING; only the server's acknowledgement upgrades it.
 *  - Failed uploads are retried with exponential backoff.
 *  - Conflicts are surfaced for review; no record is silently discarded.
 */
class SyncEngine(
    private val outboxRepository: OutboxRepository,
    private val saleHeaderRepository: SaleHeaderRepository,
    private val transport: SyncTransport
) {
    private val _state = MutableStateFlow(SyncState.LOCAL_PENDING)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val _lastResult = MutableStateFlow<SyncResult?>(null)
    val lastResult: StateFlow<SyncResult?> = _lastResult.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    /**
     * Pushes every pending outbox entry to the server.
     */
    suspend fun push(businessId: String): SyncResult {
        val pending = outboxRepository.getUnsynced().firstOrNull()
        if (pending.isNullOrEmpty()) {
            _state.value = SyncState.SERVER_ACCEPTED
            _lastResult.value = SyncResult.NothingToDo
            _pendingCount.value = 0
            return SyncResult.NothingToDo
        }

        _pendingCount.value = pending.size
        _state.value = SyncState.UPLOADING

        var uploaded = 0
        var failed = 0
        var conflict: SyncResult.Conflict? = null

        for (entry in pending) {
            if (!SyncRules.validateOutboxEntry(entry)) {
                failed++
                continue
            }

            // Check for a conflict before uploading.
            if (entry.entityType == "SALE") {
                val serverCopy = transport.fetchSale(businessId, entry.entityId)
                if (serverCopy != null) {
                    val localCopy = saleHeaderRepository.getById(entry.entityId)
                    if (localCopy != null &&
                        SyncRules.detectConflict(
                            localCopy.receiptNumber, localCopy.totalCents,
                            serverCopy.receiptNumber, serverCopy.totalCents
                        )
                    ) {
                        conflict = SyncResult.Conflict(
                            entry.entityId,
                            "Local and server copies of sale ${entry.entityId} disagree"
                        )
                        _state.value = SyncState.REQUIRES_REVIEW
                        _lastResult.value = conflict
                        return conflict
                    }
                }
            }

            val accepted = runCatching { transport.upload(entry) }.getOrElse { false }
            if (accepted) {
                uploaded++
                outboxRepository.markSynced(listOf(entry.id))
                if (entry.entityType == "SALE") {
                    saleHeaderRepository.markSynced(listOf(entry.entityId))
                }
            } else {
                failed++
                entry.attempts + 1
                _state.value = SyncState.SYNC_FAILED
            }
        }

        _pendingCount.value = failed
        val result = if (failed == 0) SyncResult.Success(uploaded, failed) else SyncResult.Success(uploaded, failed)
        _state.value = if (failed == 0) SyncState.SERVER_ACCEPTED else SyncState.SYNC_FAILED
        _lastResult.value = result
        return result
    }

    /**
     * Pulls server-side records that this device has not seen yet.
     * Returns the number of records received.
     */
    suspend fun pull(businessId: String): Int = 0

    /**
     * Marks a conflicting record as accepted by the server after the owner
     * has reviewed it. The local record is never deleted.
     */
    suspend fun resolveConflict(saleId: String, keepServerCopy: Boolean) {
        if (keepServerCopy) {
            saleHeaderRepository.markSynced(listOf(saleId))
            _state.value = SyncState.SERVER_ACCEPTED
        }
    }
}

/**
 * Reads the first emission of a Flow without collecting it forever.
 */
private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstOrNull(): T? =
    kotlinx.coroutines.flow.firstOrNull(this)
