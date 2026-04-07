package com.antigravity.pampastarshooter.core.model

import com.antigravity.pampastarshooter.core.content.DefaultGameContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerProfileProgressionTest {
    private val content = DefaultGameContent.create()
    private val rankOneModuleIds = setOf("hull", "reactor", "thrusters", "magnet")

    @Test
    fun legacyProfilesMigrateCampaignCompletionAndRewards() {
        val legacyProfile = DefaultGameContent.starterProfile().copy(
            version = 1,
            archiveRank = 6,
            bestWave = 12,
            totalBosses = 4,
            unlockTree = DefaultGameContent.starterProfile().unlockTree.copy(
                unlockedShipIds = setOf(DefaultGameContent.ShipStriker),
            ),
        )

        val migrated = legacyProfile.normalizeForContent(content)
        val allNodeIds = content.campaignSectors.flatMap { sector -> sector.nodes }.mapTo(mutableSetOf()) { node -> node.id }

        assertEquals(CURRENT_PROFILE_VERSION, migrated.version)
        assertEquals(allNodeIds, migrated.campaignState.completedNodeIds)
        assertTrue(migrated.campaignState.endlessUnlocked)
        assertTrue(DefaultGameContent.ShipWarden in migrated.unlockTree.unlockedShipIds)
        assertTrue(DefaultGameContent.ShipSpecter in migrated.unlockTree.unlockedShipIds)
        assertEquals(
            setOf(
                "emergency_shield",
                "dash_coil",
                "pulse_primer",
                "magnet_surge",
                "regen_matrix",
                "draft_cache",
            ),
            migrated.unlockedPerkIds,
        )
    }

    @Test
    fun successfulFinalBossResultUnlocksEndlessAndRunModifiersWithoutDuplicates() {
        val node = content.campaignSectors.flatMap { it.nodes }.first { it.id == "sector3_boss" }
        val result = RunResult(
            config = RunConfig(
                shipId = DefaultGameContent.ShipStriker,
                seed = 7L,
                mode = RunMode.Campaign,
                campaignNodeId = node.id,
                forcedBiomeId = "amber_rift",
                objective = node.objective,
            ),
            shipId = DefaultGameContent.ShipStriker,
            success = true,
            campaignNodeId = node.id,
            durationSeconds = 180,
            waveReached = 8,
            biomeId = "amber_rift",
            kills = 120,
            bossesDefeated = 1,
            creditsEarned = 70,
            archiveXpEarned = 42,
            score = 2400,
            discoveredEnemyIds = setOf("boss"),
            discoveredBiomeIds = setOf("amber_rift"),
            unlockedShipIds = emptySet(),
            unlockedPerkIds = setOf("regen_matrix", "draft_cache"),
            historyEntry = RunHistoryEntry(
                id = 7L,
                shipId = DefaultGameContent.ShipStriker,
                startedAtEpochMs = 7L,
                durationSeconds = 180,
                waveReached = 8,
                biomeId = "amber_rift",
                kills = 120,
                bossesDefeated = 1,
                creditsEarned = 70,
                archiveXpEarned = 42,
                score = 2400,
                modifiers = emptyList(),
            ),
        )

        val once = DefaultGameContent.starterProfile().applyRunResult(result, content)
        val twice = once.applyRunResult(result, content)

        assertTrue(once.campaignState.endlessUnlocked)
        assertEquals(setOf("glass_drive", "long_burn"), once.unlockedRunModifierIds(content))
        assertEquals(setOf("regen_matrix", "draft_cache"), once.unlockedPerkIds)
        assertEquals(setOf(node.id), twice.campaignState.completedNodeIds)
        assertEquals(setOf("glass_drive", "long_burn"), twice.unlockedRunModifierIds(content))
        assertEquals(setOf("regen_matrix", "draft_cache"), twice.unlockedPerkIds)
    }

    @Test
    fun starterProfileOnlyUnlocksEarlyLabModules() {
        val normalized = DefaultGameContent.starterProfile().normalizeForContent(content)

        assertEquals(rankOneModuleIds, normalized.unlockTree.permanentModules.keys)
    }

    @Test
    fun normalizeForContentRemovesStaleZeroLevelModulesAndUnlocksByArchiveRank() {
        val staleProfile = DefaultGameContent.starterProfile().copy(
            unlockTree = DefaultGameContent.starterProfile().unlockTree.copy(
                permanentModules = mapOf(
                    "hull" to 0,
                    "reactor" to 0,
                    "bulwark" to 0,
                    "cache" to 0,
                    "reroll" to 0,
                ),
            ),
        )

        val normalizedStarter = staleProfile.normalizeForContent(content)
        val rankFourProfile = staleProfile.copy(archiveRank = 4).normalizeForContent(content)

        assertEquals(rankOneModuleIds, normalizedStarter.unlockTree.permanentModules.keys)
        assertTrue("bulwark" in rankFourProfile.unlockTree.permanentModules)
        assertTrue("capacitor" in rankFourProfile.unlockTree.permanentModules)
        assertTrue("ordnance" in rankFourProfile.unlockTree.permanentModules)
        assertTrue("reroll" in rankFourProfile.unlockTree.permanentModules)
    }
}
