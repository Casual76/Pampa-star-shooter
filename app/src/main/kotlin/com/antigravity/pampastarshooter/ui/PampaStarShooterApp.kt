package com.antigravity.pampastarshooter.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.antigravity.pampastarshooter.AppContainer
import com.antigravity.pampastarshooter.core.content.DefaultGameContent
import com.antigravity.pampastarshooter.core.content.GameContentBundle
import com.antigravity.pampastarshooter.core.content.LabCategory
import com.antigravity.pampastarshooter.core.content.upgradeCost
import com.antigravity.pampastarshooter.core.model.GameSettings
import com.antigravity.pampastarshooter.core.model.GraphicsProfile
import com.antigravity.pampastarshooter.core.model.PlayerProfile
import com.antigravity.pampastarshooter.core.model.RunHistoryEntry
import com.antigravity.pampastarshooter.core.model.RunModifier
import com.antigravity.pampastarshooter.data.repository.SettingsRepository
import com.antigravity.pampastarshooter.game.android.R as GameAndroidR
import kotlinx.coroutines.launch

private object Route {
    const val Home = "home"
    const val Lab = "lab"
    const val Codex = "codex"
    const val Missions = "missions"
    const val Settings = "settings"
    const val History = "history"
    const val Operations = "operations"
    const val Game = "game"
}

private val PampaInk = Color(0xFF050911)
private val PampaPanelTop = Color(0xF3141D29)
private val PampaPanelBottom = Color(0xED091018)
private val PampaCyan = Color(0xFF63E7FF)
private val PampaAmber = Color(0xFFFFBF66)
private val PampaLavender = Color(0xFFAAB5FF)
private val PampaMint = Color(0xFF88FFC8)
private val PampaText = Color(0xFFF3FAFF)
private val PampaMuted = Color(0xFFA7BBCC)

private data class PendingRunSetup(
    val modifiers: List<String> = emptyList(),
)

@Composable
fun PampaStarShooterApp(
    container: AppContainer,
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val profile by container.profileRepository.profile.collectAsState(initial = DefaultGameContent.starterProfile())
    val settings by container.settingsRepository.settings.collectAsState(initial = GameSettings())
    val history by container.historyRepository.history.collectAsState(initial = emptyList())
    val content = remember { container.contentRepository.load() }
    var pendingRunSetup by remember { mutableStateOf(PendingRunSetup()) }

    ProvidePampaChrome(settings) {
        NavHost(navController = navController, startDestination = Route.Home) {
            composable(Route.Home) {
                HomeScreen(
                    profile = profile,
                    content = content,
                    onSelectShip = { shipId -> coroutineScope.launch { container.profileRepository.setSelectedShip(shipId) } },
                    onDismissOnboarding = { coroutineScope.launch { container.profileRepository.markTutorialSeen() } },
                    onNavigate = navController::navigate,
                )
            }
            composable(Route.Lab) {
                LabScreen(
                    profile = profile,
                    content = content,
                    onBack = { navController.popBackStack() },
                    onUpgrade = { moduleId ->
                        coroutineScope.launch {
                            container.profileRepository.upgradeModule(moduleId)
                        }
                    },
                )
            }
            composable(Route.Codex) {
                CodexScreen(profile = profile, content = content, onBack = { navController.popBackStack() })
            }
            composable(Route.Missions) {
                MissionsScreen(profile = profile, onBack = { navController.popBackStack() })
            }
            composable(Route.Settings) {
                SettingsScreen(settings = settings, repository = container.settingsRepository, onBack = { navController.popBackStack() })
            }
            composable(Route.History) {
                HistoryScreen(history = history, onBack = { navController.popBackStack() })
            }
            composable(Route.Operations) {
                OperationsScreen(
                    profile = profile,
                    content = content,
                    selectedModifierIds = pendingRunSetup.modifiers,
                    onBack = { navController.popBackStack() },
                    onModifierChange = { pendingRunSetup = pendingRunSetup.copy(modifiers = it) },
                    onLaunch = { modifiers ->
                        pendingRunSetup = pendingRunSetup.copy(modifiers = modifiers)
                        navController.navigate(Route.Game)
                    },
                )
            }
            composable(Route.Game) {
                GameScreen(
                    container = container,
                    profile = profile,
                    settings = settings,
                    selectedModifiers = pendingRunSetup.modifiers,
                    onExit = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun AppBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val ui = chrome()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF07101A), Color(0xFF0A1520), ui.inkArgb.asColor()),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(start = 10.dp, top = 34.dp)
                .size(280.dp)
                .background(Brush.radialGradient(listOf(ui.cyanArgb.asColor().copy(alpha = 0.22f), Color.Transparent)), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 30.dp)
                .size(360.dp)
                .background(Brush.radialGradient(listOf(ui.amberArgb.asColor().copy(alpha = 0.18f), Color.Transparent)), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 42.dp)
                .size(220.dp)
                .background(Brush.radialGradient(listOf(ui.lavenderArgb.asColor().copy(alpha = 0.12f), Color.Transparent)), CircleShape),
        )
        content()
    }
}

