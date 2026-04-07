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
    val requiredArchiveRank: Int = 1,
    val riskLabel: String = "",
)

@Serializable
data class UnlockTree(
    val unlockedShipIds: Set<String> = emptySet(),
    val permanentModules: Map<String, Int> = emptyMap(),
    val codexEnemyIds: Set<String> = emptySet(),
    val codexBiomeIds: Set<String> = emptySet(),
    val completedMetaMissionIds: Set<String> = emptySet(),
    val nextMetaMissionIndex: Int = 0,
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
    val shipId: String,
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

enum class VisualEffectKind {
    Dash,
    Pulse,
    Shield,
    Mine,
    Hit,
    Death,
    Pickup,
}

data class VisualEffectSnapshot(
    val id: Long,
    val kind: VisualEffectKind,
    val position: Vector2,
    val radius: Float,
    val color: Long,
    val alpha: Float,
    val rotationDegrees: Float = 0f,
    val direction: Vector2 = Vector2.Zero,
)

data class VisualFxSnapshot(
    val cameraShake: Float = 0f,
    val damageFlash: Float = 0f,
    val dashAlpha: Float = 0f,
    val shieldAlpha: Float = 0f,
    val overdriveAlpha: Float = 0f,
    val effects: List<VisualEffectSnapshot> = emptyList(),
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
    val visualFx: VisualFxSnapshot = VisualFxSnapshot(),
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

    val existingMissionIds = activeMissions.map { it.id }.toSet()
    val normalizedMissionCursor = maxOf(
        updatedUnlocks.nextMetaMissionIndex,
        content.metaMissionPool.indexOfLast { it.id in existingMissionIds } + 1,
    ).coerceAtLeast(0)
    val seededMissions = if (activeMissions.isEmpty()) {
        seedMetaMissions(
            content = content,
            completedIds = updatedUnlocks.completedMetaMissionIds,
            startingCursor = normalizedMissionCursor,
            slots = 3,
        ).first
    } else {
        activeMissions
    }

    var updatedMissions = seededMissions.map { mission ->
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

    val claimedMissionIds = updatedUnlocks.completedMetaMissionIds.toMutableSet()
    updatedMissions = updatedMissions.map { mission ->
        if (mission.completed && !mission.claimed) {
            newCredits += mission.rewardCredits
            newArchiveXp += mission.rewardArchiveXp
            claimedMissionIds += mission.id
            mission.copy(claimed = true)
        } else {
            mission
        }
    }

    val rotated = rotateMetaMissions(
        current = updatedMissions,
        content = content,
        completedIds = claimedMissionIds,
        startingCursor = normalizedMissionCursor,
    )
    updatedMissions = rotated.first
    val updatedUnlockState = updatedUnlocks.copy(
        completedMetaMissionIds = claimedMissionIds,
        nextMetaMissionIndex = rotated.second,
    )

    while (newArchiveXp >= rankXpRequirement(newArchiveRank)) {
        newArchiveXp -= rankXpRequirement(newArchiveRank)
        newArchiveRank++
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
        unlockTree = updatedUnlockState,
        activeMissions = updatedMissions,
    )
}

private fun seedMetaMissions(
    content: GameContentBundle,
    completedIds: Set<String>,
    startingCursor: Int,
    slots: Int,
): Pair<List<MissionState>, Int> {
    var cursor = startingCursor
    val picked = mutableListOf<MissionState>()
    repeat(slots) {
        val next = nextMissionFromPool(content.metaMissionPool, completedIds, picked.map { it.id }.toSet(), cursor)
        val nextMission = next.first
        if (nextMission != null) {
            picked += MissionState(nextMission)
            cursor = next.second
        }
    }
    return picked to cursor
}

private fun rotateMetaMissions(
    current: List<MissionState>,
    content: GameContentBundle,
    completedIds: Set<String>,
    startingCursor: Int,
): Pair<List<MissionState>, Int> {
    var cursor = startingCursor
    val rotated = current.toMutableList()
    current.forEachIndexed { index, mission ->
        if (!mission.claimed) return@forEachIndexed
        val activeIds = rotated.mapNotNullIndexed { activeIndex, activeMission ->
            if (activeIndex == index) null else activeMission.id
        }.toSet()
        val replacement = nextMissionFromPool(content.metaMissionPool, completedIds, activeIds, cursor)
        val replacementMission = replacement.first
        if (replacementMission != null) {
            rotated[index] = MissionState(replacementMission)
            cursor = replacement.second
        }
    }
    return rotated to cursor
}

private fun nextMissionFromPool(
    pool: List<com.antigravity.pampastarshooter.core.content.MissionDef>,
    completedIds: Set<String>,
    excludedIds: Set<String>,
    startingCursor: Int,
): Pair<com.antigravity.pampastarshooter.core.content.MissionDef?, Int> {
    var cursor = startingCursor.coerceAtLeast(0)
    while (cursor < pool.size) {
        val candidate = pool[cursor]
        cursor++
        if (candidate.id !in completedIds && candidate.id !in excludedIds) {
            return candidate to cursor
        }
    }
    return null to cursor
}

private inline fun <T, R : Any> List<T>.mapNotNullIndexed(transform: (Int, T) -> R?): List<R> {
    val result = ArrayList<R>(size)
    forEachIndexed { index, item ->
        transform(index, item)?.let(result::add)
    }
    return result
}
