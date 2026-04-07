package com.antigravity.pampastarshooter.ui

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.antigravity.pampastarshooter.AppContainer
import com.antigravity.pampastarshooter.core.content.GameContentBundle
import com.antigravity.pampastarshooter.core.engine.PampaGameEngine
import com.antigravity.pampastarshooter.core.model.FrameSnapshot
import com.antigravity.pampastarshooter.core.model.GameSettings
import com.antigravity.pampastarshooter.core.model.InputSnapshot
import com.antigravity.pampastarshooter.core.model.MissionSnapshot
import com.antigravity.pampastarshooter.core.model.OverlaySnapshot
import com.antigravity.pampastarshooter.core.model.PlayerProfile
import com.antigravity.pampastarshooter.core.model.PlayerSnapshot
import com.antigravity.pampastarshooter.core.model.RunConfig
import com.antigravity.pampastarshooter.core.model.RunMode
import com.antigravity.pampastarshooter.core.model.RunPhase
import com.antigravity.pampastarshooter.core.model.RunResult
import com.antigravity.pampastarshooter.core.model.Vector2
import com.antigravity.pampastarshooter.core.model.WarningSnapshot
import com.antigravity.pampastarshooter.core.model.displayLabel
import com.antigravity.pampastarshooter.core.model.findCampaignNode
import com.antigravity.pampastarshooter.game.android.GameSurfaceView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun GameScreen(
    container: AppContainer,
    profile: PlayerProfile,
    settings: GameSettings,
    runSetup: PendingRunSetup,
    onExit: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ui = chrome()
    val content = remember { container.contentRepository.load() }
    var frame by remember { mutableStateOf(FrameSnapshot()) }
    var result by remember { mutableStateOf<RunResult?>(null) }
    var surfaceView by remember { mutableStateOf<GameSurfaceView?>(null) }
    var movement by remember { mutableStateOf(Vector2.Zero) }
    var dashTick by remember { mutableIntStateOf(0) }
    var pulseTick by remember { mutableIntStateOf(0) }
    var shieldTick by remember { mutableIntStateOf(0) }
    var mineTick by remember { mutableIntStateOf(0) }
    var pauseTick by remember { mutableIntStateOf(0) }
    var resumeTick by remember { mutableIntStateOf(0) }
    var rerollTick by remember { mutableIntStateOf(0) }
    var selectedUpgrade by remember { mutableStateOf<Int?>(null) }
    var runSeed by remember { mutableLongStateOf(java.lang.System.currentTimeMillis()) }

    DisposableEffect(Unit) {
        onDispose { surfaceView?.releaseSession() }
    }

    LaunchedEffect(surfaceView, runSeed, profile.selectedShipId, runSetup) {
        surfaceView?.startRun(
            engine = PampaGameEngine(container.contentRepository),
            config = RunConfig(
                shipId = profile.selectedShipId,
                seed = runSeed,
                mode = runSetup.mode,
                campaignNodeId = runSetup.campaignNodeId,
                forcedBiomeId = runSetup.forcedBiomeId,
                objective = runSetup.objective,
                modifiers = runSetup.modifiers,
                hudLayout = settings.hudLayout,
            ),
            profile = profile,
        )
        result = null
    }

    LaunchedEffect(surfaceView) {
        var sentDash = 0
        var sentPulse = 0
        var sentShield = 0
        var sentMine = 0
        var sentPause = 0
        var sentResume = 0
        var sentReroll = 0
        var lastInput = InputSnapshot()
        while (isActive) {
            val currentSelection = selectedUpgrade
            val nextInput = InputSnapshot(
                movement = movement,
                dashPressed = dashTick != sentDash,
                pulsePressed = pulseTick != sentPulse,
                shieldPressed = shieldTick != sentShield,
                minePressed = mineTick != sentMine,
                pausePressed = pauseTick != sentPause,
                resumePressed = resumeTick != sentResume,
                rerollPressed = rerollTick != sentReroll,
                selectedUpgradeIndex = currentSelection,
            )
            if (nextInput != lastInput) {
                surfaceView?.updateInput(nextInput)
                lastInput = nextInput
            }
            sentDash = dashTick
            sentPulse = pulseTick
            sentShield = shieldTick
            sentMine = mineTick
            sentPause = pauseTick
            sentResume = resumeTick
            sentReroll = rerollTick
            if (currentSelection != null) {
                selectedUpgrade = null
            }
            delay(
                when (frame.phase) {
                    RunPhase.ChoosingUpgrade -> 8L
                    RunPhase.Running -> 16L
                    else -> 33L
                },
            )
        }
    }

    LaunchedEffect(settings, surfaceView) {
        surfaceView?.updateSettings(settings)
        container.audioController.updateSettings(settings)
        container.hapticsController.updateSettings(settings)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ui.inkArgb.asColor()),
    ) {
        val isLandscape = maxWidth > maxHeight
        val chromePadding = if (isLandscape) 18.dp else 14.dp

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                GameSurfaceView(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    bindControllers(container.audioController, container.hapticsController)
                    updateSettings(settings)
                    onSnapshot = { snapshot -> frame = snapshot }
                    onRunFinished = { finished ->
                        result = finished
                        scope.launch {
                            container.profileRepository.applyRunResult(finished)
                            container.historyRepository.record(finished.historyEntry)
                        }
                    }
                    surfaceView = this
                }
            },
            update = { view ->
                view.updateSettings(settings)
                surfaceView = view
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = chromePadding, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            TopHud(
                frame = frame,
                onBack = {
                    if (frame.phase == RunPhase.Running) pauseTick++ else onExit()
                },
            )

            val warning = frame.warnings.firstOrNull()
            if (warning != null && result == null && frame.phase == RunPhase.Running) {
                WarningBanner(warning)
            } else {
                Spacer(Modifier.height(if (isLandscape) 6.dp else 0.dp))
            }

            Spacer(Modifier.weight(1f))

            BottomControlDock(
                frame = frame,
                settings = settings,
                isLandscape = isLandscape,
                onMove = { movement = it },
                onDash = { dashTick++ },
                onPulse = { pulseTick++ },
                onShield = { shieldTick++ },
                onMine = { mineTick++ },
            )
        }

        if (frame.phase == RunPhase.ChoosingUpgrade && frame.overlay is OverlaySnapshot.LevelUp) {
            LevelUpOverlay(
                overlay = frame.overlay as OverlaySnapshot.LevelUp,
                onPick = { choiceIndex ->
                    selectedUpgrade = choiceIndex
                    surfaceView?.updateInput(
                        InputSnapshot(
                            movement = movement,
                            selectedUpgradeIndex = choiceIndex,
                        ),
                    )
                },
                onReroll = {
                    rerollTick++
                    surfaceView?.updateInput(
                        InputSnapshot(
                            movement = movement,
                            rerollPressed = true,
                        ),
                    )
                },
            )
        }

        if (frame.phase == RunPhase.Paused && result == null) {
            PauseOverlay(
                onResume = { resumeTick++ },
                onExit = onExit,
            )
        }

        if (result != null) {
            GameOverOverlay(
                content = content,
                result = result!!,
                onReplay = {
                    runSeed = java.lang.System.currentTimeMillis()
                    result = null
                },
                onExit = onExit,
            )
        }
    }
}

