package com.antigravity.pampastarshooter.core.engine

import com.antigravity.pampastarshooter.core.content.*
import com.antigravity.pampastarshooter.core.model.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

internal interface Resettable {
    fun reset()
}

internal class ObjectPool<T : Resettable>(
    private val factory: () -> T,
) {
    private val free = ArrayDeque<T>()

    fun acquire(): T = if (free.isEmpty()) factory() else free.removeLast()

    fun release(instance: T) {
        instance.reset()
        free.addLast(instance)
    }
}

internal class MutableProjectile : Resettable {
    var id: Long = 0L
    var position: Vector2 = Vector2.Zero
    var velocity: Vector2 = Vector2.Zero
    var radius: Float = 0f
    var damage: Float = 0f
    var friendly: Boolean = false
    var color: Long = 0L
    var ttl: Float = 0f
    var remainingPierce: Int = 0
    var crit: Boolean = false

    override fun reset() {
        id = 0L
        position = Vector2.Zero
        velocity = Vector2.Zero
        radius = 0f
        damage = 0f
        friendly = false
        color = 0L
        ttl = 0f
        remainingPierce = 0
        crit = false
    }
}

internal class MutableParticle : Resettable {
    var id: Long = 0L
    var position: Vector2 = Vector2.Zero
    var velocity: Vector2 = Vector2.Zero
    var radius: Float = 0f
    var color: Long = 0L
    var ttl: Float = 0f
    var maxTtl: Float = 0f

    override fun reset() {
        id = 0L
        position = Vector2.Zero
        velocity = Vector2.Zero
        radius = 0f
        color = 0L
        ttl = 0f
        maxTtl = 0f
    }
}

internal data class ActiveMine(
    val id: Long,
    var position: Vector2,
    var radius: Float,
    var power: Float,
    var ttl: Float,
)

internal data class ActiveRunMission(
    val def: MissionDef,
    var progress: Int = 0,
    var completed: Boolean = false,
)

internal data class PickupState(
    val runtimeId: Long,
    val kind: String,
    var position: Vector2,
    val value: Int,
)

internal class PlayerState(
    val shipDef: ShipDef,
    position: Vector2,
    val weaponDef: WeaponDef,
) {
    var position: Vector2 = position
    var radius: Float = 18f
    var maxHp: Float = shipDef.maxHp
    var hp: Float = shipDef.maxHp
    var shield: Float = 0f
    var shieldStrength: Float = 48f
    var level: Int = 1
    var xp: Float = 0f
    var xpToNext: Float = 18f
    var moveSpeed: Float = shipDef.moveSpeed
    var damage: Float = shipDef.baseDamage
    var fireInterval: Float = shipDef.fireInterval
    var fireCooldown: Float = 0f
    var projectileSpeed: Float = shipDef.projectileSpeed
    var projectileRadius: Float = shipDef.projectileRadius
    var critChance: Float = shipDef.critChance
    var magnetRadius: Float = shipDef.magnetRadius
    var autoTargetRange: Float = shipDef.autoTargetRange
    var dashCooldownMax: Float = shipDef.dashCooldown
    var dashCooldown: Float = 0f
    var pulseCooldownMax: Float = shipDef.pulseCooldown
    var pulseCooldown: Float = 0f
    var shieldCooldownMax: Float = shipDef.shieldCooldown
    var shieldCooldown: Float = 0f
    var mineCooldownMax: Float = shipDef.mineCooldown
    var mineCooldown: Float = 0f
    var dashRemaining: Float = 0f
    var dashVector: Vector2 = Vector2.Zero
    var shieldRemaining: Float = 0f
    var invulnerability: Float = 0f
    var regenPerSecond: Float = 0f
    var pickupSpeedMultiplier: Float = 1f
    var multishot: Int = weaponDef.projectilesPerShot
    var spreadDegrees: Float = weaponDef.spreadDegrees
    var pierce: Int = 0
    var chain: Int = if (shipDef.id == DefaultGameContent.ShipSpecter) 1 else 0
    var rerolls: Int = 1
    var scoreMultiplier: Float = 1f
    var overdriveRemaining: Float = 0f
    var overdriveDuration: Float = 2.6f
    var overdriveBonus: Float = if (shipDef.id == DefaultGameContent.ShipStriker) 0.22f else 0.12f
    var pulseRadius: Float = if (shipDef.id == DefaultGameContent.ShipSpecter) 182f else 160f
    var pulseDamage: Float = if (shipDef.id == DefaultGameContent.ShipWarden) 66f else 64f
    var dashSpeedMultiplier: Float = 1f
    var mineRadiusBonus: Float = 0f
    var minePowerBonus: Float = 0f
    var hasShield: Boolean = shipDef.id == DefaultGameContent.ShipWarden
    var hasMine: Boolean = shipDef.id == DefaultGameContent.ShipWarden
    var shotColor: Long = shipDef.accentColor
}

internal data class EnemyState(
    val runtimeId: Long,
    val def: EnemyDef,
    var position: Vector2,
    var hp: Float,
    var maxHp: Float,
    var speed: Float,
    var radius: Float,
    var color: Long,
    var shootCooldown: Float = 0f,
    var telegraphRemaining: Float = 0f,
    var chargeRemaining: Float = 0f,
    var eliteAffix: AffixDef? = null,
)

