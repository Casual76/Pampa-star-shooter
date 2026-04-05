package com.antigravity.pampastarshooter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.antigravity.pampastarshooter.core.model.RunHistoryEntry

@Entity(tableName = "run_history")
data class RunHistoryEntity(
    @PrimaryKey val id: Long,
    val shipId: String,
    val startedAtEpochMs: Long,
    val durationSeconds: Int,
    val waveReached: Int,
    val biomeId: String,
    val kills: Int,
    val bossesDefeated: Int,
    val creditsEarned: Int,
    val archiveXpEarned: Int,
    val score: Int,
    val modifiers: List<String>,
)

fun RunHistoryEntity.toDomain(): RunHistoryEntry = RunHistoryEntry(
    id = id,
    shipId = shipId,
    startedAtEpochMs = startedAtEpochMs,
    durationSeconds = durationSeconds,
    waveReached = waveReached,
    biomeId = biomeId,
    kills = kills,
    bossesDefeated = bossesDefeated,
    creditsEarned = creditsEarned,
    archiveXpEarned = archiveXpEarned,
    score = score,
    modifiers = modifiers,
)

fun RunHistoryEntry.toEntity(): RunHistoryEntity = RunHistoryEntity(
    id = id,
    shipId = shipId,
    startedAtEpochMs = startedAtEpochMs,
    durationSeconds = durationSeconds,
    waveReached = waveReached,
    biomeId = biomeId,
    kills = kills,
    bossesDefeated = bossesDefeated,
    creditsEarned = creditsEarned,
    archiveXpEarned = archiveXpEarned,
    score = score,
    modifiers = modifiers,
)