@Composable
private fun PosterScaffold(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ui = chrome()
    AppBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Surface(shape = RoundedCornerShape(30.dp), color = Color(0x6B0A1119), tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(Color(0x14000000))) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ui.textArgb.asColor())
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 28.sp, color = ui.textArgb.asColor())
                        Text(subtitle, color = ui.mutedArgb.asColor(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    accent: Color = PampaCyan,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ui = chrome()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.verticalGradient(listOf(ui.panelTopArgb.asColor(), ui.panelBottomArgb.asColor())))
            .border(1.dp, accent.copy(alpha = ui.edgeAlpha), RoundedCornerShape(30.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun shipAccent(shipId: String): Color {
    val ui = chrome()
    return when (shipId) {
        "ship_warden" -> ui.mintArgb.asColor()
        "ship_specter" -> ui.lavenderArgb.asColor()
        else -> ui.cyanArgb.asColor()
    }
}

@Composable
private fun moduleAccent(moduleId: String): Color {
    val ui = chrome()
    return when (moduleId) {
        "hull" -> ui.cyanArgb.asColor()
        "reactor" -> ui.amberArgb.asColor()
        "bulwark" -> ui.mintArgb.asColor()
        "reserve" -> ui.mintArgb.asColor()
        "targeting" -> ui.lavenderArgb.asColor()
        "capacitor" -> ui.amberArgb.asColor()
        "ordnance" -> ui.amberArgb.asColor()
        "thrusters" -> ui.cyanArgb.asColor()
        "magnet" -> ui.cyanArgb.asColor()
        "cache" -> ui.lavenderArgb.asColor()
        "overdrive" -> ui.lavenderArgb.asColor()
        "reroll" -> ui.amberArgb.asColor()
        else -> ui.cyanArgb.asColor()
    }
}

private fun categoryLabel(category: LabCategory): String = when (category) {
    LabCategory.Survival -> "Survival"
    LabCategory.Weapons -> "Weapons"
    LabCategory.Control -> "Control"
    LabCategory.Tactics -> "Tactics"
}

private fun categoryDescription(category: LabCategory): String = when (category) {
    LabCategory.Survival -> "Piastre, scudi e buffer che aumentano il margine di errore."
    LabCategory.Weapons -> "Fuoco base, pulse e mine per build piu taglienti."
    LabCategory.Control -> "Movimento, dash e raccolta pickup piu puliti."
    LabCategory.Tactics -> "Draft e moltiplicatori per run avanzate."
}

@Composable
private fun HomeScreen(
    profile: PlayerProfile,
    content: GameContentBundle,
    onSelectShip: (String) -> Unit,
    onDismissOnboarding: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val ui = chrome()
    val selectedShip = content.ships.firstOrNull { it.id == profile.selectedShipId } ?: content.ships.first()
    val selectedAccent = shipAccent(selectedShip.id)
    val pendingMissions = profile.activeMissions.count { !it.claimed }

    AppBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroPoster(
                profile = profile,
                selectedShip = selectedShip,
                accent = selectedAccent,
                onLaunch = { onNavigate(Route.Operations) },
                onOpenLab = { onNavigate(Route.Lab) },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InsightCard("Archive rank", profile.archiveRank.toString(), PampaCyan)
                InsightCard("Pending ops", pendingMissions.toString(), ui.amberArgb.asColor())
                InsightCard("Ships online", profile.unlockTree.unlockedShipIds.size.toString(), ui.mintArgb.asColor())
                InsightCard("Codex", "${profile.unlockTree.codexBiomeIds.size + profile.unlockTree.codexEnemyIds.size}", ui.lavenderArgb.asColor())
            }

            SectionHeader(
                title = "Hangar",
                subtitle = "Scegli il telaio prima della prossima run. Ogni nave sposta molto il ritmo.",
            )
            content.ships.forEach { ship ->
                val unlocked = ship.id in profile.unlockTree.unlockedShipIds
                val selected = ship.id == profile.selectedShipId
                ShipCard(
                    label = ship.label,
                    subtitle = ship.subtitle,
                    passive = ship.passiveText,
                    accent = shipAccent(ship.id),
                    unlocked = unlocked,
                    selected = selected,
                    requirement = shipUnlockRequirement(ship.id),
                    onClick = { onSelectShip(ship.id) },
                )
            }

            SectionHeader(
                title = "Operations",
                subtitle = "Meta progression, archivio e tuning del client.",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuTile(
                    title = "Laboratorio",
                    body = "Spendi crediti in moduli permanenti e rendi piu stabili le run.",
                    accent = ui.amberArgb.asColor(),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Route.Lab) },
                )
                MenuTile(
                    title = "Codex",
                    body = "Biomi, elites e boss gia incontrati dall'account.",
                    accent = ui.lavenderArgb.asColor(),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Route.Codex) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuTile(
                    title = "Missioni",
                    body = "Milestone persistenti con premi gia riversati nel profilo.",
                    accent = ui.mintArgb.asColor(),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Route.Missions) },
                )
                MenuTile(
                    title = "Cronologia",
                    body = "Ultime run, score e avanzamento archivio.",
                    accent = ui.cyanArgb.asColor(),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Route.History) },
                )
            }
            MenuTile(
                title = "Impostazioni",
                body = "Grafica, layout touch, audio, haptics e accessibilita.",
                accent = ui.cyanArgb.asColor(),
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Settings,
                onClick = { onNavigate(Route.Settings) },
            )

            GlassPanel(accent = ui.amberArgb.asColor()) {
                Text("Field note", color = ui.amberArgb.asColor(), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                Text(
                    text = "Portrait e landscape sono entrambi supportati. In portrait il gioco resta leggibile, in landscape respira meglio.",
                    color = ui.textArgb.asColor(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        if (!profile.tutorialSeen) {
            OnboardingOverlay(onDismiss = onDismissOnboarding)
        }
    }
}

@Composable
private fun HeroPoster(
    profile: PlayerProfile,
    selectedShip: com.antigravity.pampastarshooter.core.content.ShipDef,
    accent: Color,
    onLaunch: () -> Unit,
    onOpenLab: () -> Unit,
) {
    val ui = chrome()
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp),
        accent = accent,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatusChip(text = "ARCHIVE // RANK ${profile.archiveRank}", accent = accent)
                Text(
                    text = "PAMPA STAR SHOOTER",
                    color = ui.textArgb.asColor(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                )
                Text(
                    text = "Arena survival mobile-first con mutatori pre-run, VFX piu netti e una shell piu curata.",
                    color = ui.mutedArgb.asColor(),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusChip("Best wave ${profile.bestWave}", ui.amberArgb.asColor())
                    StatusChip("${profile.credits} crediti", ui.mintArgb.asColor())
                }
                GlassPanel(accent = accent, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedShip.label.uppercase(), color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.2.sp)
                    Text(selectedShip.subtitle, color = ui.textArgb.asColor(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(selectedShip.passiveText, color = ui.mutedArgb.asColor(), style = MaterialTheme.typography.bodyMedium)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onLaunch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open operations", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenLab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Text("Apri laboratorio")
                    }
                }
            }

            ShipPoster(accent = accent, shipLabel = selectedShip.label)
        }
    }
}

