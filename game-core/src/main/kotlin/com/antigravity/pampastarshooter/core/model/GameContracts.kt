package com.antigravity.pampastarshooter.core.model

import com.antigravity.pampastarshooter.core.content.CardRarity
import com.antigravity.pampastarshooter.core.content.GameContentBundle
import com.antigravity.pampastarshooter.core.content.MissionMetric
import com.antigravity.pampastarshooter.core.content.UpgradeCategory
import kotlinx.serialization.Serializable

@Serializable
enum class RunPhase {
    Idle,
    Running,
    ChoosingUpgrade,
    Paused,
    GameOver,
}

@Serializable
enum class GraphicsProfile {
    Auto,
    Clean,
    Dense,
}

@Serializable
data class HudLayout(
    val flipped: Boolean = false,
    val controlScale: Float = 1f,
    val actionColumnYOffset: Float = 0f,
)

@Serializable
data class GameSettings(
    val graphicsProfile: GraphicsProfile = GraphicsProfile.Auto,
    val lowVfx: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val musicVolume: Float = 0.55f,
    val sfxVolume: Float = 0.75f,
    val uiTextScale: Float = 1f,
    val highContrastHud: Boolean = false,
    val screenShakeEnabled: Boolean = true,
    val hudLayout: HudLayout = HudLayout(),
)

@Serializable
data class RunModifier(
    val id: String,
    val label: String,
    val description: String,
    val scoreMultiplier: Float,
)

@Serializable
data class UnlockTree(
    val unlockedShipIds: Set<String> = emptySet(),
    val permanentModules: Map<String, Int> = emptyMap(),
    val codexEnemyIds: Set<String> = emptySet(),
    val codexBiomeIds: Set<String> = emptySet(),
)

@Serializable
data class MissionState(
    val id: String,
    val label: String,
    val description: String,
    val target: Int,
    val rewardCredits: Int,
    val rewardArchiveXp: Int,
    val metric: MissionMetric,
    val progress: Int = 0,
    val completed: Boolean = false,
    val claimed: Boolean = false,
) {
    constructor(def: com.antigravity.pampastarshooter.core.content.MissionDef) : this(
        id = def.id,
        label = def.label,
        description = def.description,
        target = def.target,
        rewardCredits = def.rewardCredits,
        rewardArchiveXp = def.rewardArchiveXp,
        metric = def.metric,
    )
}

@Serializable
data class PlayerProfile(
    val version: Int = 1,
    val credits: Int = 0,
    val archiveXp: Int = 0,
    val archiveRank: Int = 1,
    val bestWave: Int = 0,
    val bestScore: Int = 0,
    val totalRuns: Int = 0,
    val totalKills: Int = 0,
    val totalBosses: Int = 0,
    val totalCreditsEarned: Int = 0,
    val selectedShipId: String,
    val tutorialSeen: Boolean = false,
    val unlockTree: UnlockTree = UnlockTree(),
    val activeMissions: List<MissionState> = emptyList(),
)

@Serializable
data class RunConfig(
    val shipId: String,
    val seed: Long,
    val modifiers: List<String> = emptyList(),
    val hudLayout: HudLayout = HudLayout(),
)

