package com.antigravity.pampastarshooter.core.model

import com.antigravity.pampastarshooter.core.content.CampaignNodeDef
import com.antigravity.pampastarshooter.core.content.CampaignSectorDef
import com.antigravity.pampastarshooter.core.content.CardRarity
import com.antigravity.pampastarshooter.core.content.DefaultGameContent
import com.antigravity.pampastarshooter.core.content.GameContentBundle
import com.antigravity.pampastarshooter.core.content.MissionMetric
import com.antigravity.pampastarshooter.core.content.UpgradeCategory
import kotlinx.serialization.SerialName
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
enum class RunMode {
    Campaign,
    Endless,
}

@Serializable
sealed class RunObjective {
    abstract val target: Int

    @Serializable
    @SerialName("reach_wave")
    data class ReachWave(
        override val target: Int,
    ) : RunObjective()

    @Serializable
    @SerialName("kill_count")
    data class KillCount(
        override val target: Int,
    ) : RunObjective()

    @Serializable
    @SerialName("earn_credits")
    data class EarnCredits(
        override val target: Int,
    ) : RunObjective()

    @Serializable
    @SerialName("defeat_boss")
    data class DefeatBoss(
        override val target: Int = 1,
    ) : RunObjective()
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
data class CampaignState(
    val completedNodeIds: Set<String> = emptySet(),
    val endlessUnlocked: Boolean = false,
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
    val version: Int = CURRENT_PROFILE_VERSION,
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
    val campaignState: CampaignState = CampaignState(),
    val unlockedPerkIds: Set<String> = emptySet(),
    val equippedPerkIds: List<String> = emptyList(),
    val activeMissions: List<MissionState> = emptyList(),
)

@Serializable
data class RunConfig(
    val shipId: String,
    val seed: Long,
    val mode: RunMode = RunMode.Campaign,
    val campaignNodeId: String? = null,
    val forcedBiomeId: String? = null,
    val objective: RunObjective? = null,
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
    val success: Boolean,
    val campaignNodeId: String? = null,
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
    val unlockedPerkIds: Set<String>,
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
    val hasShield: Boolean,
    val hasMine: Boolean,
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

data class ObjectiveSnapshot(
    val label: String,
    val progress: Int,
    val target: Int,
    val progressLabel: String,
    val completed: Boolean,
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
    val modeLabel: String = "",
    val objective: ObjectiveSnapshot? = null,
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

const val CURRENT_PROFILE_VERSION = 2

fun rankXpRequirement(rank: Int): Int = 30 + (rank - 1) * 18

fun PlayerProfile.applyRunResult(
    result: RunResult,
    content: GameContentBundle,
): PlayerProfile {
    val current = normalizeForContent(content)
    var newCredits = current.credits + result.creditsEarned
    var newArchiveXp = current.archiveXp + result.archiveXpEarned
    var newArchiveRank = current.archiveRank

    while (newArchiveXp >= rankXpRequirement(newArchiveRank)) {
        newArchiveXp -= rankXpRequirement(newArchiveRank)
        newArchiveRank++
    }

    val updatedCampaignState = current.campaignState.let { state ->
        if (!result.success || result.campaignNodeId == null) {
            state
        } else {
            val node = content.findCampaignNode(result.campaignNodeId)
            state.copy(
                completedNodeIds = if (node == null) state.completedNodeIds else state.completedNodeIds + node.id,
                endlessUnlocked = state.endlessUnlocked || (node?.unlockEndless == true),
            )
        }
    }
    val unlockedShipsFromResult = if (result.success) result.unlockedShipIds else emptySet()
    val unlockedPerksFromResult = if (result.success) result.unlockedPerkIds else emptySet()

    val updatedUnlocks = current.unlockTree.copy(
        unlockedShipIds = current.unlockTree.unlockedShipIds + unlockedShipsFromResult,
        codexEnemyIds = current.unlockTree.codexEnemyIds + result.discoveredEnemyIds,
        codexBiomeIds = current.unlockTree.codexBiomeIds + result.discoveredBiomeIds,
    )

    val existingMissionIds = current.activeMissions.map { it.id }.toSet()
    val normalizedMissionCursor = maxOf(
        updatedUnlocks.nextMetaMissionIndex,
        content.metaMissionPool.indexOfLast { it.id in existingMissionIds } + 1,
    ).coerceAtLeast(0)
    val seededMissions = if (current.activeMissions.isEmpty()) {
        seedMetaMissions(
            content = content,
            completedIds = updatedUnlocks.completedMetaMissionIds,
            startingCursor = normalizedMissionCursor,
            slots = 3,
        ).first
    } else {
        current.activeMissions
    }

    var updatedMissions = seededMissions.map { mission ->
        val progressValue = when (mission.metric) {
            MissionMetric.Runs -> current.totalRuns + 1
            MissionMetric.Kills -> current.totalKills + result.kills
            MissionMetric.Waves -> maxOf(current.bestWave, result.waveReached)
            MissionMetric.Bosses -> current.totalBosses + result.bossesDefeated
            MissionMetric.Credits -> current.totalCreditsEarned + result.creditsEarned
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

    return current.copy(
        version = CURRENT_PROFILE_VERSION,
        credits = newCredits,
        archiveXp = newArchiveXp,
        archiveRank = newArchiveRank,
        bestWave = maxOf(current.bestWave, result.waveReached),
        bestScore = maxOf(current.bestScore, result.score),
        totalRuns = current.totalRuns + 1,
        totalKills = current.totalKills + result.kills,
        totalBosses = current.totalBosses + result.bossesDefeated,
        totalCreditsEarned = current.totalCreditsEarned + result.creditsEarned,
        unlockTree = updatedUnlockState,
        campaignState = updatedCampaignState,
        unlockedPerkIds = current.unlockedPerkIds + unlockedPerksFromResult,
        activeMissions = updatedMissions,
    ).normalizeForContent(content)
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

fun RunObjective.progressValue(
    wave: Int,
    kills: Int,
    credits: Int,
    bosses: Int,
): Int = when (this) {
    is RunObjective.ReachWave -> wave
    is RunObjective.KillCount -> kills
    is RunObjective.EarnCredits -> credits
    is RunObjective.DefeatBoss -> bosses
}

fun RunObjective.displayLabel(): String = when (this) {
    is RunObjective.ReachWave -> "Reach wave $target"
    is RunObjective.KillCount -> "Eliminate $target enemies"
    is RunObjective.EarnCredits -> "Bank $target credits"
    is RunObjective.DefeatBoss -> "Defeat the sector boss"
}

fun RunObjective.progressLabel(progress: Int): String = when (this) {
    is RunObjective.DefeatBoss -> "${progress.coerceAtMost(target)}/$target boss"
    else -> "${progress.coerceAtMost(target)}/$target"
}

fun CampaignNodeDef.hasRewards(): Boolean =
    rewardShipIds.isNotEmpty() ||
        rewardPerkIds.isNotEmpty() ||
        rewardModifierIds.isNotEmpty() ||
        unlockEndless

fun GameContentBundle.allCampaignNodes(): List<CampaignNodeDef> = campaignSectors.flatMap { it.nodes }

fun GameContentBundle.findCampaignNode(id: String): CampaignNodeDef? =
    allCampaignNodes().firstOrNull { it.id == id }

fun GameContentBundle.findCampaignSector(nodeId: String): CampaignSectorDef? =
    campaignSectors.firstOrNull { sector -> sector.nodes.any { it.id == nodeId } }

fun PlayerProfile.currentCampaignNode(content: GameContentBundle): CampaignNodeDef? =
    content.allCampaignNodes().firstOrNull { it.id !in campaignState.completedNodeIds }

fun PlayerProfile.currentCampaignSector(content: GameContentBundle): CampaignSectorDef? =
    currentCampaignNode(content)?.let { node -> content.findCampaignSector(node.id) }

fun PlayerProfile.nextRewardCampaignNode(content: GameContentBundle): CampaignNodeDef? =
    content.allCampaignNodes().firstOrNull { node ->
        node.id !in campaignState.completedNodeIds && node.hasRewards()
    }

fun PlayerProfile.unlockedRunModifierIds(content: GameContentBundle): Set<String> =
    content.allCampaignNodes()
        .filter { it.id in campaignState.completedNodeIds }
        .flatMap { it.rewardModifierIds }
        .toSet()

fun PlayerProfile.isPermanentModuleAvailable(module: com.antigravity.pampastarshooter.core.content.PermanentModuleDef): Boolean =
    archiveRank >= module.unlockArchiveRank || (unlockTree.permanentModules[module.id] ?: 0) > 0

fun PlayerProfile.normalizeForContent(content: GameContentBundle): PlayerProfile {
    val migrated = if (version < CURRENT_PROFILE_VERSION) migrateLegacyProgression(content) else this
    val rewardedNodes = content.allCampaignNodes().filter { it.id in migrated.campaignState.completedNodeIds }
    val rewardedShips = rewardedNodes.flatMap { it.rewardShipIds }.toSet() + DefaultGameContent.ShipStriker
    val rewardedPerks = rewardedNodes.flatMap { it.rewardPerkIds }.toSet()
    val contentModuleIds = content.permanentModules.mapTo(mutableSetOf()) { it.id }
    val normalizedModuleLevels = migrated.unlockTree.permanentModules
        .filter { (id, level) -> id in contentModuleIds && level > 0 }
        .toMutableMap()
    content.permanentModules
        .filter(migrated::isPermanentModuleAvailable)
        .forEach { module ->
            normalizedModuleLevels[module.id] = migrated.unlockTree.permanentModules[module.id] ?: 0
        }
    val normalizedUnlockTree = migrated.unlockTree.copy(
        unlockedShipIds = migrated.unlockTree.unlockedShipIds + rewardedShips,
        permanentModules = normalizedModuleLevels,
    )
    val normalizedPerks = migrated.unlockedPerkIds + rewardedPerks
    val normalizedEquippedPerks = migrated.equippedPerkIds
        .filter { it in normalizedPerks }
        .distinct()
        .take(2)
    val selectedShip = migrated.selectedShipId.takeIf { it in normalizedUnlockTree.unlockedShipIds }
        ?: normalizedUnlockTree.unlockedShipIds.firstOrNull()
        ?: DefaultGameContent.ShipStriker

    return migrated.copy(
        version = CURRENT_PROFILE_VERSION,
        selectedShipId = selectedShip,
        unlockTree = normalizedUnlockTree,
        unlockedPerkIds = normalizedPerks,
        equippedPerkIds = normalizedEquippedPerks,
    )
}

fun PlayerProfile.migrateLegacyProgression(content: GameContentBundle): PlayerProfile {
    val completedSectorCount = when {
        archiveRank >= 6 || bestWave >= 12 -> 3
        DefaultGameContent.ShipSpecter in unlockTree.unlockedShipIds || bestWave >= 8 || totalBosses >= 3 -> 2
        DefaultGameContent.ShipWarden in unlockTree.unlockedShipIds || bestWave >= 4 -> 1
        else -> 0
    }
    val completedNodeIds = content.campaignSectors
        .take(completedSectorCount)
        .flatMap { it.nodes }
        .mapTo(mutableSetOf()) { it.id }
    val rewardedNodes = content.allCampaignNodes().filter { it.id in completedNodeIds }

    return copy(
        version = CURRENT_PROFILE_VERSION,
        campaignState = CampaignState(
            completedNodeIds = completedNodeIds,
            endlessUnlocked = completedSectorCount >= content.campaignSectors.size,
        ),
        unlockTree = unlockTree.copy(
            unlockedShipIds = unlockTree.unlockedShipIds + rewardedNodes.flatMap { it.rewardShipIds }.toSet() + DefaultGameContent.ShipStriker,
        ),
        unlockedPerkIds = unlockedPerkIds + rewardedNodes.flatMap { it.rewardPerkIds }.toSet(),
        equippedPerkIds = equippedPerkIds.distinct().take(2),
    )
}
