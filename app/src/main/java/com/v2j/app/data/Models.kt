package com.v2j.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * Per-entry polishing state.
 *  RAW       not yet polished (offline at 完成, or key missing)
 *  POLISHING a polish call is in flight (reset to RAW on app start — crash recovery)
 *  POLISHED  polishedText is ready
 *  FAILED    a polish call returned an error; retryable
 */
enum class PolishStatus { RAW, POLISHING, POLISHED, FAILED }

/**
 * One spoken reflection. createdAt decides which ISO week / day it belongs to and never changes.
 * Deletion is soft (deletedAt set) so the owning week can be re-rendered without the entry
 * before the tombstone is physically purged.
 */
@Entity(tableName = "entry")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val rawText: String,
    val polishedText: String? = null,
    val polishStatus: PolishStatus = PolishStatus.RAW,
    val syncDirty: Boolean = true,
    val lastSyncedAt: Long? = null,
    val deletedAt: Long? = null
)

/** The single in-progress textbox draft (always row id = 0). Persisted so nothing is lost. */
@Entity(tableName = "draft")
data class Draft(
    @PrimaryKey val id: Int = 0,
    val text: String,
    val updatedAt: Long
)

class Converters {
    @TypeConverter
    fun fromStatus(value: PolishStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): PolishStatus = PolishStatus.valueOf(value)
}