internal class GameWorld(
    val content: GameContentBundle,
    val config: RunConfig,
    val profile: PlayerProfile,
    val random: Random,
) {
    val bounds = RectBounds(0f, 0f, 1000f, 1000f)
    private val projectilePool = ObjectPool(::MutableProjectile)
    private val particlePool = ObjectPool(::MutableParticle)
    private var nextRuntimeId = 1L

    val shipDef = content.ships.first { it.id == config.shipId }
    val weaponDef = when (shipDef.id) {
        DefaultGameContent.ShipWarden -> content.weapons.first { it.id == "piercer" }
        DefaultGameContent.ShipSpecter -> content.weapons.first { it.id == "scatter" }
        else -> content.weapons.first { it.id == "blaster" }
    }
    val player = PlayerState(shipDef, bounds.center, weaponDef)
    val enemies = mutableListOf<EnemyState>()
    val friendlyProjectiles = mutableListOf<MutableProjectile>()
    val enemyProjectiles = mutableListOf<MutableProjectile>()
    val particles = mutableListOf<MutableParticle>()
    val pickups = mutableListOf<PickupState>()
    val mines = mutableListOf<ActiveMine>()
    val runMissions = content.runMissionPool.shuffled(random).take(3).map(::ActiveRunMission).toMutableList()
    val upgradeStacks = mutableMapOf<String, Int>()
    val discoveredEnemyIds = mutableSetOf<String>()
    val discoveredBiomeIds = mutableSetOf<String>()
    val queuedEnemySpawns = mutableListOf<String>()
    val selectedModifiers = content.runModifiers.filter { it.id in config.modifiers }

    var phase: RunPhase = RunPhase.Running
    var startedAtEpochMs: Long = java.lang.System.currentTimeMillis()
    var elapsedSeconds: Float = 0f
    var wave: Int = 1
    var waveElapsed: Float = 0f
    var spawnTimer: Float = 0.4f
    var activeBiomeIndex: Int = 0
    var activeBiome: BiomeDef = content.biomes.first()
    var activeEvent: WorldEventDef? = null
    var activeEventRemaining: Float = 0f
    var warningTitle: String? = "Boot Sequence"
    var warningSubtitle: String? = "Sistema pronto. Naviga il settore con movimenti precisi."
    var warningTime: Float = 2.6f
    var bossSpawnedThisWave: Boolean = false
    var minibossSpawnedThisWave: Boolean = false
    var kills: Int = 0
    var bossesDefeated: Int = 0
    var creditsEarned: Int = 0
    var missionArchiveBonus: Int = 0
    var score: Int = 0
    var projectedArchiveXp: Int = 0
    var pendingUpgradeChoices: List<UpgradeCardDef> = emptyList()
    var rerollsRemaining: Int = 1
    var snapshot: FrameSnapshot = FrameSnapshot()
    var finalResult: RunResult? = null

    init {
        discoveredBiomeIds += activeBiome.id
        val hullBonus = moduleLevel("hull") * 10f
        val reactorBonus = moduleLevel("reactor") * 0.05f
        val shieldBonus = moduleLevel("bulwark") * 12f
        val rerollBonus = moduleLevel("reroll")
        val cacheBonus = moduleLevel("cache") * 0.03f
        val thrusterLevel = moduleLevel("thrusters")
        val targetingLevel = moduleLevel("targeting")
        val magnetLevel = moduleLevel("magnet")
        val capacitorLevel = moduleLevel("capacitor")
        val reserveLevel = moduleLevel("reserve")
        val overdriveLevel = moduleLevel("overdrive")
        val ordnanceLevel = moduleLevel("ordnance")

        player.maxHp += hullBonus
        player.hp = player.maxHp
        player.damage *= 1f + reactorBonus
        player.fireInterval *= max(0.65f, 1f - reactorBonus)
        player.shieldStrength += shieldBonus
        player.rerolls += rerollBonus
        player.scoreMultiplier += cacheBonus
        player.moveSpeed *= 1f + thrusterLevel * 0.035f
        player.dashSpeedMultiplier += thrusterLevel * 0.04f
        player.autoTargetRange += targetingLevel * 18f
        player.projectileSpeed += targetingLevel * 20f
        player.magnetRadius += magnetLevel * 18f
        player.pickupSpeedMultiplier += magnetLevel * 0.08f
        player.pulseDamage *= 1f + capacitorLevel * 0.08f
        player.pulseCooldownMax = max(3.4f, player.pulseCooldownMax * (1f - capacitorLevel * 0.05f))
        player.shieldStrength += reserveLevel * 10f
        player.shieldCooldownMax = max(6.6f, player.shieldCooldownMax * (1f - reserveLevel * 0.04f))
        player.overdriveBonus += overdriveLevel * 0.04f
        player.overdriveDuration += overdriveLevel * 0.18f
        player.mineRadiusBonus += ordnanceLevel * 6f
        player.minePowerBonus += ordnanceLevel * 10f
        player.mineCooldownMax = max(5.4f, player.mineCooldownMax * (1f - ordnanceLevel * 0.05f))
        rerollsRemaining = player.rerolls

        selectedModifiers.forEach { modifier ->
            player.scoreMultiplier *= modifier.scoreMultiplier
            when (modifier.id) {
                "dense_swarm" -> spawnTimer *= 0.85f
                "glass_drive" -> {
                    player.damage *= 1.18f
                    player.maxHp *= 0.9f
                    player.hp = player.maxHp
                }
            }
        }

        rebuildSnapshot()
    }

    fun nextId(): Long = nextRuntimeId++

    fun moduleLevel(id: String): Int = profile.unlockTree.permanentModules[id] ?: 0

    fun calculateScore(): Int {
        val raw = kills * 14 + wave * 96 + creditsEarned * 5 + bossesDefeated * 160
        return (raw * player.scoreMultiplier).roundToInt()
    }

    fun calculateProjectedArchiveXp(): Int {
        return (kills * 0.55f + wave * 6f + bossesDefeated * 18f + missionArchiveBonus + elapsedSeconds / 14f).roundToInt()
    }

    fun recycleProjectile(projectile: MutableProjectile) {
        projectilePool.release(projectile)
    }

    fun recycleParticle(particle: MutableParticle) {
        particlePool.release(particle)
    }

    fun spawnParticles(
        position: Vector2,
        color: Long,
        count: Int,
        speed: Float = 140f,
        ttlRange: ClosedFloatingPointRange<Float> = 0.2f..0.45f,
    ) {
        repeat(count) {
            val particle = particlePool.acquire()
            val angle = random.nextFloat() * (PI.toFloat() * 2f)
            val magnitude = speed * (0.35f + random.nextFloat() * 0.75f)
            particle.id = nextId()
            particle.position = position
            particle.velocity = Vector2(cos(angle) * magnitude, sin(angle) * magnitude)
            particle.radius = 2f + random.nextFloat() * 4f
            particle.color = color
            particle.ttl = ttlRange.start + random.nextFloat() * (ttlRange.endInclusive - ttlRange.start)
            particle.maxTtl = particle.ttl
            particles += particle
        }
    }

    fun spawnPickup(position: Vector2, kind: String, value: Int) {
        pickups += PickupState(
            runtimeId = nextId(),
            kind = kind,
            position = position,
            value = value,
        )
    }

    fun spawnFriendlyProjectile(target: EnemyState) {
        val direction = (target.position - player.position).normalized()
        val spreadRadians = player.spreadDegrees / 180f * PI.toFloat()
        val overdriveFactor = if (player.overdriveRemaining > 0f) 1f + player.overdriveBonus else 1f
        repeat(player.multishot) { index ->
            val projectile = projectilePool.acquire()
            val centered = index - (player.multishot - 1) * 0.5f
            val spread = centered * spreadRadians
            val angle = atan2(direction.y, direction.x) + spread
            projectile.id = nextId()
            projectile.position = player.position
            projectile.velocity = Vector2(cos(angle), sin(angle)) * player.projectileSpeed
            projectile.radius = player.projectileRadius
            projectile.damage = player.damage * overdriveFactor * if (random.nextFloat() < player.critChance) 1.75f else 1f
            projectile.friendly = true
            projectile.color = player.shotColor
            projectile.ttl = 1.25f
            projectile.remainingPierce = player.pierce
            projectile.crit = projectile.damage > player.damage * overdriveFactor * 1.3f
            friendlyProjectiles += projectile
        }
        spawnParticles(player.position, player.shipDef.accentColor, count = 2, speed = 64f, ttlRange = 0.08f..0.18f)
    }

    fun spawnEnemyProjectile(
        origin: Vector2,
        angle: Float,
        speed: Float,
        radius: Float,
        damage: Float,
        color: Long,
    ) {
        val projectile = projectilePool.acquire()
        projectile.id = nextId()
        projectile.position = origin
        projectile.velocity = Vector2(cos(angle), sin(angle)) * speed
        projectile.radius = radius
        projectile.damage = damage
        projectile.friendly = false
        projectile.color = color
        projectile.ttl = 4.8f
        enemyProjectiles += projectile
    }

    fun spawnEnemy(kindId: String, forceElite: Boolean = false) {
        val def = content.enemies.first { it.id == kindId }
        val spawnPos = randomSpawnPoint()
        val eliteChance = when {
            wave >= 8 -> 0.16f + wave * 0.014f
            wave >= 4 -> 0.08f + wave * 0.012f
            else -> 0f
        }
        val affix = if (!def.isBoss && !def.isMiniBoss && (forceElite || random.nextFloat() < eliteChance)) {
            content.affixes.random(random)
        } else {
            null
        }
        val hpMultiplier = affix?.hpMultiplier ?: 1f
        val speedMultiplier = (affix?.speedMultiplier ?: 1f) * (activeEvent?.enemySpeedMultiplier ?: 1f)
        val waveHpMultiplier = 1f + wave * 0.13f + if (wave >= 6) 0.08f else 0f
        val waveSpeedMultiplier = 1f + min(0.22f, (wave - 1).coerceAtLeast(0) * 0.018f)
        val state = EnemyState(
            runtimeId = nextId(),
            def = def,
            position = spawnPos,
            hp = def.maxHp * waveHpMultiplier * hpMultiplier,
            maxHp = def.maxHp * waveHpMultiplier * hpMultiplier,
            speed = def.speed * speedMultiplier * waveSpeedMultiplier,
            radius = def.radius,
            color = affix?.color ?: def.color,
            shootCooldown = def.shootCooldown?.let { it * (0.6f + random.nextFloat() * 0.45f) } ?: 0f,
            eliteAffix = affix,
        )
        enemies += state
        discoveredEnemyIds += def.id
    }

    private fun randomSpawnPoint(): Vector2 {
        return when (random.nextInt(4)) {
            0 -> Vector2(random.nextFloat() * bounds.width, bounds.top - 24f)
            1 -> Vector2(bounds.right + 24f, random.nextFloat() * bounds.height)
            2 -> Vector2(random.nextFloat() * bounds.width, bounds.bottom + 24f)
            else -> Vector2(bounds.left - 24f, random.nextFloat() * bounds.height)
        }
    }

    fun nearestEnemy(maxDistance: Float = player.autoTargetRange): EnemyState? {
        var best: EnemyState? = null
        var bestDistance = maxDistance
        enemies.forEach { enemy ->
            val distance = player.position.distanceTo(enemy.position)
            if (distance < bestDistance) {
                best = enemy
                bestDistance = distance
            }
        }
        return best
    }

    fun applyPlayerDamage(amount: Float) {
        if (player.invulnerability > 0f || phase == RunPhase.GameOver) return
        var remaining = amount
        if (player.shield > 0f) {
            val absorbed = min(player.shield, remaining)
            player.shield -= absorbed
            remaining -= absorbed
        }
        if (remaining <= 0f) return
        player.hp -= remaining
        player.invulnerability = 0.4f
        spawnParticles(player.position, 0xFFFF7A8E, 8, speed = 180f)
        if (player.hp <= 0f) {
            player.hp = 0f
            finishRun("Signal Lost")
        }
    }

    fun rewardEnemyKill(enemy: EnemyState) {
        kills++
        val baseCredits = enemy.def.creditReward
        val affixCredits = enemy.eliteAffix?.creditMultiplier ?: 1f
        val eventCredits = activeEvent?.creditMultiplier ?: 1f
        val credits = (baseCredits * affixCredits * eventCredits).roundToInt().coerceAtLeast(0)
        spawnPickup(enemy.position, "xp", enemy.def.xpReward)
        if (credits > 0) {
            spawnPickup(enemy.position, "credit", credits)
        }
        if (enemy.def.isMiniBoss) {
            spawnPickup(enemy.position, "credit", 4)
        }
        if (enemy.def.isBoss) {
            bossesDefeated++
            warningTitle = "Boss Down"
            warningSubtitle = "${enemy.def.label} neutralizzato."
            warningTime = 2f
            spawnPickup(enemy.position, "credit", 8)
        }
        if (enemy.def.splitInto != null) {
            repeat(2) {
                queuedEnemySpawns += enemy.def.splitInto
            }
        }
        score = calculateScore()
        projectedArchiveXp = calculateProjectedArchiveXp()
        updateRunMissionProgress()
        spawnParticles(enemy.position, enemy.color, count = if (enemy.def.isBoss) 24 else 10, speed = 190f)
    }

    fun updateRunMissionProgress() {
        runMissions.forEach { mission ->
            mission.progress = when (mission.def.metric) {
                MissionMetric.Runs -> 1
                MissionMetric.Kills -> kills
                MissionMetric.Waves -> wave
                MissionMetric.Bosses -> bossesDefeated
                MissionMetric.Credits -> creditsEarned
            }.coerceAtMost(mission.def.target)

            if (!mission.completed && mission.progress >= mission.def.target) {
                mission.completed = true
                creditsEarned += mission.def.rewardCredits
                missionArchiveBonus += mission.def.rewardArchiveXp
                warningTitle = "Run Mission"
                warningSubtitle = "${mission.def.label} completata: +${mission.def.rewardCredits} crediti."
                warningTime = 2.2f
            }
        }
        score = calculateScore()
        projectedArchiveXp = calculateProjectedArchiveXp()
    }

    fun levelUp() {
        player.level++
        player.xp -= player.xpToNext
        player.xpToNext = player.xpToNext * 1.34f + 8f
        pendingUpgradeChoices = buildUpgradeChoices()
        phase = RunPhase.ChoosingUpgrade
        warningTitle = "Upgrade Available"
        warningSubtitle = "Scegli il prossimo modulo per la run."
        warningTime = 1.6f
        rebuildSnapshot()
    }

    private fun buildUpgradeChoices(): List<UpgradeCardDef> {
        val eligible = content.upgradeCards.filter { card ->
            val stack = upgradeStacks[card.id] ?: 0
            stack < card.maxStacks &&
                !(card.primaryStat == UpgradeStat.ShieldUnlock && player.hasShield) &&
                !(card.primaryStat == UpgradeStat.MineUnlock && player.hasMine)
        }.toMutableList()
        if (eligible.isEmpty()) return emptyList()

        val picks = mutableListOf<UpgradeCardDef>()
        if (player.level <= 3) {
            pickWeightedCard(eligible, UpgradeCategory.Offense)?.let { picks += it; eligible.remove(it) }
            pickWeightedCard(eligible, UpgradeCategory.Defense)?.let { picks += it; eligible.remove(it) }
        }
        while (picks.size < 3 && eligible.isNotEmpty()) {
            val preferred = if (shipDef.id == DefaultGameContent.ShipSpecter && random.nextFloat() < 0.45f) {
                UpgradeCategory.Synergy
            } else {
                null
            }
            val next = pickWeightedCard(eligible, preferred)
            if (next != null) {
                picks += next
                eligible.remove(next)
            } else {
                picks += eligible.removeAt(random.nextInt(eligible.size))
            }
        }
        return picks
    }

    private fun pickWeightedCard(
        source: List<UpgradeCardDef>,
        category: UpgradeCategory?,
    ): UpgradeCardDef? {
        val pool = if (category == null) source else source.filter { it.category == category }
        if (pool.isEmpty()) return null
        val total = pool.sumOf { adjustedCardWeight(it).toDouble() }
        var roll = random.nextDouble() * total
        pool.forEach { card ->
            roll -= adjustedCardWeight(card).toDouble()
            if (roll <= 0.0) return card
        }
        return pool.last()
    }

    private fun adjustedCardWeight(card: UpgradeCardDef): Float {
        val rarityWeight = when (card.rarity) {
            CardRarity.Common -> 1f
            CardRarity.Rare -> 0.82f
            CardRarity.Epic -> 0.58f
        }
        val specterBonus = if (shipDef.id == DefaultGameContent.ShipSpecter && card.category == UpgradeCategory.Synergy) 1.3f else 1f
        return card.baseWeight * rarityWeight * specterBonus
    }

    fun applyUpgrade(index: Int) {
        val choice = pendingUpgradeChoices.getOrNull(index) ?: return
        applyUpgrade(choice)
        pendingUpgradeChoices = emptyList()
        phase = RunPhase.Running
        rebuildSnapshot()
    }

    fun rerollUpgrades() {
        if (rerollsRemaining <= 0) return
        rerollsRemaining--
        pendingUpgradeChoices = buildUpgradeChoices()
        rebuildSnapshot()
    }

    private fun applyUpgrade(card: UpgradeCardDef) {
        upgradeStacks[card.id] = (upgradeStacks[card.id] ?: 0) + 1
        applyStat(card.primaryStat, card.primaryAmount)
        card.secondaryStat?.let { applyStat(it, card.secondaryAmount) }
        warningTitle = "Module Synced"
        warningSubtitle = card.label
        warningTime = 1.4f
    }

    private fun applyStat(stat: UpgradeStat, amount: Float) {
        when (stat) {
            UpgradeStat.Damage -> player.damage *= 1f + amount
            UpgradeStat.FireRate -> player.fireInterval = max(0.12f, player.fireInterval * (1f + amount))
            UpgradeStat.Speed -> player.moveSpeed *= 1f + amount
            UpgradeStat.Magnet -> player.magnetRadius += amount
            UpgradeStat.MaxHp -> {
                player.maxHp += amount
                player.hp = min(player.maxHp, player.hp + amount)
            }
            UpgradeStat.Regen -> player.regenPerSecond += amount
            UpgradeStat.DashCooldown -> player.dashCooldownMax = max(1.5f, player.dashCooldownMax * (1f + amount))
            UpgradeStat.PulseCooldown -> player.pulseCooldownMax = max(2.8f, player.pulseCooldownMax * (1f + amount))
            UpgradeStat.ShieldStrength -> {
                player.hasShield = true
                player.shieldStrength += amount
                player.shield = max(player.shield, amount * 0.7f)
            }
            UpgradeStat.PulseRadius -> player.pulseRadius += amount
            UpgradeStat.PulseDamage -> player.pulseDamage *= 1f + amount
            UpgradeStat.CritChance -> player.critChance = min(0.65f, player.critChance + amount)
            UpgradeStat.MultiShot -> player.multishot = (player.multishot + amount.roundToInt()).coerceAtMost(5)
            UpgradeStat.Pierce -> player.pierce = (player.pierce + amount.roundToInt()).coerceAtMost(4)
            UpgradeStat.Chain -> player.chain = (player.chain + amount.roundToInt()).coerceAtMost(5)
            UpgradeStat.ShieldUnlock -> player.hasShield = true
            UpgradeStat.MineUnlock -> player.hasMine = true
            UpgradeStat.Reroll -> rerollsRemaining += amount.roundToInt()
            UpgradeStat.ScoreMultiplier -> player.scoreMultiplier += amount
        }
    }

    fun finishRun(title: String) {
        if (phase == RunPhase.GameOver) return
        phase = RunPhase.GameOver
        score = calculateScore()
        projectedArchiveXp = calculateProjectedArchiveXp()

        val unlockedShips = buildSet {
            if (wave >= 4) add(DefaultGameContent.ShipWarden)
            if (wave >= 8 || bossesDefeated >= 3) add(DefaultGameContent.ShipSpecter)
        } - profile.unlockTree.unlockedShipIds

        finalResult = RunResult(
            config = config,
            shipId = shipDef.id,
            durationSeconds = elapsedSeconds.roundToInt(),
            waveReached = wave,
            biomeId = activeBiome.id,
            kills = kills,
            bossesDefeated = bossesDefeated,
            creditsEarned = creditsEarned,
            archiveXpEarned = projectedArchiveXp,
            score = score,
            discoveredEnemyIds = discoveredEnemyIds.toSet(),
            discoveredBiomeIds = discoveredBiomeIds.toSet(),
            unlockedShipIds = unlockedShips,
            historyEntry = RunHistoryEntry(
                id = startedAtEpochMs,
                shipId = shipDef.id,
                startedAtEpochMs = startedAtEpochMs,
                durationSeconds = elapsedSeconds.roundToInt(),
                waveReached = wave,
                biomeId = activeBiome.id,
                kills = kills,
                bossesDefeated = bossesDefeated,
                creditsEarned = creditsEarned,
                archiveXpEarned = projectedArchiveXp,
                score = score,
                modifiers = config.modifiers,
            ),
        )
        warningTitle = title
        warningSubtitle = "Run archiviata. Score $score."
        warningTime = 3f
        rebuildSnapshot()
    }

    fun rebuildSnapshot() {
        score = calculateScore()
        projectedArchiveXp = calculateProjectedArchiveXp()
        snapshot = FrameSnapshot(
            phase = phase,
            bounds = bounds,
            elapsedSeconds = elapsedSeconds,
            player = PlayerSnapshot(
                position = player.position,
                radius = player.radius,
                hp = player.hp,
                maxHp = player.maxHp,
                shield = player.shield,
                level = player.level,
                xp = player.xp,
                xpToNext = player.xpToNext,
                dashCooldown = player.dashCooldown,
                pulseCooldown = player.pulseCooldown,
                shieldCooldown = player.shieldCooldown,
                mineCooldown = player.mineCooldown,
                activeShipColor = shipDef.accentColor,
                trailColor = shipDef.trailColor,
            ),
            enemies = enemies.map { enemy ->
                EnemySnapshot(
                    id = enemy.runtimeId,
                    kindId = enemy.def.id,
                    label = enemy.def.label,
                    position = enemy.position,
                    radius = enemy.radius,
                    hpRatio = (enemy.hp / enemy.maxHp).coerceIn(0f, 1f),
                    color = enemy.color,
                    isElite = enemy.eliteAffix != null,
                    affixLabel = enemy.eliteAffix?.label,
                    telegraphAlpha = (enemy.telegraphRemaining / 0.8f).coerceIn(0f, 1f),
                )
            },
            projectiles = (friendlyProjectiles + enemyProjectiles).map { projectile ->
                ProjectileSnapshot(
                    id = projectile.id,
                    position = projectile.position,
                    radius = projectile.radius,
                    color = projectile.color,
                    friendly = projectile.friendly,
                )
            },
            particles = particles.map { particle ->
                ParticleSnapshot(
                    id = particle.id,
                    position = particle.position,
                    radius = particle.radius,
                    color = particle.color,
                    alpha = if (particle.maxTtl == 0f) 0f else (particle.ttl / particle.maxTtl).coerceIn(0f, 1f),
                )
            },
            pickups = pickups.map { pickup ->
                PickupSnapshot(
                    id = pickup.runtimeId,
                    position = pickup.position,
                    radius = if (pickup.kind == "xp") 6f else 8f,
                    color = if (pickup.kind == "xp") 0xFF9FF2FF else 0xFFFFCF75,
                    kind = pickup.kind,
                )
            },
            hud = HudSnapshot(
                shipLabel = shipDef.label,
                passiveLabel = shipDef.passiveLabel,
                wave = wave,
                biomeLabel = activeBiome.label,
                kills = kills,
                score = score,
                credits = creditsEarned,
                archiveXpProjected = projectedArchiveXp,
                activeEventLabel = activeEvent?.label,
            ),
            warnings = listOfNotNull(
                if (warningTime > 0f && warningTitle != null && warningSubtitle != null) {
                    WarningSnapshot(warningTitle!!, warningSubtitle!!, warningTime)
                } else {
                    null
                },
            ),
            runMissions = runMissions.map { mission ->
                MissionSnapshot(
                    id = mission.def.id,
                    label = mission.def.label,
                    progress = mission.progress,
                    target = mission.def.target,
                    completed = mission.completed,
                    rewardCredits = mission.def.rewardCredits,
                )
            },
            overlay = when (phase) {
                RunPhase.ChoosingUpgrade -> OverlaySnapshot.LevelUp(
                    level = player.level,
                    rerollsRemaining = rerollsRemaining,
                    choices = pendingUpgradeChoices.map { choice ->
                        UpgradeChoiceSnapshot(
                            id = choice.id,
                            label = choice.label,
                            description = choice.description,
                            category = choice.category,
                            rarity = choice.rarity,
                            currentStacks = upgradeStacks[choice.id] ?: 0,
                            maxStacks = choice.maxStacks,
                        )
                    },
                )
                RunPhase.GameOver -> finalResult?.let { result ->
                    OverlaySnapshot.GameOver(
                        title = "Run Conclusa",
                        summary = "Ondata $wave, ${result.kills} nemici eliminati, +${result.creditsEarned} crediti.",
                        result = result,
                    )
                }
                RunPhase.Paused -> OverlaySnapshot.Pause(
                    title = "In Pausa",
                    subtitle = "Riposiziona i pollici e torna nel settore quando vuoi.",
                )
                else -> null
            },
        )
    }
}