@Serializable
data class RunHistoryEntry(
    val id: Long,
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

@Serializable
data class RunResult(
    val config: RunConfig,
    val shipId: String,
    val durationSeconds: Int,
    val waveReached: Int,
    val biomeId: String,
    val kills: Int,
    val bossesDefeated: Int,
    val creditsEarned: Int,
    val archiveXpEarned: Int,
    val score: Int,
    val discoveredEnemyIds: Set<String>,
    val discoveredBiomeIds: Set<String>,
    val unlockedShipIds: Set<String>,
    val historyEntry: RunHistoryEntry,
)

data class InputSnapshot(
    val movement: Vector2 = Vector2.Zero,
    val aim: Vector2 = Vector2.Zero,
    val dashPressed: Boolean = false,
    val pulsePressed: Boolean = false,
    val shieldPressed: Boolean = false,
    val minePressed: Boolean = false,
    val pausePressed: Boolean = false,
    val resumePressed: Boolean = false,
    val rerollPressed: Boolean = false,
    val selectedUpgradeIndex: Int? = null,
)

data class PlayerSnapshot(
    val position: Vector2,
    val radius: Float,
    val hp: Float,
    val maxHp: Float,
    val shield: Float,
    val level: Int,
    val xp: Float,
    val xpToNext: Float,
    val dashCooldown: Float,
    val pulseCooldown: Float,
    val shieldCooldown: Float,
    val mineCooldown: Float,
    val activeShipColor: Long,
    val trailColor: Long,
)

data class EnemySnapshot(
    val id: Long,
    val kindId: String,
    val label: String,
    val position: Vector2,
    val radius: Float,
    val hpRatio: Float,
    val color: Long,
    val isElite: Boolean,
    val affixLabel: String?,
    val telegraphAlpha: Float,
)

data class ProjectileSnapshot(
    val id: Long,
    val position: Vector2,
    val radius: Float,
    val color: Long,
    val friendly: Boolean,
)

data class ParticleSnapshot(
    val id: Long,
    val position: Vector2,
    val radius: Float,
    val color: Long,
    val alpha: Float,
)

data class PickupSnapshot(
    val id: Long,
    val position: Vector2,
    val radius: Float,
    val color: Long,
    val kind: String,
)

data class WarningSnapshot(
    val title: String,
    val subtitle: String,
    val timeLeft: Float,
)

data class MissionSnapshot(
    val id: String,
    val label: String,
    val progress: Int,
    val target: Int,
    val completed: Boolean,
    val rewardCredits: Int,
)

data class UpgradeChoiceSnapshot(
    val id: String,
    val label: String,
    val description: String,
    val category: UpgradeCategory,
    val rarity: CardRarity,
    val currentStacks: Int,
    val maxStacks: Int,
)

sealed interface OverlaySnapshot {
    data class LevelUp(
        val level: Int,
        val rerollsRemaining: Int,
        val choices: List<UpgradeChoiceSnapshot>,
    ) : OverlaySnapshot

    data class GameOver(
        val title: String,
        val summary: String,
        val result: RunResult,
    ) : OverlaySnapshot

    data class Pause(
        val title: String,
        val subtitle: String,
    ) : OverlaySnapshot
}

data class HudSnapshot(
    val shipLabel: String,
    val passiveLabel: String,
    val wave: Int,
    val biomeLabel: String,
    val kills: Int,
    val score: Int,
    val credits: Int,
    val archiveXpProjected: Int,
    val activeEventLabel: String?,
)

data class FrameSnapshot(
    val phase: RunPhase = RunPhase.Idle,
    val bounds: RectBounds = RectBounds(0f, 0f, 1000f, 1000f),
    val elapsedSeconds: Float = 0f,
    val player: PlayerSnapshot? = null,
    val enemies: List<EnemySnapshot> = emptyList(),
    val projectiles: List<ProjectileSnapshot> = emptyList(),
    val particles: List<ParticleSnapshot> = emptyList(),
    val pickups: List<PickupSnapshot> = emptyList(),
    val hud: HudSnapshot = HudSnapshot("", "", 1, "", 0, 0, 0, 0, null),
    val warnings: List<WarningSnapshot> = emptyList(),
    val runMissions: List<MissionSnapshot> = emptyList(),
    val overlay: OverlaySnapshot? = null,
)

interface RendererBridge {
    fun render(snapshot: FrameSnapshot)
}

internal interface System {
    fun update(world: com.antigravity.pampastarshooter.core.engine.GameWorld, input: InputSnapshot, deltaSeconds: Float)
}

interface GameEngine {
    fun startRun(config: RunConfig, profile: PlayerProfile)
    fun submitInput(input: InputSnapshot)
    fun step(deltaSeconds: Float)
    fun currentSnapshot(): FrameSnapshot
    fun consumeLatestResult(): RunResult?
    fun pause()
    fun resume()
}

fun rankXpRequirement(rank: Int): Int = 30 + (rank - 1) * 18

fun PlayerProfile.applyRunResult(
    result: RunResult,
    content: GameContentBundle,
): PlayerProfile {
    var newCredits = credits + result.creditsEarned
    var newArchiveXp = archiveXp + result.archiveXpEarned
    var newArchiveRank = archiveRank

    while (newArchiveXp >= rankXpRequirement(newArchiveRank)) {
        newArchiveXp -= rankXpRequirement(newArchiveRank)
        newArchiveRank++
    }

    val updatedUnlocks = unlockTree.copy(
        unlockedShipIds = unlockTree.unlockedShipIds + result.unlockedShipIds,
        codexEnemyIds = unlockTree.codexEnemyIds + result.discoveredEnemyIds,
        codexBiomeIds = unlockTree.codexBiomeIds + result.discoveredBiomeIds,
    )

    var updatedMissions = activeMissions.map { mission ->
        val progressValue = when (mission.metric) {
            MissionMetric.Runs -> totalRuns + 1
            MissionMetric.Kills -> totalKills + result.kills
            MissionMetric.Waves -> maxOf(bestWave, result.waveReached)
            MissionMetric.Bosses -> totalBosses + result.bossesDefeated
            MissionMetric.Credits -> totalCreditsEarned + result.creditsEarned
        }
        mission.copy(
            progress = progressValue.coerceAtMost(mission.target),
            completed = mission.completed || progressValue >= mission.target,
        )
    }

    updatedMissions = updatedMissions.map { mission ->
        if (mission.completed && !mission.claimed) {
            newCredits += mission.rewardCredits
            newArchiveXp += mission.rewardArchiveXp
            mission.copy(claimed = true)
        } else {
            mission
        }
    }

    while (newArchiveXp >= rankXpRequirement(newArchiveRank)) {
        newArchiveXp -= rankXpRequirement(newArchiveRank)
        newArchiveRank++
    }

    if (updatedMissions.isEmpty()) {
        updatedMissions = content.metaMissionPool.take(3).map(::MissionState)
    }

    return copy(
        credits = newCredits,
        archiveXp = newArchiveXp,
        archiveRank = newArchiveRank,
        bestWave = maxOf(bestWave, result.waveReached),
        bestScore = maxOf(bestScore, result.score),
        totalRuns = totalRuns + 1,
        totalKills = totalKills + result.kills,
        totalBosses = totalBosses + result.bossesDefeated,
        totalCreditsEarned = totalCreditsEarned + result.creditsEarned,
        unlockTree = updatedUnlocks,
        activeMissions = updatedMissions,
    )
}
