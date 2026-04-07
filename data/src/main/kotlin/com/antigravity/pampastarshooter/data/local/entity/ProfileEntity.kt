package com.antigravity.pampastarshooter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.antigravity.pampastarshooter.core.model.CampaignState
import com.antigravity.pampastarshooter.core.model.MissionState
import com.antigravity.pampastarshooter.core.model.PlayerProfile
import com.antigravity.pampastarshooter.core.model.UnlockTree

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val version: Int,
    val credits: Int,
    val archiveXp: Int,
    val archiveRank: Int,
    val bestWave: Int,
    val bestScore: Int,
    val totalRuns: Int,
    val totalKills: Int,
    val totalBosses: Int,
    val totalCreditsEarned: Int,
    val selectedShipId: String,
    val tutorialSeen: Boolean,
    val unlockTree: UnlockTree,
    val campaignState: CampaignState,
    val unlockedPerkIds: Set<String>,
    val equippedPerkIds: List<String>,
    val activeMissions: List<MissionState>,
)

fun ProfileEntity.toDomain(): PlayerProfile = PlayerProfile(
    version = version,
    credits = credits,
    archiveXp = archiveXp,
    archiveRank = archiveRank,
    bestWave = bestWave,
    bestScore = bestScore,
    totalRuns = totalRuns,
    totalKills = totalKills,
    totalBosses = totalBosses,
    totalCreditsEarned = totalCreditsEarned,
    selectedShipId = selectedShipId,
    tutorialSeen = tutorialSeen,
    unlockTree = unlockTree,
    campaignState = campaignState,
    unlockedPerkIds = unlockedPerkIds,
    equippedPerkIds = equippedPerkIds,
    activeMissions = activeMissions,
)

fun PlayerProfile.toEntity(): ProfileEntity = ProfileEntity(
    version = version,
    credits = credits,
    archiveXp = archiveXp,
    archiveRank = archiveRank,
    bestWave = bestWave,
    bestScore = bestScore,
    totalRuns = totalRuns,
    totalKills = totalKills,
    totalBosses = totalBosses,
    totalCreditsEarned = totalCreditsEarned,
    selectedShipId = selectedShipId,
    tutorialSeen = tutorialSeen,
    unlockTree = unlockTree,
    campaignState = campaignState,
    unlockedPerkIds = unlockedPerkIds,
    equippedPerkIds = equippedPerkIds,
    activeMissions = activeMissions,
)