private class TimingSystem : System {
    override fun update(world: GameWorld, input: InputSnapshot, deltaSeconds: Float) {
        world.elapsedSeconds += deltaSeconds
        world.waveElapsed += deltaSeconds
        world.warningTime = max(0f, world.warningTime - deltaSeconds)

        world.player.fireCooldown = max(0f, world.player.fireCooldown - deltaSeconds)
        world.player.dashCooldown = max(0f, world.player.dashCooldown - deltaSeconds)
        world.player.pulseCooldown = max(0f, world.player.pulseCooldown - deltaSeconds)
        world.player.shieldCooldown = max(0f, world.player.shieldCooldown - deltaSeconds)
        world.player.mineCooldown = max(0f, world.player.mineCooldown - deltaSeconds)
        world.player.dashRemaining = max(0f, world.player.dashRemaining - deltaSeconds)
        world.player.shieldRemaining = max(0f, world.player.shieldRemaining - deltaSeconds)
        world.player.invulnerability = max(0f, world.player.invulnerability - deltaSeconds)
        world.player.overdriveRemaining = max(0f, world.player.overdriveRemaining - deltaSeconds)

        if (world.player.regenPerSecond > 0f) {
            world.player.hp = min(world.player.maxHp, world.player.hp + world.player.regenPerSecond * deltaSeconds)
        }

        world.activeEventRemaining = max(0f, world.activeEventRemaining - deltaSeconds)
        if (world.activeEventRemaining <= 0f) {
            world.activeEvent = null
        }
    }
}

