package com.antigravity.pampastarshooter.core.content

import com.antigravity.pampastarshooter.core.model.*
import kotlinx.serialization.Serializable

@Serializable
enum class UpgradeCategory {
    Offense,
    Defense,
    Utility,
    Synergy,
}

@Serializable
enum class CardRarity {
    Common,
    Rare,
    Epic,
}

@Serializable
enum class MissionMetric {
    Runs,
    Kills,
    Waves,
    Bosses,
    Credits,
}

@Serializable
enum class UpgradeStat {
    Damage,
    FireRate,
    Speed,
    Magnet,
    MaxHp,
    Regen,
    DashCooldown,
    PulseCooldown,
    ShieldStrength,
    PulseRadius,
    PulseDamage,
    CritChance,
    MultiShot,
    Pierce,
    Chain,
    ShieldUnlock,
    MineUnlock,
    Reroll,
    ScoreMultiplier,
}

@Serializable
data class ShipDef(
    val id: String,
    val label: String,
    val subtitle: String,
    val flavor: String,
    val accentColor: Long,
    val trailColor: Long,
    val maxHp: Float,
    val moveSpeed: Float,
    val baseDamage: Float,
    val fireInterval: Float,
    val projectileSpeed: Float,
    val projectileRadius: Float,
    val critChance: Float,
    val magnetRadius: Float,
    val autoTargetRange: Float,
    val dashCooldown: Float,
    val pulseCooldown: Float,
    val shieldCooldown: Float,
    val mineCooldown: Float,
    val passiveLabel: String,
    val passiveText: String,
)

@Serializable
data class WeaponDef(
    val id: String,
    val label: String,
    val damageMultiplier: Float,
    val projectileSpeedBonus: Float,
    val projectileRadiusBonus: Float,
    val projectilesPerShot: Int,
    val spreadDegrees: Float,
)

@Serializable
data class AbilityDef(
    val id: String,
    val label: String,
    val description: String,
    val cooldownSeconds: Float,
    val power: Float,
    val radius: Float,
)

@Serializable
data class EnemyDef(
    val id: String,
    val label: String,
    val role: String,
    val maxHp: Float,
    val speed: Float,
    val radius: Float,
    val xpReward: Int,
    val creditReward: Int,
    val contactDamage: Float,
    val shootCooldown: Float? = null,
    val projectileSpeed: Float = 0f,
    val color: Long,
    val splitInto: String? = null,
    val isMiniBoss: Boolean = false,
    val isBoss: Boolean = false,
    val bossPatternId: String? = null,
)

@Serializable
data class AffixDef(
    val id: String,
    val label: String,
    val hpMultiplier: Float,
    val speedMultiplier: Float,
    val creditMultiplier: Float,
    val color: Long,
    val extraContactDamage: Float = 0f,
)

@Serializable
data class BiomeDef(
    val id: String,
    val label: String,
    val subtitle: String,
    val accentPrimary: Long,
    val accentSecondary: Long,
    val backgroundTop: Long,
    val backgroundBottom: Long,
    val enemyPool: List<String>,
    val bossId: String,
    val eventIds: List<String>,
)

@Serializable
data class BossPatternDef(
    val id: String,
    val label: String,
    val burstCount: Int,
    val spreadDegrees: Float,
    val telegraphSeconds: Float,
    val projectileSpeed: Float,
    val chargeSpeed: Float,
)

@Serializable
data class UpgradeCardDef(
    val id: String,
    val label: String,
    val description: String,
    val category: UpgradeCategory,
    val rarity: CardRarity,
    val baseWeight: Float,
    val maxStacks: Int,
    val primaryStat: UpgradeStat,
    val primaryAmount: Float,
    val secondaryStat: UpgradeStat? = null,
    val secondaryAmount: Float = 0f,
)

@Serializable
data class MissionDef(
    val id: String,
    val label: String,
    val description: String,
    val target: Int,
    val rewardCredits: Int,
    val rewardArchiveXp: Int,
    val metric: MissionMetric,
)

@Serializable
data class WorldEventDef(
    val id: String,
    val label: String,
    val description: String,
    val durationSeconds: Float,
    val spawnRateMultiplier: Float = 1f,
    val enemySpeedMultiplier: Float = 1f,
    val creditMultiplier: Float = 1f,
)