@Composable
private fun TopHud(
    frame: FrameSnapshot,
    onBack: () -> Unit,
) {
    val ui = chrome()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallActionPill(
                    label = if (frame.phase == RunPhase.Running) "Pause" else "Hangar",
                    onClick = onBack,
                )
                Surface(
                    color = ui.panelTopArgb.asColor().copy(alpha = 0.72f),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = frame.hud.shipLabel.ifBlank { "Striker" },
                            color = ui.textArgb.asColor(),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                        )
                        Text(
                            text = "${frame.hud.modeLabel} | Wave ${frame.hud.wave} | ${frame.hud.biomeLabel}",
                            color = ui.mutedArgb.asColor(),
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HudChip("Score ${frame.hud.score}")
                    HudChip("+${frame.hud.archiveXpProjected} XP", accent = ui.amberArgb.asColor())
                }
                frame.hud.objective?.let { objective ->
                    HudChip(objective.label, accent = ui.cyanArgb.asColor())
                    HudChip(
                        objective.progressLabel,
                        accent = if (objective.completed) ui.mintArgb.asColor() else ui.amberArgb.asColor(),
                    )
                }
                frame.hud.activeEventLabel?.let {
                    HudChip(it, accent = ui.lavenderArgb.asColor())
                }
                frame.runMissions.take(2).forEach { mission ->
                    MissionStrip(mission)
                }
            }
        }
    }
}