private class SpawnSystem : System {
    override fun update(world: GameWorld, input: InputSnapshot, deltaSeconds: Float) {
        val modifierSpawn = world.activeEvent?.spawnRateMultiplier ?: 1f
        val baseWaveDuration = if ("long_burn" in world.config.modifiers) 20.5f else 16.5f
        val nextSpawnDelay = max(0.26f, (1.02f - world.wave * 0.035f) / modifierSpawn)

        if (world.waveElapsed >= baseWaveDuration) {
            world.wave++
            world.waveElapsed = 0f
            world.spawnTimer = 0.35f
            world.bossSpawnedThisWave = false
            world.minibossSpawnedThisWave = false
            val biomeIndex = min((world.wave - 1) / 5, world.content.biomes.lastIndex)
            if (biomeIndex != world.activeBiomeIndex) {
                world.activeBiomeIndex = biomeIndex
                world.activeBiome = world.content.biomes[biomeIndex]
                world.discoveredBiomeIds += world.activeBiome.id
                world.warningTitle = world.activeBiome.label
                world.warningSubtitle = world.activeBiome.subtitle
                world.warningTime = 3.1f
            } else {
                world.warningTitle = "Wave ${world.wave}"
                world.warningSubtitle = "Il settore aumenta la pressione."
                world.warningTime = 1.8f
            }
            world.updateRunMissionProgress()
        }

        if (world.wave >= 3 && world.activeEvent == null && world.wave % 3 == 0 && world.waveElapsed < 0.3f) {
            val eventId = world.activeBiome.eventIds.random(world.random)
            world.activeEvent = world.content.worldEvents.first { it.id == eventId }
            world.activeEventRemaining = world.activeEvent!!.durationSeconds
            world.warningTitle = world.activeEvent!!.label
            world.warningSubtitle = world.activeEvent!!.description
            world.warningTime = 2.6f
        }

        if (world.wave % 5 == 0 && !world.bossSpawnedThisWave && world.enemies.none { it.def.isBoss }) {
            world.spawnEnemy(world.activeBiome.bossId)
            world.warningTitle = "Boss Signal"
            world.warningSubtitle = "Un picco energetico entra in arena."
            world.warningTime = 2.6f
            world.bossSpawnedThisWave = true
        } else if (world.wave % 4 == 0 && !world.minibossSpawnedThisWave && world.enemies.none { it.def.isMiniBoss }) {
            world.spawnEnemy("minotaur", forceElite = true)
            world.minibossSpawnedThisWave = true
        }

        world.spawnTimer -= deltaSeconds
        if (world.spawnTimer > 0f) return

        val densityBonus = if ("dense_swarm" in world.config.modifiers) 1 else 0
        val spawnCount = if (world.enemies.any { it.def.isBoss }) {
            1
        } else {
            1 +
                densityBonus +
                if (world.wave > 3 && world.random.nextFloat() < 0.45f) 1 else 0 +
                if (world.wave > 8 && world.random.nextFloat() < 0.22f) 1 else 0
        }
        repeat(spawnCount) {
            val pool = buildList {
                addAll(world.activeBiome.enemyPool)
                if (world.wave > 4) add("brute")
                if (world.wave > 6) add("sniper")
            }
            world.spawnEnemy(pool.random(world.random))
        }
        world.spawnTimer = nextSpawnDelay
    }
}

