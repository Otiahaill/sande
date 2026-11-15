package com.example.sande.data.daos

import androidx.room.*
import com.example.sande.data.entities.Outbox
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(outbox: Outbox)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(outbox: List<Outbox>)

    @Query("SELECT * FROM outbox WHERE synced = 0 ORDER BY createdAt ASC")
    fun getUnsynced(): Flow<List<Outbox>>

    @Query("SELECT * FROM outbox ORDER BY createdAt ASC")
    fun getAll(): Flow<List<Outbox>>

    @Query("SELECT COUNT(*) FROM outbox WHERE synced = 0")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM outbox WHERE entityId = :entityId LIMIT 1")
    suspend fun getByEntityId(entityId: String): Outbox?

    @Update
    suspend fun update(outbox: Outbox)

    @Query("UPDATE outbox SET synced = 1, syncState = 'SERVER_ACCEPTED' WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("UPDATE outbox SET syncState = :state WHERE id IN (:ids)")
    suspend fun markSyncState(ids: List<String>, state: String)

    @Query("UPDATE outbox SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempt(id: String)

    @Query("UPDATE outbox SET syncState = 'REQUIRES_REVIEW', conflictReason = :reason WHERE id = :id")
    suspend fun markConflict(id: String, reason: String)

    @Query("DELETE FROM outbox WHERE synced = 1")
    suspend fun deleteSynced()
}
