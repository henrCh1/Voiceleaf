package com.v2j.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Query("SELECT * FROM entry WHERE id = :id")
    suspend fun byId(id: Long): Entry?

    /** Field-level status update that will NOT resurrect a row soft-deleted mid-polish. */
    @Query("UPDATE entry SET polishStatus = :status, updatedAt = :ts WHERE id = :id AND deletedAt IS NULL")
    suspend fun updateStatus(id: Long, status: PolishStatus, ts: Long)

    @Query(
        "UPDATE entry SET polishedText = :text, polishStatus = :status, updatedAt = :ts " +
            "WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun updatePolished(id: Long, text: String, status: PolishStatus, ts: Long)

    /**
     * User hand-edit of the finished text. Field-level (not @Update) so a concurrent sync can't
     * clobber it: writes only polishedText/status/dirty/updatedAt, never touches createdAt
     * (which decides week/day membership) or rawText (kept as the original transcript).
     */
    @Query(
        "UPDATE entry SET polishedText = :text, polishStatus = 'POLISHED', syncDirty = 1, " +
            "updatedAt = :ts WHERE id = :id AND deletedAt IS NULL"
    )
    suspend fun editFinalText(id: Long, text: String, ts: Long)

    /** Soft-delete by id (field-level, marks dirty so the owning week is re-rendered on sync). */
    @Query("UPDATE entry SET deletedAt = :ts, syncDirty = 1, updatedAt = :ts WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDeleteById(id: Long, ts: Long)

    /** Live (non-deleted) entries of one week, for the on-screen list (newest first). */
    @Query(
        "SELECT * FROM entry WHERE deletedAt IS NULL AND createdAt >= :startMillis " +
            "AND createdAt < :endExclusiveMillis ORDER BY createdAt DESC"
    )
    fun observeWeekLive(startMillis: Long, endExclusiveMillis: Long): Flow<List<Entry>>

    /** All live (non-deleted) entries, newest first — for the History screen. */
    @Query("SELECT * FROM entry WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAllLive(): Flow<List<Entry>>

    /** All entries of one week including tombstones (used by sync for render + purge). */
    @Query(
        "SELECT * FROM entry WHERE createdAt >= :startMillis " +
            "AND createdAt < :endExclusiveMillis ORDER BY createdAt ASC"
    )
    suspend fun entriesInRange(startMillis: Long, endExclusiveMillis: Long): List<Entry>

    /** Everything with unsynced changes (new / edited / soft-deleted). */
    @Query("SELECT * FROM entry WHERE syncDirty = 1")
    suspend fun dirtyEntries(): List<Entry>

    /** Crash recovery: a process killed mid-polish leaves POLISHING zombies. */
    @Query("UPDATE entry SET polishStatus = 'RAW' WHERE polishStatus = 'POLISHING'")
    suspend fun resetPolishing()

    /**
     * Mark synced ONLY the given ids, and only if untouched since the snapshot timestamp.
     * The `updatedAt <= :snapshotAt` guard prevents clearing the dirty flag of an entry the
     * user edited/deleted while the upload was in flight.
     */
    @Query("UPDATE entry SET syncDirty = 0, lastSyncedAt = :ts WHERE id IN (:ids) AND updatedAt <= :snapshotAt")
    suspend fun markSyncedByIds(ids: List<Long>, ts: Long, snapshotAt: Long)

    /** Physically remove the given tombstone ids (only those accounted for in the snapshot). */
    @Query("DELETE FROM entry WHERE id IN (:ids)")
    suspend fun purgeByIds(ids: List<Long>)
}

@Dao
interface DraftDao {

    @Query("SELECT text FROM draft WHERE id = 0")
    suspend fun load(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: Draft)

    @Query("DELETE FROM draft WHERE id = 0")
    suspend fun clear()
}