@Serializable
enum class LabCategory {
    Survival,
    Weapons,
    Control,
    Tactics,
}

@Serializable
data class PermanentModuleDef(
    val id: String,
    val label: String,
    val description: String,
    val category: LabCategory,
    val baseCost: Int,
    val costPerLevel: Int,
    val costCurve: Int,
    val maxLevel: Int,
    val unlockArchiveRank: Int = 1,
)

@Serializable
enum class CampaignNodeKind {
    Recon,
    Sweep,
    Salvage,
    Boss,
}

@Serializable
data class PerkDef(
    val id: String,
    val label: String,
    val description: String,
    val flavor: String,
    val accentColor: Long,
)

@Serializable
data class CampaignNodeDef(
    val id: String,
    val label: String,
    val description: String,
    val kind: CampaignNodeKind,
    val objective: RunObjective,
    val rewardShipIds: List<String> = emptyList(),
    val rewardPerkIds: List<String> = emptyList(),
    val rewardModifierIds: List<String> = emptyList(),
    val unlockEndless: Boolean = false,
)

@Serializable
data class CampaignSectorDef(
    val id: String,
    val label: String,
    val subtitle: String,
    val biomeId: String,
    val nodes: List<CampaignNodeDef>,
)

@Serializable
data class GameContentBundle(
    val ships: List<ShipDef>,
    val weapons: List<WeaponDef>,
    val abilities: List<AbilityDef>,
    val enemies: List<EnemyDef>,
    val biomes: List<BiomeDef>,
    val bossPatterns: List<BossPatternDef>,
    val upgradeCards: List<UpgradeCardDef>,
    val runMissionPool: List<MissionDef>,
    val metaMissionPool: List<MissionDef>,
    val permanentModules: List<PermanentModuleDef>,
    val perks: List<PerkDef>,
    val campaignSectors: List<CampaignSectorDef>,
    val affixes: List<AffixDef>,
    val runModifiers: List<RunModifier>,
    val worldEvents: List<WorldEventDef>,
)

fun PermanentModuleDef.upgradeCost(level: Int): Int = baseCost + costPerLevel * level + costCurve * level * level

interface ContentRepository {
    fun load(): GameContentBundle
}

object DefaultGameContent {
    const val ShipStriker = "ship_striker"
    const val ShipWarden = "ship_warden"
    const val ShipSpecter = "ship_specter"

