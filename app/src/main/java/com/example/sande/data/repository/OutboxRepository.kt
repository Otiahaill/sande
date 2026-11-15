package com.example.sande.data.repository

import com.example.sande.data.db.AppDatabase
import com.example.sande.data.daos.OutboxDao
import com.example.sande.data.entities.Outbox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class OutboxRepository constructor(
    private val database: AppDatabase
) {
    private val dao: OutboxDao = database.outboxDao()

    suspend fun insert(entityType: String, entityId: String, operation: String, payload: String): String {
        val id = UUID.randomUUID().toString()
        val outbox = Outbox(
            id = id,
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = payload,
            synced = false,
            syncState = "LOCAL_PENDING"
        )
        dao.insert(outbox)
        return id
    }

    /** All entries that still need to reach the server. */
    fun getUnsynced(): Flow<List<Outbox>> = dao.getUnsynced()

    /** Entries that failed and need a retry. */
    fun getFailed(): Flow<List<Outbox>> = dao.getAll().map { it.filter { e -> e.syncState == "SYNC_FAILED" } }

    /** Entries that need human review. */
    fun getNeedsReview(): Flow<List<Outbox>> = dao.getAll().map { it.filter { e -> e.syncState == "REQUIRES_REVIEW" } }

    /** Count of entries still pending. */
    fun getPendingCount(): Flow<Int> = dao.getUnsynced().map { it.size }

    suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)

    suspend fun markState(ids: List<String>, state: String) = dao.markSyncState(ids, state)

    suspend fun incrementAttempt(id: String) = dao.incrementAttempt(id)

    suspend fun markConflict(id: String, reason: String) = dao.markConflict(id, reason)

    suspend fun deleteSynced() = dao.deleteSynced()
}