private class PlayerCombatSystem : System {
    override fun update(world: GameWorld, input: InputSnapshot, deltaSeconds: Float) {
        val player = world.player
        val movement = input.movement.clampLength(1f)
        val speedMultiplier = if (player.overdriveRemaining > 0f) 1.14f else 1f

        if (input.dashPressed && player.dashCooldown <= 0f) {
            val direction = if (movement.length() > 0.1f) movement.normalized() else {
                (world.nearestEnemy()?.let { player.position - it.position } ?: Vector2(1f, 0f)).normalized()
            }
            player.dashVector = direction
            player.dashRemaining = 0.22f
            player.dashCooldown = player.dashCooldownMax
            player.invulnerability = 0.24f
            player.overdriveRemaining = player.overdriveDuration
            world.spawnParticles(player.position, player.shipDef.accentColor, 10, speed = 260f)
        }

        if (player.dashRemaining > 0f) {
            player.position = world.bounds.clamp(player.position + player.dashVector * (760f * player.dashSpeedMultiplier * deltaSeconds), player.radius)
        } else {
            player.position = world.bounds.clamp(player.position + movement * (player.moveSpeed * speedMultiplier * deltaSeconds), player.radius)
        }

        if (input.pulsePressed && player.pulseCooldown <= 0f) {
            player.pulseCooldown = player.pulseCooldownMax
            val radius = player.pulseRadius
            val baseDamage = player.pulseDamage * if (player.overdriveRemaining > 0f) 1.15f else 1f
            val hits = world.enemies.toList()
            hits.forEach { enemy ->
                val distance = player.position.distanceTo(enemy.position)
                if (distance <= radius + enemy.radius) {
                    enemy.hp -= baseDamage * (1f - distance / (radius + enemy.radius) * 0.45f)
                    val push = (enemy.position - player.position).normalized() * (120f - min(100f, distance))
                    enemy.position += push * 0.15f
                    if (player.chain > 0) {
                        chainPulse(world, enemy, baseDamage * 0.45f, player.chain)
                    }
                }
            }
            world.spawnParticles(player.position, 0xFFAFB6FF, 18, speed = 220f, ttlRange = 0.15f..0.35f)
        }

        if (input.shieldPressed && player.hasShield && player.shieldCooldown <= 0f) {
            player.shieldCooldown = player.shieldCooldownMax
            player.shieldRemaining = 4.2f
            player.shield = max(player.shield, player.shieldStrength)
            world.spawnParticles(player.position, 0xFF96FFC6, 14, speed = 160f)
        }

        if (input.minePressed && player.hasMine && player.mineCooldown <= 0f) {
            player.mineCooldown = player.mineCooldownMax
            world.mines += ActiveMine(
                id = world.nextId(),
                position = player.position,
                radius = 104f + world.moduleLevel("bulwark") * 4f + player.mineRadiusBonus,
                power = 70f + world.moduleLevel("bulwark") * 5f + player.minePowerBonus,
                ttl = 1.4f,
            )
        }

        val target = world.nearestEnemy()
        if (target != null && player.fireCooldown <= 0f) {
            player.fireCooldown = max(0.12f, player.fireInterval / if (player.overdriveRemaining > 0f) 1.12f else 1f)
            world.spawnFriendlyProjectile(target)
        }
    }