    fun create(): GameContentBundle = GameContentBundle(
        ships = listOf(
            ShipDef(
                id = ShipStriker,
                label = "Striker",
                subtitle = "Duelista mobile",
                flavor = "Telaio reattivo pensato per attraversare il caos e convertire il dash in overdrive offensivo.",
                accentColor = 0xFF4CE5FF,
                trailColor = 0xFF93FCFF,
                maxHp = 110f,
                moveSpeed = 320f,
                baseDamage = 16f,
                fireInterval = 0.36f,
                projectileSpeed = 620f,
                projectileRadius = 6f,
                critChance = 0.10f,
                magnetRadius = 96f,
                autoTargetRange = 420f,
                dashCooldown = 4.8f,
                pulseCooldown = 8.4f,
                shieldCooldown = 15f,
                mineCooldown = 11f,
                passiveLabel = "Slipstream",
                passiveText = "Dopo il dash ottieni 2.5s di overdrive a danno e velocita di fuoco aumentati.",
            ),
            ShipDef(
                id = ShipWarden,
                label = "Warden",
                subtitle = "Bastione tattico",
                flavor = "Scafo pesante, barriera reattiva e mine piu affidabili. Meno elegante, molto piu difficile da piegare.",
                accentColor = 0xFFFFB54A,
                trailColor = 0xFFFFE0A8,
                maxHp = 144f,
                moveSpeed = 264f,
                baseDamage = 13f,
                fireInterval = 0.46f,
                projectileSpeed = 540f,
                projectileRadius = 7f,
                critChance = 0.05f,
                magnetRadius = 92f,
                autoTargetRange = 390f,
                dashCooldown = 5.3f,
                pulseCooldown = 9.6f,
                shieldCooldown = 13.6f,
                mineCooldown = 9.8f,
                passiveLabel = "Bulwark",
                passiveText = "Lo scudo attivo assorbe di piu e le mine esplodono piu in fretta quando un elite si avvicina.",
            ),
            ShipDef(
                id = ShipSpecter,
                label = "Specter",
                subtitle = "Operatore di segnali",
                flavor = "Specialista in pulse e catene ioniche. Piuttosto fragile, ma letale quando il campo si affolla.",
                accentColor = 0xFF94A0FF,
                trailColor = 0xFFD9DDFF,
                maxHp = 98f,
                moveSpeed = 300f,
                baseDamage = 15f,
                fireInterval = 0.34f,
                projectileSpeed = 590f,
                projectileRadius = 5.5f,
                critChance = 0.08f,
                magnetRadius = 104f,
                autoTargetRange = 430f,
                dashCooldown = 5.1f,
                pulseCooldown = 7.1f,
                shieldCooldown = 15f,
                mineCooldown = 11f,
                passiveLabel = "Ghost Echo",
                passiveText = "Il pulse guadagna una catena bonus e le rare carte synergy compaiono piu spesso.",
            ),
        ),
        weapons = listOf(
            WeaponDef("blaster", "Blaster", 1f, 0f, 0f, 1, 0f),
            WeaponDef("scatter", "Scatter", 0.8f, -40f, 0f, 3, 14f),
            WeaponDef("piercer", "Piercer", 1.15f, 35f, 1f, 1, 0f),
        ),
        abilities = listOf(
            AbilityDef("dash", "Dash", "Scatto invulnerabile con breve overdrive.", 4.8f, 1f, 0f),
            AbilityDef("pulse", "Pulse", "Esplosione ad area con knockback.", 8.4f, 64f, 160f),
            AbilityDef("shield", "Shield", "Barriera temporanea che assorbe danni.", 12f, 54f, 0f),
            AbilityDef("mine", "Mine", "Carica a innesco prossimita.", 9f, 72f, 106f),
        ),
        enemies = listOf(
            EnemyDef("drone", "Needle Drone", "chaser", 34f, 118f, 15f, 1, 0, 11f, color = 0xFFFF6A8B),
            EnemyDef("gunner", "Line Gunner", "shooter", 42f, 86f, 16f, 2, 1, 11f, 1.55f, 225f, 0xFF7EA0FF),
            EnemyDef("brute", "Scrap Brute", "tank", 94f, 60f, 22f, 3, 2, 18f, color = 0xFFFFBD5A),
            EnemyDef("splitter", "Shard Splitter", "splitter", 46f, 92f, 17f, 2, 1, 12f, color = 0xFF4FFFC6, splitInto = "wisp"),
            EnemyDef("wisp", "Micro Wisp", "swarm", 18f, 154f, 10f, 1, 0, 9f, color = 0xFFA2FFE7),
            EnemyDef("sniper", "Rail Sniper", "shooter", 54f, 74f, 15f, 3, 2, 12f, 2.25f, 290f, 0xFFF4C7FF),
            EnemyDef("minotaur", "Minotaur Core", "miniboss", 280f, 82f, 26f, 8, 6, 22f, 1.05f, 255f, 0xFFFF8B4A, isMiniBoss = true, bossPatternId = "minotaur"),
            EnemyDef("relay", "Relay Tyrant", "boss", 610f, 78f, 34f, 18, 14, 26f, 0.95f, 255f, 0xFFFFD15A, isBoss = true, bossPatternId = "relay"),
            EnemyDef("lich", "Signal Lich", "boss", 580f, 86f, 32f, 18, 14, 24f, 0.88f, 275f, 0xFFA7B6FF, isBoss = true, bossPatternId = "lich"),
            EnemyDef("hydra", "Amber Hydra", "boss", 700f, 72f, 36f, 20, 15, 28f, 0.82f, 235f, 0xFFFFB85A, isBoss = true, bossPatternId = "hydra"),
        ),
        biomes = listOf(
            BiomeDef(
                id = "neon_docks",
                label = "Neon Docks",
                subtitle = "Corsie industriali interrotte da scariche fredde.",
                accentPrimary = 0xFF4CE5FF,
                accentSecondary = 0xFF163146,
                backgroundTop = 0xFF050B11,
                backgroundBottom = 0xFF0A1724,
                enemyPool = listOf("drone", "gunner", "splitter"),
                bossId = "relay",
                eventIds = listOf("ion_storm", "cache_drop"),
            ),
            BiomeDef(
                id = "obsidian_relay",
                label = "Obsidian Relay",
                subtitle = "Nodi neri, linee dure, cecchini in posizione.",
                accentPrimary = 0xFFA7B6FF,
                accentSecondary = 0xFF1D1F38,
                backgroundTop = 0xFF090A14,
                backgroundBottom = 0xFF11152B,
                enemyPool = listOf("drone", "gunner", "brute", "sniper"),
                bossId = "lich",
                eventIds = listOf("gravity_swell", "ion_storm"),
            ),
            BiomeDef(
                id = "amber_rift",
                label = "Amber Rift",
                subtitle = "Frammenti incandescenti e bestie corazzate.",
                accentPrimary = 0xFFFFB54A,
                accentSecondary = 0xFF362313,
                backgroundTop = 0xFF120B08,
                backgroundBottom = 0xFF1E130E,
                enemyPool = listOf("drone", "brute", "splitter", "sniper"),
                bossId = "hydra",
                eventIds = listOf("cache_drop", "gravity_swell"),
            ),
        ),
        bossPatterns = listOf(
            BossPatternDef("minotaur", "Ram Burst", 4, 20f, 0.32f, 285f, 240f),
            BossPatternDef("relay", "Relay Fan", 6, 42f, 0.42f, 300f, 225f),
            BossPatternDef("lich", "Lattice Bloom", 8, 60f, 0.56f, 320f, 210f),
            BossPatternDef("hydra", "Amber Crown", 10, 78f, 0.66f, 270f, 200f),
        ),
        upgradeCards = listOf(
            UpgradeCardDef("damage_amp", "Damage Amp", "+14% danno base.", UpgradeCategory.Offense, CardRarity.Common, 1.6f, 6, UpgradeStat.Damage, 0.14f),
            UpgradeCardDef("reactive_barrel", "Reactive Barrel", "-9% intervallo di fuoco.", UpgradeCategory.Offense, CardRarity.Common, 1.4f, 6, UpgradeStat.FireRate, -0.09f),
            UpgradeCardDef("thruster_burst", "Thruster Burst", "+10% velocita di movimento.", UpgradeCategory.Utility, CardRarity.Common, 1.1f, 5, UpgradeStat.Speed, 0.10f),
            UpgradeCardDef("magnet_grid", "Magnet Grid", "+18 raggio magnetico.", UpgradeCategory.Utility, CardRarity.Common, 1.2f, 5, UpgradeStat.Magnet, 18f),
            UpgradeCardDef("hull_patch", "Hull Patch", "+18 HP massimi e cura del bonus.", UpgradeCategory.Defense, CardRarity.Common, 1.2f, 5, UpgradeStat.MaxHp, 18f),
            UpgradeCardDef("field_regen", "Field Regen", "+0.45 HP/s.", UpgradeCategory.Defense, CardRarity.Rare, 0.9f, 4, UpgradeStat.Regen, 0.45f),
            UpgradeCardDef("dash_link", "Dash Link", "-10% cooldown dash.", UpgradeCategory.Utility, CardRarity.Common, 1.1f, 4, UpgradeStat.DashCooldown, -0.10f),
            UpgradeCardDef("pulse_sync", "Pulse Sync", "-12% cooldown pulse.", UpgradeCategory.Utility, CardRarity.Common, 1.0f, 4, UpgradeStat.PulseCooldown, -0.12f),
            UpgradeCardDef("bulwark", "Bulwark", "+18 forza scudo.", UpgradeCategory.Defense, CardRarity.Common, 1.0f, 4, UpgradeStat.ShieldStrength, 18f),
            UpgradeCardDef("overclock_pulse", "Overclock Pulse", "+20 raggio pulse e +14% danni pulse.", UpgradeCategory.Synergy, CardRarity.Rare, 0.8f, 4, UpgradeStat.PulseRadius, 20f, UpgradeStat.PulseDamage, 0.14f),
            UpgradeCardDef("crit_link", "Crit Link", "+6% critico.", UpgradeCategory.Offense, CardRarity.Rare, 0.7f, 4, UpgradeStat.CritChance, 0.06f),
            UpgradeCardDef("scatter_array", "Scatter Array", "+1 proiettile.", UpgradeCategory.Offense, CardRarity.Rare, 0.7f, 2, UpgradeStat.MultiShot, 1f),
            UpgradeCardDef("phase_rounds", "Phase Rounds", "+1 perforazione.", UpgradeCategory.Offense, CardRarity.Rare, 0.6f, 2, UpgradeStat.Pierce, 1f),
            UpgradeCardDef("ion_arc", "Ion Arc", "Il pulse guadagna una catena.", UpgradeCategory.Synergy, CardRarity.Epic, 0.4f, 3, UpgradeStat.Chain, 1f),
            UpgradeCardDef("barrier_module", "Barrier Module", "Sblocca Shield attivo.", UpgradeCategory.Defense, CardRarity.Rare, 0.5f, 1, UpgradeStat.ShieldUnlock, 1f),
            UpgradeCardDef("mine_layer", "Mine Layer", "Sblocca Mine attiva.", UpgradeCategory.Defense, CardRarity.Rare, 0.5f, 1, UpgradeStat.MineUnlock, 1f),
            UpgradeCardDef("tactical_reroll", "Tactical Reroll", "+1 reroll durante i level-up.", UpgradeCategory.Utility, CardRarity.Rare, 0.5f, 2, UpgradeStat.Reroll, 1f),
            UpgradeCardDef("archive_cache", "Archive Cache", "+10% punteggio e frammenti.", UpgradeCategory.Synergy, CardRarity.Epic, 0.45f, 3, UpgradeStat.ScoreMultiplier, 0.10f),
        ),
        runMissionPool = listOf(
            MissionDef("run_kills_40", "Pulizia veloce", "Elimina 55 nemici in una run.", 55, 10, 8, MissionMetric.Kills),
            MissionDef("run_wave_6", "Linea tenuta", "Raggiungi l'ondata 7.", 7, 14, 12, MissionMetric.Waves),
            MissionDef("run_credits_45", "Raccolta rapida", "Ottieni 32 crediti in una run.", 32, 12, 10, MissionMetric.Credits),
            MissionDef("run_boss_1", "Interdizione", "Abbatti un boss.", 1, 18, 16, MissionMetric.Bosses),
            MissionDef("run_kills_80", "Saturazione", "Elimina 100 nemici in una run.", 100, 20, 18, MissionMetric.Kills),
        ),
        metaMissionPool = listOf(
            MissionDef("meta_runs_5", "Warm-up", "Completa 5 run.", 5, 60, 25, MissionMetric.Runs),
            MissionDef("meta_kills_250", "Ammasso di rottami", "Elimina 250 nemici totali.", 250, 90, 36, MissionMetric.Kills),
            MissionDef("meta_wave_12", "Tenuta di settore", "Raggiungi l'ondata 12 totale migliore.", 12, 120, 44, MissionMetric.Waves),
            MissionDef("meta_boss_4", "Signal Breaker", "Distruggi 4 boss in totale.", 4, 110, 40, MissionMetric.Bosses),
            MissionDef("meta_credits_350", "Archivista", "Raccogli 350 crediti totali.", 350, 140, 54, MissionMetric.Credits),
        ),
        permanentModules = listOf(
            PermanentModuleDef("hull", "Reinforced Hull", "+10 HP iniziali per livello.", LabCategory.Survival, 42, 24, 8, 6, unlockArchiveRank = 1),
            PermanentModuleDef("bulwark", "Bulwark Mesh", "Scudi piu robusti e mine con impatto migliore.", LabCategory.Survival, 50, 26, 8, 6, unlockArchiveRank = 2),
            PermanentModuleDef("reserve", "Reserve Cell", "Migliora forza e uptime dello scudo.", LabCategory.Survival, 54, 28, 10, 5, unlockArchiveRank = 3),
            PermanentModuleDef("reactor", "Reactor Feed", "Piu danno base e cadenza di fuoco.", LabCategory.Weapons, 48, 28, 9, 6, unlockArchiveRank = 1),
            PermanentModuleDef("targeting", "Targeting Lattice", "Lock-on piu ampio e colpi piu veloci.", LabCategory.Weapons, 46, 26, 8, 6, unlockArchiveRank = 2),
            PermanentModuleDef("capacitor", "Pulse Capacitor", "Pulse piu pesanti e frequenti.", LabCategory.Weapons, 58, 30, 10, 5, unlockArchiveRank = 3),
            PermanentModuleDef("ordnance", "Ordnance Rack", "Mine piu ampie, forti e reattive.", LabCategory.Weapons, 56, 30, 10, 5, unlockArchiveRank = 4),
            PermanentModuleDef("thrusters", "Vector Thrusters", "Migliora movimento e portata del dash.", LabCategory.Control, 44, 24, 8, 6, unlockArchiveRank = 1),
            PermanentModuleDef("magnet", "Magnet Array", "Pickup attratti prima e trascinati piu in fretta.", LabCategory.Control, 40, 22, 8, 6, unlockArchiveRank = 1),
            PermanentModuleDef("cache", "Archive Cache", "Moltiplicatore score migliore per run pulite.", LabCategory.Tactics, 52, 28, 8, 5, unlockArchiveRank = 2),
            PermanentModuleDef("overdrive", "Slipstream Core", "Overdrive piu lungo e con bonus superiore.", LabCategory.Tactics, 60, 32, 10, 5, unlockArchiveRank = 3),
            PermanentModuleDef("reroll", "Tactical Buffer", "Aggiunge reroll durante il draft moduli.", LabCategory.Tactics, 68, 34, 12, 3, unlockArchiveRank = 4),
        ),
        perks = listOf(
            PerkDef("emergency_shield", "Emergency Shield", "Shield sbloccato e +20 forza scudo iniziale.", "Buffer difensivo permanente per ingressi sporchi.", 0xFF8DFFBF),
            PerkDef("dash_coil", "Dash Coil", "-10% cooldown dash e +6% velocita di movimento.", "Thrusters piu reattivi e traiettorie piu pulite.", 0xFF63E7FF),
            PerkDef("pulse_primer", "Pulse Primer", "+20 raggio pulse e +10% danni pulse.", "Il core del pulse esce piu largo e piu denso.", 0xFFAAB5FF),
            PerkDef("magnet_surge", "Magnet Surge", "+30 magnet radius e +10% pickup speed.", "Il campo di raccolta aggancia prima e tira piu deciso.", 0xFF63E7FF),
            PerkDef("regen_matrix", "Regen Matrix", "+12 HP massimi e +0.6 HP/s.", "Un reticolo di recupero per run lunghe e sporche.", 0xFF88FFC8),
            PerkDef("draft_cache", "Draft Cache", "+1 reroll e +10% score multiplier.", "Archivio tattico dedicato alle run ad alta precisione.", 0xFFFFBF66),
        ),
        campaignSectors = listOf(
            CampaignSectorDef(
                id = "sector_neon",
                label = "Neon Docks",
                subtitle = "Primo corridoio operativo, visibilita alta e traffico leggero.",
                biomeId = "neon_docks",
                nodes = listOf(
                    CampaignNodeDef("sector1_recon", "Recon", "Riconquista le prime corsie e stabilizza il settore.", CampaignNodeKind.Recon, RunObjective.ReachWave(4)),
                    CampaignNodeDef("sector1_sweep", "Sweep", "Ripulisci le unit com piu aggressive del molo.", CampaignNodeKind.Sweep, RunObjective.KillCount(55)),
                    CampaignNodeDef("sector1_salvage", "Salvage", "Recupera abbastanza crediti per finanziare il salto successivo.", CampaignNodeKind.Salvage, RunObjective.EarnCredits(32)),
                    CampaignNodeDef(
                        id = "sector1_boss",
                        label = "Boss",
                        description = "Neutralizza il segnale dominante del distretto.",
                        kind = CampaignNodeKind.Boss,
                        objective = RunObjective.DefeatBoss(),
                        rewardShipIds = listOf(ShipWarden),
                        rewardPerkIds = listOf("emergency_shield", "dash_coil"),
                    ),
                ),
            ),
            CampaignSectorDef(
                id = "sector_obsidian",
                label = "Obsidian Relay",
                subtitle = "Reti indurite, piu cecchini e finestre d'ingaggio piu strette.",
                biomeId = "obsidian_relay",
                nodes = listOf(
                    CampaignNodeDef("sector2_recon", "Recon", "Riapri i nodi oscurati del relay.", CampaignNodeKind.Recon, RunObjective.ReachWave(6)),
                    CampaignNodeDef("sector2_sweep", "Sweep", "Spezza la pressione dei tiratori di linea.", CampaignNodeKind.Sweep, RunObjective.KillCount(85)),
                    CampaignNodeDef("sector2_salvage", "Salvage", "Accumula risorse per il salto finale.", CampaignNodeKind.Salvage, RunObjective.EarnCredits(48)),
                    CampaignNodeDef(
                        id = "sector2_boss",
                        label = "Boss",
                        description = "Abbatti il lich di segnale e apri il settore avanzato.",
                        kind = CampaignNodeKind.Boss,
                        objective = RunObjective.DefeatBoss(),
                        rewardShipIds = listOf(ShipSpecter),
                        rewardPerkIds = listOf("pulse_primer", "magnet_surge"),
                        rewardModifierIds = listOf("dense_swarm"),
                    ),
                ),
            ),
            CampaignSectorDef(
                id = "sector_amber",
                label = "Amber Rift",
                subtitle = "Fratture incandescenti e pressione da endgame.",
                biomeId = "amber_rift",
                nodes = listOf(
                    CampaignNodeDef("sector3_recon", "Recon", "Sonda il fronte caldo senza perdere ritmo.", CampaignNodeKind.Recon, RunObjective.ReachWave(8)),
                    CampaignNodeDef("sector3_sweep", "Sweep", "Sfoltisci la camera a piu alta densita del teatro.", CampaignNodeKind.Sweep, RunObjective.KillCount(120)),
                    CampaignNodeDef("sector3_salvage", "Salvage", "Recupera il payload necessario a chiudere il varco.", CampaignNodeKind.Salvage, RunObjective.EarnCredits(70)),
                    CampaignNodeDef(
                        id = "sector3_boss",
                        label = "Boss",
                        description = "Elimina il guardiano finale e apri la frontiera endless.",
                        kind = CampaignNodeKind.Boss,
                        objective = RunObjective.DefeatBoss(),
                        rewardPerkIds = listOf("regen_matrix", "draft_cache"),
                        rewardModifierIds = listOf("glass_drive", "long_burn"),
                        unlockEndless = true,
                    ),
                ),
            ),
        ),
        affixes = listOf(
            AffixDef("swift", "Swift", 1.15f, 1.28f, 1.08f, 0xFF66E8FF),
            AffixDef("armored", "Armored", 1.45f, 0.92f, 1.14f, 0xFFFFCF7A, extraContactDamage = 4f),
            AffixDef("volatile", "Volatile", 1.1f, 1.06f, 1.12f, 0xFFFF7A8E, extraContactDamage = 3f),
            AffixDef("draining", "Draining", 1.2f, 1.02f, 1.18f, 0xFF9BB3FF, extraContactDamage = 5f),
        ),
        runModifiers = listOf(
            RunModifier("dense_swarm", "Dense Swarm", "More enemies, better rewards.", 1.18f, requiredArchiveRank = 2, riskLabel = "Swarm"),
            RunModifier("glass_drive", "Glass Drive", "Higher damage dealt and received.", 1.24f, requiredArchiveRank = 4, riskLabel = "Glass"),
            RunModifier("long_burn", "Long Burn", "Longer waves with steadier pressure.", 1.20f, requiredArchiveRank = 6, riskLabel = "Endurance"),
        ),
        worldEvents = listOf(
            WorldEventDef("ion_storm", "Ion Storm", "Più elite e cadenza di spawn accelerata.", 18f, spawnRateMultiplier = 1.25f, creditMultiplier = 1.15f),
            WorldEventDef("cache_drop", "Cache Drop", "I nemici rilasciano più crediti per un tratto.", 20f, creditMultiplier = 1.35f),
            WorldEventDef("gravity_swell", "Gravity Swell", "I nemici rallentano ma sparano pattern più densi.", 16f, enemySpeedMultiplier = 0.85f, spawnRateMultiplier = 1.1f),
        ),
    )

    fun starterProfile(): PlayerProfile = PlayerProfile(
        unlockTree = UnlockTree(
            unlockedShipIds = setOf(ShipStriker),
            permanentModules = create()
                .permanentModules
                .filter { it.unlockArchiveRank <= 1 }
                .associate { it.id to 0 },
            nextMetaMissionIndex = 3,
        ),
        activeMissions = create().metaMissionPool.take(3).map(::MissionState),
        selectedShipId = ShipStriker,
    )
}