@Composable
private fun ShipPoster(
    accent: Color,
    shipLabel: String,
) {
    val ui = chrome()
    Box(
        modifier = Modifier
            .width(128.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.18f), Color(0x22000000))))
            .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(26.dp)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .size(86.dp)
                .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.42f), Color.Transparent)), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(84.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xC40A1018))
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(28.dp)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .width(22.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(50.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ui.textArgb.asColor()),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .width(36.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ui.amberArgb.asColor()),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("CURRENT FRAME", color = accent, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(shipLabel, color = ui.textArgb.asColor(), fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
        }
    }
}

@Composable
private fun InsightCard(
    label: String,
    value: String,
    accent: Color,
) {
    val ui = chrome()
    GlassPanel(modifier = Modifier.width(148.dp), accent = accent) {
        Text(label.uppercase(), color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
        Text(value, color = ui.textArgb.asColor(), fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 28.sp)
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    val ui = chrome()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title.uppercase(), color = ui.cyanArgb.asColor(), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.4.sp)
        Text(subtitle, color = ui.mutedArgb.asColor(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ShipCard(
    label: String,
    subtitle: String,
    passive: String,
    accent: Color,
    unlocked: Boolean,
    selected: Boolean,
    requirement: String,
    onClick: () -> Unit,
) {
    val ui = chrome()
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = unlocked, onClick = onClick),
        accent = if (selected) accent else accent.copy(alpha = 0.6f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accent.copy(alpha = if (selected) 0.9f else 0.45f)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, color = ui.textArgb.asColor(), fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(subtitle, color = ui.mutedArgb.asColor(), style = MaterialTheme.typography.bodyMedium)
                Text(passive, color = Color(0xFFD6E6F2), style = MaterialTheme.typography.bodyMedium)
                if (!unlocked) {
                    Text(requirement, color = ui.amberArgb.asColor(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            StatusChip(
                text = when {
                    !unlocked -> "Locked"
                    selected -> "Selected"
                    else -> "Ready"
                },
                accent = if (selected) accent else ui.amberArgb.asColor(),
            )
        }
    }
}

@Composable
private fun MenuTile(
    title: String,
    body: String,
    accent: Color,
    modifier: Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val ui = chrome()
    GlassPanel(modifier = modifier.clickable(onClick = onClick), accent = accent) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent)
        }
        Text(title, color = ui.textArgb.asColor(), fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(body, color = ui.mutedArgb.asColor(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusChip(
    text: String,
    accent: Color = PampaCyan,
) {
    val ui = chrome()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, color = ui.textArgb.asColor(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun OnboardingOverlay(
    onDismiss: () -> Unit,
) {
    val ui = chrome()
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xCC03070D)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), accent = ui.amberArgb.asColor()) {
                Text("FIRST FLIGHT", color = ui.amberArgb.asColor(), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.6.sp)
                Text(
                    text = "Muovi la nave a sinistra, usa le abilita a destra e fermati solo quando il gioco ti offre carte davvero forti.",
                    color = ui.textArgb.asColor(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                )
                TutorialTip("Movement", "Joystick sinistro per kite, spacing e sopravvivenza.")
                TutorialTip("Abilities", "Dash, Pulse, Shield e Mine hanno ruoli diversi. Non sprecarli insieme.")
                TutorialTip("Meta", "Dopo ogni run torna nel laboratorio e reinvesti i crediti.")
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = ui.amberArgb.asColor(), contentColor = Color.Black),
                ) {
                    Text("Entriamo in hangar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TutorialTip(
    title: String,
    body: String,
) {
    val ui = chrome()
    GlassPanel(accent = ui.cyanArgb.asColor()) {
        Text(title.uppercase(), color = ui.cyanArgb.asColor(), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(body, color = ui.mutedArgb.asColor(), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun shipUnlockRequirement(shipId: String): String = when (shipId) {
    DefaultGameContent.ShipWarden -> "Unlock at wave 4."
    DefaultGameContent.ShipSpecter -> "Unlock at wave 8 or after 3 bosses."
    else -> "Starter frame."
}

@Composable
private fun OperationsScreen(
    profile: PlayerProfile,
    content: GameContentBundle,
    selectedModifierIds: List<String>,
    onBack: () -> Unit,
    onModifierChange: (List<String>) -> Unit,
    onLaunch: (List<String>) -> Unit,
) {
    val ui = chrome()
    val ship = content.ships.firstOrNull { it.id == profile.selectedShipId } ?: content.ships.first()
    val accent = shipAccent(ship.id)
    val unlockedModifierIds = content.runModifiers
        .filter { profile.archiveRank >= it.requiredArchiveRank }
        .map { it.id }
        .toSet()
    val selected = selectedModifierIds.filter { it in unlockedModifierIds }.take(2)
    val scoreMultiplier = selected.fold(1f) { acc, modifierId ->
        acc * (content.runModifiers.firstOrNull { it.id == modifierId }?.scoreMultiplier ?: 1f)
    }

    PosterScaffold(
        title = "Operations",
        subtitle = "Configure ship, mutators, and score risk before the run.",
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlassPanel(accent = accent) {
                StatusChip("SELECTED FRAME", accent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(ship.label, color = ui.textArgb.asColor(), fontWeight = FontWeight.Black, fontSize = 28.sp)
                        Text(ship.subtitle, color = ui.mutedArgb.asColor(), style = MaterialTheme.typography.bodyLarge)
                        Text(ship.passiveText, color = ui.textArgb.asColor(), style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip("Move ${ship.moveSpeed.toInt()}", accent)
                            StatusChip("Hull ${ship.maxHp.toInt()}", ui.amberArgb.asColor())
                            StatusChip("Pulse ${ship.pulseCooldown.toInt()}s", ui.lavenderArgb.asColor())
                        }
                    }
                    Image(
                        painter = painterResource(
                            when (ship.id) {
                                DefaultGameContent.ShipWarden -> GameAndroidR.drawable.fx_ship_warden
                                DefaultGameContent.ShipSpecter -> GameAndroidR.drawable.fx_ship_specter
                                else -> GameAndroidR.drawable.fx_ship_striker
                            },
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(92.dp),
                    )
                }
            }

            GlassPanel(accent = ui.cyanArgb.asColor()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("RUN MUTATORS", color = ui.textArgb.asColor(), fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("Select up to 2 unlocked mutators.", color = ui.mutedArgb.asColor())
                    }
                    StatusChip("${selected.size}/2 selected", ui.cyanArgb.asColor())
                }
                content.runModifiers.forEach { modifier ->
                    val unlocked = profile.archiveRank >= modifier.requiredArchiveRank
                    val isSelected = modifier.id in selected
                    ModifierCard(
                        modifierDef = modifier,
                        unlocked = unlocked,
                        selected = isSelected,
                        onToggle = {
                            if (!unlocked) return@ModifierCard
                            val next = when {
                                isSelected -> selected - modifier.id
                                selected.size >= 2 -> selected.drop(1) + modifier.id
                                else -> selected + modifier.id
                            }
                            onModifierChange(next)
                        },
                    )
                }
            }

            GlassPanel(accent = ui.amberArgb.asColor()) {
                Text("RISK PREVIEW", color = ui.amberArgb.asColor(), fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("Projected score multiplier and current mission pressure for the next run.", color = ui.mutedArgb.asColor())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusChip("x${"%.2f".format(scoreMultiplier)} score", ui.amberArgb.asColor())
                    StatusChip("Rank ${profile.archiveRank}", accent)
                    StatusChip("${profile.activeMissions.count { !it.claimed }} active ops", ui.mintArgb.asColor())
                }
            }

            Button(
                onClick = { onLaunch(selected) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Launch run", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun ModifierCard(
    modifierDef: RunModifier,
    unlocked: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val ui = chrome()
    val accent = when (modifierDef.id) {
        "dense_swarm" -> ui.cyanArgb.asColor()
        "glass_drive" -> ui.lavenderArgb.asColor()
        else -> ui.amberArgb.asColor()
    }
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = unlocked, onClick = onToggle),
        accent = if (selected) accent else accent.copy(alpha = 0.58f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(GameAndroidR.drawable.fx_badge_modifier),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(modifierDef.label, color = ui.textArgb.asColor(), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(modifierDef.description, color = ui.mutedArgb.asColor())
                Text(
                    if (unlocked) "Risk ${modifierDef.riskLabel} | x${"%.2f".format(modifierDef.scoreMultiplier)} score"
                    else "Unlock at archive rank ${modifierDef.requiredArchiveRank}",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            StatusChip(
                text = when {
                    selected -> "Queued"
                    unlocked -> "Ready"
                    else -> "Locked"
                },
                accent = accent,
            )
        }
    }
}

@Composable
private fun LabScreen(
    profile: PlayerProfile,
    content: GameContentBundle,
    onBack: () -> Unit,
    onUpgrade: (String) -> Unit,
) {
    val investedLevels = content.permanentModules.sumOf { profile.unlockTree.permanentModules[it.id] ?: 0 }
    val totalLevels = content.permanentModules.sumOf { it.maxLevel }
    val affordableProjects = content.permanentModules.count { module ->
        val level = profile.unlockTree.permanentModules[module.id] ?: 0
        level < module.maxLevel && profile.credits >= module.upgradeCost(level)
    }
    PosterScaffold(
        title = "Laboratorio",
        subtitle = "Potenzia il telaio account-wide e spingi il meta sempre piu avanti.",
        onBack = onBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = WindowInsets(0).asPaddingValues(),
        ) {
            item {
                GlassPanel(accent = PampaAmber) {
                    Text("CREDIT RESERVE", color = PampaAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                    Text("${profile.credits} crediti", color = PampaText, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 30.sp)
                    Text("Ogni upgrade modella tutte le run future. I costi ora crescono piu in fretta e il laboratorio e stato ampliato.", color = PampaMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusChip("$affordableProjects pronti", PampaMint)
                        StatusChip("$investedLevels/$totalLevels livelli", PampaCyan)
                    }
                }
            }
            LabCategory.values().forEach { category ->
                val modules = content.permanentModules.filter { it.category == category }
                item {
                    SectionHeader(
                        title = categoryLabel(category),
                        subtitle = categoryDescription(category),
                    )
                }
                items(modules, key = { it.id }) { module ->
                    val level = profile.unlockTree.permanentModules[module.id] ?: 0
                    val isMaxed = level >= module.maxLevel
                    val cost = module.upgradeCost(level)
                    val accent = moduleAccent(module.id)
                    FeatureCard(
                        title = module.label,
                        body = module.description,
                        meta = "${categoryLabel(module.category)} | Lv.$level/${module.maxLevel} | ${if (isMaxed) "MAXED" else "Next $cost"}",
                        accent = accent,
                    ) {
                        Button(
                            onClick = { onUpgrade(module.id) },
                            enabled = profile.credits >= cost && !isMaxed,
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                        ) {
                            Text(if (isMaxed) "MAX" else "Upgrade")
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun CodexScreen(
    profile: PlayerProfile,
    content: GameContentBundle,
    onBack: () -> Unit,
) {
    PosterScaffold(
        title = "Codex",
        subtitle = "Osservazioni dal campo. Quello che scopri resta legato all'account.",
        onBack = onBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GlassPanel(accent = PampaLavender) {
                    Text("Survey progress", color = PampaLavender, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${profile.unlockTree.codexBiomeIds.size}/${content.biomes.size} biomi  |  ${profile.unlockTree.codexEnemyIds.size}/${content.enemies.size} nemici",
                        color = PampaText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                    )
                }
            }
            item { SectionHeader("Biomi", "Climi, palette e minacce che definiscono il tono della run.") }
            items(content.biomes) { biome ->
                val discovered = biome.id in profile.unlockTree.codexBiomeIds
                FeatureCard(
                    title = biome.label,
                    body = if (discovered) biome.subtitle else "Ancora da sondare in run.",
                    meta = if (discovered) "Scoperto" else "Ignoto",
                    accent = if (discovered) PampaCyan else PampaMuted,
                )
            }
            item { SectionHeader("Nemici", "Elites, fodder e boss che popolano l'arena.") }
            items(content.enemies) { enemy ->
                val discovered = enemy.id in profile.unlockTree.codexEnemyIds
                FeatureCard(
                    title = enemy.label,
                    body = if (discovered) enemy.role else "Profilo non ancora acquisito.",
                    meta = if (discovered) enemy.role else "Ignoto",
                    accent = if (discovered) PampaAmber else PampaMuted,
                )
            }
            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun MissionsScreen(
    profile: PlayerProfile,
    onBack: () -> Unit,
) {
    PosterScaffold(
        title = "Missioni",
        subtitle = "Milestone persistenti dell'account e premi gia assorbiti dal profilo.",
        onBack = onBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(profile.activeMissions) { mission ->
                val progressRatio = mission.progress.toFloat() / mission.target.coerceAtLeast(1)
                val accent = when {
                    mission.claimed -> PampaMint
                    mission.completed -> PampaAmber
                    else -> PampaCyan
                }
                GlassPanel(accent = accent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(mission.label, color = PampaText, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(mission.description, color = PampaMuted)
                        }
                        StatusChip(
                            text = if (mission.claimed) "Claimed" else if (mission.completed) "Done" else "Tracking",
                            accent = accent,
                        )
                    }
                    ProgressBar(progressRatio, accent)
                    Text("${mission.progress}/${mission.target} | +${mission.rewardCredits} crediti | +${mission.rewardArchiveXp} XP", color = PampaMuted, fontSize = 12.sp)
                }
            }
            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: GameSettings,
    repository: SettingsRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    PosterScaffold(
        title = "Impostazioni",
        subtitle = "Ottimizza feedback, layout touch e leggibilita dell'arena.",
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlassPanel(accent = PampaCyan) {
                Text("Grafica", color = PampaText, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GraphicsProfile.values().forEach { profile ->
                        FilterChip(
                            selected = settings.graphicsProfile == profile,
                            onClick = { scope.launch { repository.setGraphicsProfile(profile) } },
                            label = { Text(profile.name) },
                        )
                    }
                }
                SettingSwitch("Low VFX", settings.lowVfx) { scope.launch { repository.setLowVfx(it) } }
                SettingSwitch("HUD contrastato", settings.highContrastHud) { scope.launch { repository.setHighContrastHud(it) } }
                SettingSwitch("Screen shake", settings.screenShakeEnabled) { scope.launch { repository.setScreenShakeEnabled(it) } }
            }
            GlassPanel(accent = PampaAmber) {
                Text("Feedback", color = PampaText, fontWeight = FontWeight.Black, fontSize = 20.sp)
                SettingSwitch("Haptics", settings.hapticsEnabled) { scope.launch { repository.setHapticsEnabled(it) } }
                SettingSlider("Volume musica", settings.musicVolume) { scope.launch { repository.setMusicVolume(it) } }
                SettingSlider("Volume SFX", settings.sfxVolume) { scope.launch { repository.setSfxVolume(it) } }
            }
            GlassPanel(accent = PampaMint) {
                Text("Touch e accessibilita", color = PampaText, fontWeight = FontWeight.Black, fontSize = 20.sp)
                SettingSlider("Scala testo", settings.uiTextScale, 0.85f..1.35f) { scope.launch { repository.setUiTextScale(it) } }
                SettingSwitch("Layout invertito", settings.hudLayout.flipped) {
                    scope.launch { repository.setHudLayout(settings.hudLayout.copy(flipped = it)) }
                }
                SettingSlider("Scala controlli", settings.hudLayout.controlScale, 0.8f..1.4f) {
                    scope.launch { repository.setHudLayout(settings.hudLayout.copy(controlScale = it)) }
                }
                SettingSlider("Offset azioni", settings.hudLayout.actionColumnYOffset, -1f..1f) {
                    scope.launch { repository.setHudLayout(settings.hudLayout.copy(actionColumnYOffset = it)) }
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun HistoryScreen(
    history: List<RunHistoryEntry>,
    onBack: () -> Unit,
) {
    PosterScaffold(
        title = "Cronologia",
        subtitle = "Ultime run archiviate localmente sul dispositivo.",
        onBack = onBack,
    ) {
        if (history.isEmpty()) {
            GlassPanel(accent = PampaCyan) {
                Text("Nessuna run salvata ancora.", color = PampaText, fontWeight = FontWeight.Black)
                Text("Entra in arena e il log comincera a riempirsi automaticamente.", color = PampaMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(history) { entry ->
                    FeatureCard(
                        title = "${entry.shipId.removePrefix("ship_").replaceFirstChar { it.uppercase() }} | Score ${entry.score}",
                        body = "Wave ${entry.waveReached} | ${entry.kills} kill | ${entry.bossesDefeated} boss | ${entry.durationSeconds}s",
                        meta = "${entry.biomeId} | +${entry.creditsEarned} crediti | +${entry.archiveXpEarned} XP",
                        accent = PampaLavender,
                    )
                }
                item { Spacer(modifier = Modifier.navigationBarsPadding()) }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    body: String,
    meta: String,
    accent: Color,
    trailing: (@Composable () -> Unit)? = null,
) {
    val ui = chrome()
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = ui.textArgb.asColor(), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(body, color = ui.mutedArgb.asColor())
                Text(meta, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailing()
            }
        }
    }
}

@Composable
private fun ProgressBar(
    ratio: Float,
    accent: Color,
) {
    val ui = chrome()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(ui.panelBottomArgb.asColor().copy(alpha = 0.32f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .height(12.dp)
                .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.45f)))),
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    val ui = chrome()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ui.panelTopArgb.asColor().copy(alpha = 0.22f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = ui.textArgb.asColor())
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = ui.textArgb.asColor(), checkedTrackColor = ui.cyanArgb.asColor()),
        )
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChanged: (Float) -> Unit,
) {
    val ui = chrome()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ui.panelTopArgb.asColor().copy(alpha = 0.22f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("$label: ${"%.2f".format(value)}", color = ui.textArgb.asColor())
        Slider(
            value = value,
            onValueChange = onValueChanged,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = ui.cyanArgb.asColor(),
                activeTrackColor = ui.cyanArgb.asColor(),
                inactiveTrackColor = Color(0x40293A4A),
            ),
        )
    }
}
