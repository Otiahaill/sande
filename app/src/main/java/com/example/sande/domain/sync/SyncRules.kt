package com.example.sande.domain.sync

import com.example.sande.data.entities.Movement
import com.example.sande.data.entities.MovementType
import com.example.sande.data.entities.Outbox
import com.example.sande.data.entities.SaleHeader
import java.util.UUID

/**
 * Pure business rules for the synchronisation layer.
 *
 * Kept free of Firebase and Room types so it can be unit tested on the JVM
 * and reused by the trusted server implementation.
 */
object SyncRules {

    /**
     * Applies a movement to a stock balance.
     * RECEIVE and ADJUST add, SALE subtracts, VOID restores.
     */
    fun applyMovement(balance: Long, type: MovementType, quantity: Long): Long =
        when (type) {
            MovementType.RECEIVE -> balance + quantity
            MovementType.ADJUST -> balance + quantity
            MovementType.SALE -> balance - quantity
            MovementType.VOID -> balance + quantity
        }

    /**
     * Decides what must happen when a movement would drive stock negative.
     */
    enum class NegativeStockPolicy { WARN, BLOCK, ALLOW }

    /**
     * Evaluates a sale against current stock under the given policy.
     *
     * WARN   - allow, but flag for review (default).
     * BLOCK  - refuse when stock would go negative.
     * ALLOW  - allow with no warning.
     */
    fun evaluateStock(
        currentBalance: Long,
        quantity: Long,
        policy: NegativeStockPolicy
    ): StockDecision {
        val resulting = currentBalance - quantity
        return when {
            resulting >= 0 -> StockDecision.ACCEPT
            policy == NegativeStockPolicy.BLOCK -> StockDecision.REJECT
            policy == NegativeStockPolicy.ALLOW -> StockDecision.ACCEPT
            else -> StockDecision.ACCEPT_WITH_WARNING
        }
    }

    enum class StockDecision { ACCEPT, ACCEPT_WITH_WARNING, REJECT }

    /**
     * Builds the deterministic idempotency key for an operation.
     *
     * Re-uploading the same local record always yields the same key, so the
     * trusted server can detect and ignore a duplicate submission.
     */
    fun idempotencyKey(businessId: String, entityType: String, entityId: String): String =
        "$businessId:$entityType:$entityId"

    /**
     * Detects a conflict between a local record and the server's copy.
     *
     * A conflict exists when both sides changed the same logical record but
     * disagree on its content. Both histories are preserved; the caller
     * flags the local record for review rather than overwriting either side.
     */
    fun detectConflict(
        localReceiptNumber: String,
        localTotalCents: Long,
        serverReceiptNumber: String,
        serverTotalCents: Long
    ): Boolean =
        localReceiptNumber != serverReceiptNumber || localTotalCents != serverTotalCents

    /**
     * Validates an outbox payload before it is sent.
     */
    fun validateOutboxEntry(entry: Outbox): Boolean =
        entry.entityId.isNotBlank() &&
            entry.entityType.isNotBlank() &&
            entry.operation.isNotBlank() &&
            entry.payload.isNotBlank()

    /**
     * Computes the retry delay for a failed upload using exponential backoff.
     */
    fun retryDelayMillis(attempts: Int): Long {
        val exponent = attempts.coerceIn(0, 6)
        return 1_000L * (1L shl exponent)
    }

    /**
     * Builds the receipt number for a sale.
     * Format: {prefix}-{deviceShort}-{counter}
     */
    fun buildReceiptNumber(prefix: String, deviceShort: String, counter: Int): String =
        "$prefix-$deviceShort-${counter.toString().padStart(5, '0')}"

    /**
     * Creates a new outbox entry for a sale.
     */
    fun saleOutboxEntry(
        businessId: String,
        sale: SaleHeader,
        movements: List<Movement>
    ): Outbox {
        val payload = buildString {
            append("{")
            append("\"type\":\"SALE\",")
            append("\"businessId\":\"$businessId\",")
            append("\"saleHeaderId\":\"${sale.id}\",")
            append("\"receiptNumber\":\"${sale.receiptNumber}\",")
            append("\"totalCents\":${sale.totalCents},")
            append("\"deviceId\":\"${sale.deviceId}\",")
            append("\"createdAt\":${sale.createdAt},")
            append("\"items\":[")
            append(movements.joinToString(",") { m ->
                "{\"productId\":\"${m.productId}\",\"quantity\":${m.quantity},\"unitPriceCents\":${m.unitPriceCents ?: 0},\"type\":\"${m.type.name}\"}"
            })
            append("]")
            append("}")
        }
        return Outbox(
            id = UUID.randomUUID().toString(),
            entityType = "SALE",
            entityId = sale.id,
            operation = "CREATE",
            payload = payload,
            createdAt = sale.createdAt,
            attempts = 0,
            synced = false
        )
    }
}