@Composable
private fun WarningBanner(warning: WarningSnapshot) {
    val ui = chrome()
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = Color(0xD8131C27),
            shape = RoundedCornerShape(999.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = warning.title.uppercase(),
                    color = ui.textArgb.asColor(),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontSize = 13.sp,
                )
                Text(
                    text = warning.subtitle,
                    color = ui.mutedArgb.asColor(),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun BottomControlDock(
    frame: FrameSnapshot,
    settings: GameSettings,
    isLandscape: Boolean,
    onMove: (Vector2) -> Unit,
    onDash: () -> Unit,
    onPulse: () -> Unit,
    onShield: () -> Unit,
    onMine: () -> Unit,
) {
    val ui = chrome()
    val player = frame.player
    val dockScale = settings.hudLayout.controlScale
    val joystickFirst = !settings.hudLayout.flipped
    val joystickModifier = Modifier.size(if (isLandscape) 180.dp else 164.dp * dockScale)
    val actionYOffset = (settings.hudLayout.actionColumnYOffset * 18f).dp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ui.panelBottomArgb.asColor().copy(alpha = 0.8f),
        shape = RoundedCornerShape(if (isLandscape) 30.dp else 28.dp),
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                if (joystickFirst) {
                    JoystickControl(joystickModifier, onMove)
                }
                StatusCluster(
                    frame = frame,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 18.dp),
                )
                ActionCluster(
                    player = player,
                    modifier = Modifier.offset(y = actionYOffset),
                    onDash = onDash,
                    onPulse = onPulse,
                    onShield = onShield,
                    onMine = onMine,
                )
                if (!joystickFirst) {
                    Spacer(Modifier.width(18.dp))
                    JoystickControl(joystickModifier, onMove)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusCluster(frame = frame, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (joystickFirst) {
                        JoystickControl(joystickModifier, onMove)
                    }
                    ActionCluster(
                        player = player,
                        modifier = Modifier.offset(y = actionYOffset),
                        onDash = onDash,
                        onPulse = onPulse,
                        onShield = onShield,
                        onMine = onMine,
                    )
                    if (!joystickFirst) {
                        JoystickControl(joystickModifier, onMove)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCluster(
    frame: FrameSnapshot,
    modifier: Modifier = Modifier,
) {
    val ui = chrome()
    val player = frame.player
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = frame.hud.shipLabel.ifBlank { "Run" },
                    color = ui.textArgb.asColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Text(
                    text = "Kills ${frame.hud.kills} | Credits ${frame.hud.credits}",
                    color = ui.mutedArgb.asColor(),
                    fontSize = 12.sp,
                )
            }
            Text(
                text = "Lv.${player?.level ?: 1}",
                color = ui.amberArgb.asColor(),
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
            )
        }
        player?.let {
            StatBar("Hull", it.hp / it.maxHp, Color(0xFF5CE7FF), "${it.hp.toInt()}/${it.maxHp.toInt()}")
            StatBar("Sync", it.xp / it.xpToNext, Color(0xFFFFB85A), "${it.xp.toInt()}/${it.xpToNext.toInt()}")
        }
    }
}

@Composable
private fun StatBar(
    label: String,
    ratio: Float,
    accent: Color,
    value: String,
) {
    val ui = chrome()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label.uppercase(), color = ui.mutedArgb.asColor(), fontSize = 11.sp, letterSpacing = 1.sp)
            Text(value, color = ui.textArgb.asColor(), fontSize = 11.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x40131D28)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent, accent.copy(alpha = 0.55f)),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun JoystickControl(
    modifier: Modifier,
    onMove: (Vector2) -> Unit,
) {
    val ui = chrome()
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0x4D16222E))
            .border(1.dp, ui.cyanArgb.asColor().copy(alpha = 0.28f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start -> onMove(normalizeDrag(start, size)) },
                    onDragEnd = { onMove(Vector2.Zero) },
                    onDragCancel = { onMove(Vector2.Zero) },
                    onDrag = { change, _ -> onMove(normalizeDrag(change.position, size)) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(ui.cyanArgb.asColor(), Color(0xFF2D8FA7)),
                    ),
                ),
        )
    }
}