    private fun chainPulse(world: GameWorld, source: EnemyState, damage: Float, jumps: Int) {
        var current = source
        repeat(jumps) {
            val next = world.enemies
                .filter { it.runtimeId != current.runtimeId && it.position.distanceTo(current.position) < 140f }
                .minByOrNull { it.position.distanceTo(current.position) } ?: return
            next.hp -= damage
            world.spawnParticles(next.position, 0xFFE4EAFF, 6, speed = 120f, ttlRange = 0.08f..0.16f)
            current = next
        }
    }
}

private class EnemySystem : System {
    override fun update(world: GameWorld, input: InputSnapshot, deltaSeconds: Float) {
        val player = world.player

        val enemyIterator = world.enemies.iterator()
        while (enemyIterator.hasNext()) {
            val enemy = enemyIterator.next()
            val toPlayer = player.position - enemy.position
            val distance = toPlayer.length()
            val direction = toPlayer.normalized()

            when (enemy.def.role) {
                "shooter" -> {
                    if (distance > 200f) {
                        enemy.position += direction * (enemy.speed * deltaSeconds)
                    } else if (distance < 130f) {
                        enemy.position -= direction * (enemy.speed * 0.7f * deltaSeconds)
                    }
                }
                else -> enemy.position += direction * (enemy.speed * deltaSeconds)
            }

            if (enemy.chargeRemaining > 0f) {
                enemy.position += direction * (180f * deltaSeconds)
                enemy.chargeRemaining = max(0f, enemy.chargeRemaining - deltaSeconds)
            }

            enemy.shootCooldown = max(0f, enemy.shootCooldown - deltaSeconds)
            enemy.telegraphRemaining = max(0f, enemy.telegraphRemaining - deltaSeconds)

            if (enemy.def.shootCooldown != null) {
                if (enemy.def.isBoss) {
                    handleBossAttack(world, enemy, direction, deltaSeconds)
                } else if (enemy.shootCooldown <= 0f && distance < 360f) {
                    world.spawnEnemyProjectile(
                        origin = enemy.position,
                        angle = atan2(direction.y, direction.x),
                        speed = enemy.def.projectileSpeed,
                        radius = 6f,
                        damage = 11f + world.wave * 0.45f + enemy.eliteAffix?.extraContactDamage.orZero(),
                        color = enemy.color,
                    )
                    enemy.shootCooldown = enemy.def.shootCooldown
                }
            }

            if (distance < enemy.radius + player.radius) {
                val glassMultiplier = if ("glass_drive" in world.config.modifiers) 1.16f else 1f
                val contactDamage = (enemy.def.contactDamage + enemy.eliteAffix?.extraContactDamage.orZero()) * (1f + world.wave * 0.045f)
                world.applyPlayerDamage(contactDamage * glassMultiplier)
                enemy.position -= direction * 18f
                if (player.shieldRemaining > 0f) {
                    enemy.hp -= 20f
                }
            }

            if (enemy.hp <= 0f) {
                world.rewardEnemyKill(enemy)
                enemyIterator.remove()
            }
        }

        updateProjectiles(world, deltaSeconds)
        updateMines(world, deltaSeconds)
    }

