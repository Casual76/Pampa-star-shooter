package com.antigravity.pampastarshooter.core.engine

import com.antigravity.pampastarshooter.core.content.ContentRepository
import com.antigravity.pampastarshooter.core.content.DefaultGameContent
import com.antigravity.pampastarshooter.core.model.InputSnapshot
import com.antigravity.pampastarshooter.core.model.PlayerProfile
import com.antigravity.pampastarshooter.core.model.RunConfig
import com.antigravity.pampastarshooter.core.model.RunMode
import com.antigravity.pampastarshooter.core.model.VisualEffectKind
import com.antigravity.pampastarshooter.core.model.applyRunResult
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PampaGameEngineTest {
    private val content = DefaultGameContent.create()
    private val profile = DefaultGameContent.starterProfile()

    @Test
    fun shieldAbsorbsDamageBeforeHull() {
        val world = newWorld()
        world.player.shield = 40f
        val hpBefore = world.player.hp

        world.applyPlayerDamage(22f)

        assertEquals(hpBefore, world.player.hp)
        assertEquals(18f, world.player.shield)
    }

    @Test
    fun earlyLevelUpOffersOffenseAndDefense() {
        val world = newWorld()

        world.levelUp()

        assertEquals(3, world.pendingUpgradeChoices.size)
        assertTrue(world.pendingUpgradeChoices.any { it.category.name == "Offense" })
        assertTrue(world.pendingUpgradeChoices.any { it.category.name == "Defense" })
    }

    @Test
    fun creditsNowDropAsPickupsInsteadOfInstantRewards() {
        val world = newWorld()
        val def = content.enemies.first { it.id == "gunner" }
        val enemy = EnemyState(
            runtimeId = 1L,
            def = def,
            position = world.player.position,
            hp = def.maxHp,
            maxHp = def.maxHp,
            speed = def.speed,
            radius = def.radius,
            color = def.color,
        )

        world.rewardEnemyKill(enemy)

        assertEquals(0, world.creditsEarned)
        assertTrue(world.pickups.any { it.kind == "credit" && it.value >= 1 })
    }

    @Test
    fun permanentModulesBoostOpeningStats() {
        val boostedProfile = DefaultGameContent.starterProfile().copy(
            unlockTree = DefaultGameContent.starterProfile().unlockTree.copy(
                permanentModules = mapOf(
                    "thrusters" to 1,
                    "targeting" to 2,
                    "magnet" to 2,
                    "capacitor" to 2,
                    "reserve" to 1,
                    "ordnance" to 1,
                ),
            ),
        )
        val world = newWorld(profile = boostedProfile)

        assertTrue(abs(world.player.moveSpeed - (content.ships.first().moveSpeed * 1.035f)) < 0.001f)
        assertEquals(content.ships.first().autoTargetRange + 36f, world.player.autoTargetRange)
        assertEquals(content.ships.first().magnetRadius + 36f, world.player.magnetRadius)
        assertTrue(world.player.pulseCooldownMax < content.ships.first().pulseCooldown)
        assertTrue(world.player.shieldStrength > 48f)
        assertTrue(world.player.minePowerBonus > 0f)
    }

    @Test
    fun equippedPerksStackWithCoreModulesOnOpeningStats() {
        val boostedProfile = DefaultGameContent.starterProfile().copy(
            unlockTree = DefaultGameContent.starterProfile().unlockTree.copy(
                permanentModules = DefaultGameContent.starterProfile().unlockTree.permanentModules + mapOf(
                    "thrusters" to 1,
                    "cache" to 1,
                    "reroll" to 1,
                ),
            ),
            unlockedPerkIds = setOf("dash_coil", "draft_cache"),
            equippedPerkIds = listOf("dash_coil", "draft_cache"),
        )
        val world = newWorld(profile = boostedProfile)

        assertTrue(abs(world.player.moveSpeed - (content.ships.first().moveSpeed * 1.035f * 1.06f)) < 0.001f)
        assertTrue(abs(world.player.scoreMultiplier - 1.13f) < 0.001f)
        assertEquals(3, world.rerollsRemaining)
    }

    @Test
    fun campaignNodesAutoCompleteEachObjectiveType() {
        listOf(
            "sector1_recon" to { world: GameWorld -> world.wave = 4 },
            "sector1_sweep" to { world: GameWorld -> world.kills = 55 },
            "sector1_salvage" to { world: GameWorld -> world.creditsEarned = 32 },
            "sector1_boss" to { world: GameWorld -> world.bossesDefeated = 1 },
        ).forEach { (nodeId, completeObjective) ->
            val world = newCampaignWorld(nodeId)

            completeObjective(world)
            world.checkObjectiveCompletion()

            val result = world.finalResult
            assertNotNull(result, nodeId)
            assertTrue(result.success, nodeId)
            assertEquals(nodeId, result.campaignNodeId)
        }
    }

    @Test
    fun campaignBossNodeUnlocksSectorRewardsOnSuccess() {
        val world = newCampaignWorld("sector1_boss")
        world.bossesDefeated = 1

        world.checkObjectiveCompletion()

        val result = world.finalResult
        assertNotNull(result)
        assertTrue(result.success)
        assertTrue(DefaultGameContent.ShipWarden in result.unlockedShipIds)
        assertEquals(setOf("emergency_shield", "dash_coil"), result.unlockedPerkIds)
    }

    @Test
    fun failedCampaignRunDoesNotUnlockBossRewardsOrAdvanceProgression() {
        val world = newCampaignWorld("sector1_boss")

        world.finishRun("Signal Lost")

        val result = world.finalResult
        assertNotNull(result)
        assertFalse(result.success)
        assertTrue(result.unlockedShipIds.isEmpty())
        assertTrue(result.unlockedPerkIds.isEmpty())

        val updated = profile.applyRunResult(result, content)
        assertTrue("sector1_boss" !in updated.campaignState.completedNodeIds)
        assertTrue(DefaultGameContent.ShipWarden !in updated.unlockTree.unlockedShipIds)
        assertTrue("dash_coil" !in updated.unlockedPerkIds)
    }

    @Test
    fun strikerSnapshotOnlyShowsUnlockedActiveAbilities() {
        val starterSnapshot = newWorld().snapshot.player
        assertNotNull(starterSnapshot)
        assertFalse(starterSnapshot.hasShield)
        assertFalse(starterSnapshot.hasMine)

        val shieldProfile = profile.copy(
            unlockedPerkIds = setOf("emergency_shield"),
            equippedPerkIds = listOf("emergency_shield"),
        )
        val shieldSnapshot = newWorld(profile = shieldProfile).snapshot.player
        assertNotNull(shieldSnapshot)
        assertTrue(shieldSnapshot.hasShield)
        assertFalse(shieldSnapshot.hasMine)

        val wardenSnapshot = GameWorld(
            content = content,
            config = RunConfig(
                shipId = DefaultGameContent.ShipWarden,
                seed = 24L,
            ),
            profile = profile.copy(selectedShipId = DefaultGameContent.ShipWarden),
            random = Random(24L),
        ).snapshot.player
        assertNotNull(wardenSnapshot)
        assertTrue(wardenSnapshot.hasShield)
        assertTrue(wardenSnapshot.hasMine)
    }

    @Test
    fun openingSpawnTimerStartsSlowerThanPreviousTuning() {
        val world = newWorld()
        assertTrue(abs(world.spawnTimer - 0.55f) < 0.001f)

        val denseWorld = GameWorld(
            content = content,
            config = RunConfig(
                shipId = DefaultGameContent.ShipStriker,
                seed = 84L,
                mode = RunMode.Endless,
                modifiers = listOf("dense_swarm"),
            ),
            profile = profile,
            random = Random(84L),
        )
        assertTrue(abs(denseWorld.spawnTimer - 0.495f) < 0.001f)
    }

    @Test
    fun dashAndPulseEmitVisualFxSnapshots() {
        val engine = PampaGameEngine(repository)
        engine.startRun(
            RunConfig(shipId = DefaultGameContent.ShipStriker, seed = 7L),
            profile,
        )

        engine.submitInput(InputSnapshot(movement = com.antigravity.pampastarshooter.core.model.Vector2(1f, 0f), dashPressed = true))
        engine.step(0.016f)
        val dashSnapshot = engine.currentSnapshot()
        assertTrue(dashSnapshot.visualFx.effects.any { it.kind == VisualEffectKind.Dash })
        assertTrue(dashSnapshot.visualFx.dashAlpha > 0f)

        engine.submitInput(InputSnapshot(pulsePressed = true))
        engine.step(0.016f)
        val pulseSnapshot = engine.currentSnapshot()
        assertTrue(pulseSnapshot.visualFx.effects.any { it.kind == VisualEffectKind.Pulse })
    }

    @Test
    fun wardenShieldAndMineEmitVisualFxSnapshots() {
        val engine = PampaGameEngine(repository)
        engine.startRun(
            RunConfig(shipId = DefaultGameContent.ShipWarden, seed = 9L),
            profile.copy(selectedShipId = DefaultGameContent.ShipWarden),
        )

        engine.submitInput(InputSnapshot(shieldPressed = true))
        engine.step(0.016f)
        val shieldSnapshot = engine.currentSnapshot()
        assertTrue(shieldSnapshot.visualFx.effects.any { it.kind == VisualEffectKind.Shield })
        assertTrue(shieldSnapshot.visualFx.shieldAlpha > 0f)

        engine.submitInput(InputSnapshot(minePressed = true))
        engine.step(0.016f)
        val mineSnapshot = engine.currentSnapshot()
        assertTrue(mineSnapshot.visualFx.effects.any { it.kind == VisualEffectKind.Mine })
    }

    @Test
    fun completedMetaMissionRotatesToNextPoolEntry() {
        val rotatedProfile = DefaultGameContent.starterProfile().copy(totalRuns = 4)
        val updated = rotatedProfile.applyRunResult(
            result = newWorld().run {
                finishRun("Archive")
                finalResult!!
            },
            content = content,
        )

        assertTrue(updated.unlockTree.completedMetaMissionIds.contains("meta_runs_5"))
        assertTrue(updated.activeMissions.any { it.id == "meta_boss_4" })
        assertTrue(updated.activeMissions.none { it.id == "meta_runs_5" && it.claimed })
    }

    private fun newWorld(profile: PlayerProfile = this.profile): GameWorld = GameWorld(
        content = content,
        config = RunConfig(
            shipId = DefaultGameContent.ShipStriker,
            seed = 42L,
        ),
        profile = profile,
        random = Random(42L),
    )

    private fun newCampaignWorld(
        nodeId: String,
        profile: PlayerProfile = this.profile,
    ): GameWorld {
        val sector = content.campaignSectors.first { campaignSector -> campaignSector.nodes.any { it.id == nodeId } }
        val node = sector.nodes.first { it.id == nodeId }
        return GameWorld(
            content = content,
            config = RunConfig(
                shipId = DefaultGameContent.ShipStriker,
                seed = 42L,
                mode = RunMode.Campaign,
                campaignNodeId = node.id,
                forcedBiomeId = sector.biomeId,
                objective = node.objective,
            ),
            profile = profile,
            random = Random(42L),
        )
    }

    private val repository = object : ContentRepository {
        override fun load() = content
    }
}