private fun normalizeDrag(position: Offset, size: IntSize): Vector2 {
    val center = Offset(size.width / 2f, size.height / 2f)
    val dx = position.x - center.x
    val dy = position.y - center.y
    val radius = min(size.width, size.height) * 0.42f
    val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val clamped = min(distance, radius)
    return Vector2(dx / distance * clamped / radius, dy / distance * clamped / radius)
}

@Composable
private fun ActionCluster(
    player: PlayerSnapshot?,
    modifier: Modifier = Modifier,
    onDash: () -> Unit,
    onPulse: () -> Unit,
    onShield: () -> Unit,
    onMine: () -> Unit,
) {
    val activePlayer = player
    Column(
        modifier = modifier.width(190.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AbilityButton(
                label = "Dash",
                icon = Icons.Rounded.Bolt,
                status = cooldownLabel(player?.dashCooldown),
                modifier = Modifier.weight(1f),
                onClick = onDash,
                accent = Color(0xFF5CE7FF),
            )
            AbilityButton(
                label = "Pulse",
                icon = Icons.Rounded.FlashOn,
                status = cooldownLabel(player?.pulseCooldown),
                modifier = Modifier.weight(1f),
                onClick = onPulse,
                accent = Color(0xFFA8B4FF),
            )
        }
        if (activePlayer != null && (activePlayer.hasShield || activePlayer.hasMine)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (activePlayer.hasShield) {
                    AbilityButton(
                        label = "Shield",
                        icon = Icons.Rounded.Security,
                        status = cooldownLabel(activePlayer.shieldCooldown),
                        modifier = Modifier.weight(1f),
                        onClick = onShield,
                        accent = Color(0xFF8DFFBF),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (activePlayer.hasMine) {
                    AbilityButton(
                        label = "Mine",
                        icon = Icons.Rounded.TripOrigin,
                        status = cooldownLabel(activePlayer.mineCooldown),
                        modifier = Modifier.weight(1f),
                        onClick = onMine,
                        accent = Color(0xFFFFC970),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AbilityButton(
    label: String,
    icon: ImageVector,
    status: String,
    modifier: Modifier,
    onClick: () -> Unit,
    accent: Color = Color(0xFF5CE7FF),
) {
    val ui = chrome()
    val ready = status == "READY"
    val readyGlow by animateFloatAsState(
        targetValue = if (ready) 0.24f else 0.1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 220f),
        label = "abilityGlow",
    )
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = ui.panelTopArgb.asColor().copy(alpha = if (ready) 0.94f else 0.8f),
            contentColor = ui.textArgb.asColor(),
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = readyGlow)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(status, fontSize = 11.sp, color = if (ready) accent else ui.mutedArgb.asColor())
        }
    }
}

private fun cooldownLabel(value: Float?): String {
    if (value == null) return "--"
    if (value <= 0.05f) return "READY"
    val deciseconds = (value * 10f).toInt().coerceAtLeast(0)
    return "${deciseconds / 10}.${deciseconds % 10}s"
}

private fun formatUnlockedShipLabel(id: String): String =
    id.removePrefix("ship_").replace("_", " ").replaceFirstChar { it.uppercase() }

private fun formatPerkLabel(id: String): String =
    id.replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun formatModifierLabel(id: String): String =
    id.replace('_', ' ').replaceFirstChar { it.uppercase() }

@Composable
private fun SmallActionPill(
    label: String,
    onClick: () -> Unit,
) {
    val ui = chrome()
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = ui.textArgb.asColor().copy(alpha = 0.92f),
            contentColor = Color(0xFF10141B),
        ),
        shape = RoundedCornerShape(999.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HudChip(
    text: String,
    accent: Color = Color(0xFF5CE7FF),
) {
    val ui = chrome()
    Surface(
        color = ui.panelTopArgb.asColor().copy(alpha = 0.72f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MissionStrip(mission: MissionSnapshot) {
    val ui = chrome()
    Surface(
        color = Color(0x8E0D151E),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(mission.label, color = ui.textArgb.asColor(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${mission.progress}/${mission.target}",
                color = if (mission.completed) ui.mintArgb.asColor() else ui.mutedArgb.asColor(),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun LevelUpOverlay(
    overlay: OverlaySnapshot.LevelUp,
    onPick: (Int) -> Unit,
    onReroll: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA060A10)),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Panel(
                title = "Module Draft",
                subtitle = "Level ${overlay.level} | scegli una carta per continuare",
            ) {
                overlay.choices.forEachIndexed { index, choice ->
                    FilledTonalButton(
                        onClick = { onPick(index) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF111A26),
                            contentColor = Color.White,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(choice.label, fontWeight = FontWeight.Black)
                            Text(choice.description, fontSize = 12.sp, color = Color(0xFFB4C9D8))
                            Text(
                                "${choice.category} | ${choice.currentStacks}/${choice.maxStacks}",
                                fontSize = 11.sp,
                                color = Color(0xFF6FE3FF),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedButton(
                    onClick = onReroll,
                    enabled = overlay.rerollsRemaining > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reroll (${overlay.rerollsRemaining})")
                }
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onExit: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0x99070B11)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Panel(title = "Run in pausa", subtitle = "Riprendi o rientra in hangar.") {
                FilledTonalButton(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                    Text("Riprendi")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                    Text("Esci")
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    content: GameContentBundle,
    result: RunResult,
    onReplay: () -> Unit,
    onExit: () -> Unit,
) {
    val ui = chrome()
    val campaignNode = result.campaignNodeId?.let(content::findCampaignNode)
    val campaignRewards = buildList {
        addAll(campaignNode?.rewardModifierIds.orEmpty().map(::formatModifierLabel))
        if (campaignNode?.unlockEndless == true) {
            add("Endless Frontier")
        }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xB205080E)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Panel(
                title = when {
                    result.success && result.config.mode == RunMode.Campaign -> "Sector Cleared"
                    result.config.mode == RunMode.Campaign -> "Sector Failed"
                    else -> "Run Archiviata"
                },
                subtitle = when {
                    result.config.mode == RunMode.Campaign ->
                        "${campaignNode?.label ?: "Campaign"} | Wave ${result.waveReached} | ${result.kills} kill"
                    else -> "Wave ${result.waveReached} | ${result.kills} kill | ${result.bossesDefeated} boss"
                },
            ) {
                Text(
                    text = "Score ${result.score}",
                    color = ui.textArgb.asColor(),
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                )
                Text(
                    text = "+${result.creditsEarned} crediti | +${result.archiveXpEarned} XP",
                    color = ui.amberArgb.asColor(),
                    fontWeight = FontWeight.Bold,
                )
                result.config.objective?.let {
                    Text(
                        text = it.displayLabel(),
                        color = ui.mutedArgb.asColor(),
                        fontSize = 12.sp,
                    )
                }
                if (result.config.modifiers.isNotEmpty()) {
                    Text(
                        text = "Mutators: ${result.config.modifiers.joinToString(transform = ::formatModifierLabel)}",
                        color = ui.mutedArgb.asColor(),
                        fontSize = 12.sp,
                    )
                }
                if (result.unlockedShipIds.isNotEmpty()) {
                    Text(
                        text = "Unlocked ships: ${result.unlockedShipIds.joinToString(transform = ::formatUnlockedShipLabel)}",
                        color = ui.mintArgb.asColor(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
                if (result.unlockedPerkIds.isNotEmpty()) {
                    Text(
                        text = "Unlocked perks: ${result.unlockedPerkIds.joinToString(transform = ::formatPerkLabel)}",
                        color = ui.lavenderArgb.asColor(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
                if (campaignRewards.isNotEmpty()) {
                    Text(
                        text = "Campaign rewards: ${campaignRewards.joinToString()}",
                        color = ui.amberArgb.asColor(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = onReplay, modifier = Modifier.fillMaxWidth()) {
                    Text("Nuova run")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                    Text("Hangar")
                }
            }
        }
    }
}

@Composable
private fun Panel(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ui = chrome()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF0101721), Color(0xEC090E15)),
                ),
            )
            .border(1.dp, ui.cyanArgb.asColor().copy(alpha = if (ui.highContrast) 0.42f else 0.14f), RoundedCornerShape(30.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            Text(
                title,
                color = ui.textArgb.asColor(),
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 26.sp,
            )
            Text(subtitle, color = ui.mutedArgb.asColor())
            content()
        },
    )
}