    private fun handleBossAttack(world: GameWorld, enemy: EnemyState, direction: Vector2, deltaSeconds: Float) {
        val patternId = enemy.def.bossPatternId ?: return
        val pattern = world.content.bossPatterns.first { it.id == patternId }
        if (enemy.telegraphRemaining > 0f) {
            if (enemy.telegraphRemaining <= deltaSeconds) {
                val baseAngle = atan2(direction.y, direction.x)
                repeat(pattern.burstCount) { index ->
                    val centered = index - (pattern.burstCount - 1) * 0.5f
                    val spread = centered * (pattern.spreadDegrees / 180f * PI.toFloat() / max(1, pattern.burstCount - 1))
                    world.spawnEnemyProjectile(
                        origin = enemy.position,
                        angle = baseAngle + spread,
                        speed = pattern.projectileSpeed,
                        radius = if (enemy.def.id == "hydra") 9f else 7f,
                        damage = 13f + world.wave * 1.0f,
                        color = enemy.color,
                    )
                }
                enemy.chargeRemaining = if (world.random.nextFloat() < 0.4f) 0.35f else 0f
            }
        } else if (enemy.shootCooldown <= 0f) {
            enemy.telegraphRemaining = pattern.telegraphSeconds
            enemy.shootCooldown = enemy.def.shootCooldown ?: 1.4f
        }
    }

    private fun updateProjectiles(world: GameWorld, deltaSeconds: Float) {
        val friendlyIterator = world.friendlyProjectiles.iterator()
        while (friendlyIterator.hasNext()) {
            val projectile = friendlyIterator.next()
            projectile.position += projectile.velocity * deltaSeconds
            projectile.ttl -= deltaSeconds

            var consumed = projectile.ttl <= 0f
            val enemyIterator = world.enemies.iterator()
            while (!consumed && enemyIterator.hasNext()) {
                val enemy = enemyIterator.next()
                if (projectile.position.distanceTo(enemy.position) <= projectile.radius + enemy.radius) {
                    enemy.hp -= projectile.damage
                    consumed = projectile.remainingPierce <= 0
                    projectile.remainingPierce--
                    world.spawnParticles(enemy.position, projectile.color, count = if (projectile.crit) 5 else 3, speed = 100f, ttlRange = 0.08f..0.16f)
                    if (enemy.hp <= 0f) {
                        world.rewardEnemyKill(enemy)
                        enemyIterator.remove()
                    }
                }
            }

            if (consumed || !inside(world.bounds, projectile.position, 40f)) {
                friendlyIterator.remove()
                world.recycleProjectile(projectile)
            }
        }

        val hostileIterator = world.enemyProjectiles.iterator()
        while (hostileIterator.hasNext()) {
            val projectile = hostileIterator.next()
            projectile.position += projectile.velocity * deltaSeconds
            projectile.ttl -= deltaSeconds
            val hitPlayer = projectile.position.distanceTo(world.player.position) <= projectile.radius + world.player.radius
            if (hitPlayer) {
                val glassMultiplier = if ("glass_drive" in world.config.modifiers) 1.16f else 1f
                world.applyPlayerDamage(projectile.damage * glassMultiplier)
            }
            if (hitPlayer || projectile.ttl <= 0f || !inside(world.bounds, projectile.position, 60f)) {
                hostileIterator.remove()
                world.recycleProjectile(projectile)
            }
        }
    }

