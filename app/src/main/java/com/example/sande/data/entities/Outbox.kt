package com.example.sande.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "outbox", indices = [Index("createdAt"), Index("synced")])
data class Outbox(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val synced: Boolean = false,
    /** Explicit sync state; distinguishes "queued locally" from "server accepted". */
    val syncState: String = "LOCAL_PENDING",
    /** Set when a conflict needs human review. */
    val conflictReason: String? = null
)
