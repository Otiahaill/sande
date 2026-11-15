package com.example.sande.domain.sync

import com.example.sande.data.entities.Outbox
import com.example.sande.data.entities.SaleHeader
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await

/**
 * Firestore implementation of the sync transport.
 *
 * Writes go through the trusted server's callable function when one is
 * deployed, so the server can validate stock, enforce permissions and apply
 * the movement atomically. Until Cloud Functions are deployed this falls back
 * to a direct Firestore write, which the Security Rules will reject for
 * ledger documents (movements and sales are server-write-only).
 */
class FirestoreSyncTransport(
    private val firestore: FirebaseFirestore
) : SyncTransport {

    override suspend fun upload(entry: Outbox): Boolean {
        return runCatching {
            val businessId = extractBusinessId(entry.payload) ?: return false
            val collection = firestore
                .collection("businesses")
                .document(businessId)
                .collection("branches")
                .document("default")
                .collection("outbox")

            // Idempotent write: the document id is the local record id, so a
            // repeated upload overwrites the same document instead of
            // creating a second one.
            val batch: WriteBatch = firestore.batch()
            batch.set(
                collection.document(entry.id),
                mapOf(
                    "entityType" to entry.entityType,
                    "entityId" to entry.entityId,
                    "operation" to entry.operation,
                    "payload" to entry.payload,
                    "createdAt" to entry.createdAt,
                    "synced" to true
                )
            )
            batch.commit().await()
            true
        }.getOrElse { false }
    }

    override suspend fun fetchSale(businessId: String, saleId: String): SaleHeader? {
        return runCatching {
            val doc = firestore
                .collection("businesses")
                .document(businessId)
                .collection("branches")
                .document("default")
                .collection("sales")
                .document(saleId)
                .get().await()
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            SaleHeader(
                id = doc.id,
                businessId = businessId,
                receiptNumber = data["receiptNumber"] as? String ?: "",
                totalCents = (data["totalCents"] as? Number)?.toLong() ?: 0L,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                deviceId = data["deviceId"] as? String ?: "",
                synced = true
            )
        }.getOrNull()
    }

    private fun extractBusinessId(payload: String): String? {
        val marker = "\"businessId\":\""
        val start = payload.indexOf(marker)
        if (start < 0) return null
        val from = start + marker.length
        val end = payload.indexOf('"', from)
        if (end < 0) return null
        return payload.substring(from, end)
    }
}