    private fun updateMines(world: GameWorld, deltaSeconds: Float) {
        val mineIterator = world.mines.iterator()
        while (mineIterator.hasNext()) {
            val mine = mineIterator.next()
            mine.ttl -= deltaSeconds
            val triggered = world.enemies.any { enemy -> enemy.position.distanceTo(mine.position) < mine.radius * 0.72f }
            if (mine.ttl <= 0f || triggered) {
                world.enemies.toList().forEach { enemy ->
                    val distance = enemy.position.distanceTo(mine.position)
                    if (distance <= mine.radius + enemy.radius) {
                        enemy.hp -= mine.power * (1f - distance / (mine.radius + enemy.radius) * 0.5f)
                    }
                }
                world.spawnParticles(mine.position, 0xFFFFCF75, 20, speed = 220f, ttlRange = 0.12f..0.4f)
                mineIterator.remove()
            }
        }
    }

    private fun inside(bounds: RectBounds, position: Vector2, padding: Float): Boolean {
        return position.x in (bounds.left - padding)..(bounds.right + padding) &&
            position.y in (bounds.top - padding)..(bounds.bottom + padding)
    }
}

private class ProgressionSystem : System {
    override fun update(world: GameWorld, input: InputSnapshot, deltaSeconds: Float) {
        val pickupIterator = world.pickups.iterator()
        while (pickupIterator.hasNext()) {
            val pickup = pickupIterator.next()
            val distance = world.player.position.distanceTo(pickup.position)
            if (distance < world.player.magnetRadius) {
                val direction = (world.player.position - pickup.position).normalized()
                pickup.position += direction * (160f + pickup.value * 14f) * world.player.pickupSpeedMultiplier * deltaSeconds
            }
            if (distance < world.player.radius + 10f) {
                when (pickup.kind) {
                    "xp" -> {
                        world.player.xp += pickup.value
                        while (world.player.xp >= world.player.xpToNext) {
                            world.levelUp()
                            if (world.phase != RunPhase.Running) break
                        }
                    }
                    "credit" -> world.creditsEarned += pickup.value
                }
                pickupIterator.remove()
            }
        }
        world.updateRunMissionProgress()
    }
}

private class CleanupSystem : System {
    override fun update(world: GameWorld, input: InputSnapshot, deltaSeconds: Float) {
        val particleIterator = world.particles.iterator()
        while (particleIterator.hasNext()) {
            val particle = particleIterator.next()
            particle.position += particle.velocity * deltaSeconds
            particle.velocity = particle.velocity * 0.96f
            particle.ttl -= deltaSeconds
            if (particle.ttl <= 0f) {
                particleIterator.remove()
                world.recycleParticle(particle)
            }
        }
        if (world.queuedEnemySpawns.isNotEmpty()) {
            val pending = world.queuedEnemySpawns.toList()
            world.queuedEnemySpawns.clear()
            pending.forEach { world.spawnEnemy(it) }
        }
    }
}

class PampaGameEngine(
    private val contentRepository: ContentRepository,
) : GameEngine {
    private val systems: List<System> = listOf(
        TimingSystem(),
        SpawnSystem(),
        PlayerCombatSystem(),
        EnemySystem(),
        ProgressionSystem(),
        CleanupSystem(),
    )

    private var latestInput = InputSnapshot()
    private var world: GameWorld? = null
    private var latestResult: RunResult? = null

    override fun startRun(config: RunConfig, profile: PlayerProfile) {
        world = GameWorld(
            content = contentRepository.load(),
            config = config,
            profile = profile,
            random = Random(config.seed),
        )
        latestInput = InputSnapshot()
        latestResult = null
    }

    override fun submitInput(input: InputSnapshot) {
        latestInput = input
    }

    override fun step(deltaSeconds: Float) {
        val current = world ?: return

        if (current.phase == RunPhase.GameOver) {
            latestInput = latestInput.clearEdgeTriggers()
            return
        }

        if (latestInput.pausePressed && current.phase == RunPhase.Running) {
            current.phase = RunPhase.Paused
            current.rebuildSnapshot()
        } else if ((latestInput.resumePressed || latestInput.pausePressed) && current.phase == RunPhase.Paused) {
            current.phase = RunPhase.Running
        }

        if (current.phase == RunPhase.ChoosingUpgrade) {
            var changed = false
            latestInput.selectedUpgradeIndex?.let {
                current.applyUpgrade(it)
                changed = true
            }
            if (latestInput.rerollPressed) {
                current.rerollUpgrades()
                changed = true
            }
            latestInput = latestInput.clearEdgeTriggers()
            if (changed && current.phase == RunPhase.GameOver && latestResult == null) {
                latestResult = current.finalResult
            }
            return
        }

        if (current.phase != RunPhase.Running) {
            latestInput = latestInput.clearEdgeTriggers()
            return
        }

        val dt = deltaSeconds.coerceIn(0f, 1f / 20f)
        systems.forEach { system ->
            system.update(current, latestInput, dt)
        }

        if (current.player.hp <= 0f && current.phase != RunPhase.GameOver) {
            current.finishRun("Signal Lost")
        }

        current.rebuildSnapshot()
        if (current.phase == RunPhase.GameOver && latestResult == null) {
            latestResult = current.finalResult
        }
        latestInput = latestInput.clearEdgeTriggers()
    }

    override fun currentSnapshot(): FrameSnapshot = world?.snapshot ?: FrameSnapshot()

    override fun consumeLatestResult(): RunResult? = latestResult.also { latestResult = null }

    override fun pause() {
        world?.let {
            if (it.phase == RunPhase.Running) {
                it.phase = RunPhase.Paused
                it.rebuildSnapshot()
            }
        }
    }

    override fun resume() {
        world?.let {
            if (it.phase == RunPhase.Paused) {
                it.phase = RunPhase.Running
                it.rebuildSnapshot()
            }
        }
    }
}

private fun Float?.orZero(): Float = this ?: 0f

private fun InputSnapshot.clearEdgeTriggers(): InputSnapshot = copy(
    dashPressed = false,
    pulsePressed = false,
    shieldPressed = false,
    minePressed = false,
    pausePressed = false,
    resumePressed = false,
    rerollPressed = false,
    selectedUpgradeIndex = null,
)
