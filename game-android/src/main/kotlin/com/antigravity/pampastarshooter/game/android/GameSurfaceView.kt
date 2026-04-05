package com.antigravity.pampastarshooter.game.android

import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.antigravity.pampastarshooter.core.model.FrameSnapshot
import com.antigravity.pampastarshooter.core.model.GameEngine
import com.antigravity.pampastarshooter.core.model.GameSettings
import com.antigravity.pampastarshooter.core.model.InputSnapshot
import com.antigravity.pampastarshooter.core.model.PlayerProfile
import com.antigravity.pampastarshooter.core.model.RunConfig
import com.antigravity.pampastarshooter.core.model.RunPhase
import com.antigravity.pampastarshooter.core.model.RunResult
import kotlin.math.max

class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val renderer = AndroidGameRenderer()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var running = false

    @Volatile
    private var latestInput = InputSnapshot()

    @Volatile
    private var latestSettings = GameSettings()

    private var engine: GameEngine? = null
    private var audioController: AudioController? = null
    private var hapticsController: HapticsController? = null
    private var loopThread: Thread? = null
    private var lastPhase: RunPhase = RunPhase.Idle

    var onSnapshot: ((FrameSnapshot) -> Unit)? = null
    var onRunFinished: ((RunResult) -> Unit)? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        keepScreenOn = true
    }

    fun bindControllers(
        audioController: AudioController,
        hapticsController: HapticsController,
    ) {
        this.audioController = audioController
        this.hapticsController = hapticsController
        audioController.updateSettings(latestSettings)
        hapticsController.updateSettings(latestSettings)
    }

    fun updateSettings(settings: GameSettings) {
        latestSettings = settings
        audioController?.updateSettings(settings)
        hapticsController?.updateSettings(settings)
    }

    fun startRun(
        engine: GameEngine,
        config: RunConfig,
        profile: PlayerProfile,
    ) {
        stopLoop()
        this.engine = engine
        this.lastPhase = RunPhase.Idle
        engine.startRun(config, profile)
        if (holder.surface.isValid) startLoop()
    }

    fun updateInput(input: InputSnapshot) {
        latestInput = input
    }

    fun pauseGame() {
        engine?.pause()
    }

    fun resumeGame() {
        engine?.resume()
    }

    fun releaseSession() {
        stopLoop()
        engine = null
        latestInput = InputSnapshot()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    private fun startLoop() {
        if (running || engine == null || !holder.surface.isValid) return
        running = true
        loopThread = Thread(
            {
                var lastFrameNanos = System.nanoTime()
                var publishCountdown = 0
                var lastPublishedSnapshot: FrameSnapshot? = null
                var lastRenderedSnapshot: FrameSnapshot? = null
                while (running) {
                    val activeEngine = engine ?: break
                    val now = System.nanoTime()
                    val deltaSeconds = max(0f, (now - lastFrameNanos) / 1_000_000_000f)
                    lastFrameNanos = now
                    activeEngine.submitInput(latestInput)
                    activeEngine.step(deltaSeconds)
                    val snapshot = activeEngine.currentSnapshot()
                    publishPhaseEffects(snapshot)
                    if (snapshot.phase == RunPhase.Running || snapshot !== lastRenderedSnapshot) {
                        drawSnapshot(snapshot)
                        lastRenderedSnapshot = snapshot
                    }
                    val shouldPublish = when (snapshot.phase) {
                        RunPhase.Running -> publishCountdown++ % 2 == 0
                        else -> snapshot !== lastPublishedSnapshot
                    }
                    if (shouldPublish) {
                        lastPublishedSnapshot = snapshot
                        mainHandler.post { onSnapshot?.invoke(snapshot) }
                    }
                    activeEngine.consumeLatestResult()?.let { result ->
                        mainHandler.post { onRunFinished?.invoke(result) }
                    }
                    val frameMillis = ((System.nanoTime() - now) / 1_000_000L).coerceAtLeast(0L)
                    val targetFrameMillis = if (snapshot.phase == RunPhase.Running) 16L else 33L
                    val sleepMillis = (targetFrameMillis - frameMillis).coerceAtLeast(1L)
                    try {
                        Thread.sleep(sleepMillis)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            },
            "PampaSurfaceLoop",
        ).also { it.start() }
    }

    private fun stopLoop() {
        running = false
        loopThread?.interrupt()
        loopThread?.join(500)
        loopThread = null
    }

    private fun drawSnapshot(snapshot: FrameSnapshot) {
        val canvas: Canvas = holder.lockCanvas() ?: return
        try {
            renderer.render(canvas, snapshot, latestSettings, width, height)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun publishPhaseEffects(snapshot: FrameSnapshot) {
        if (snapshot.phase == lastPhase) return
        when (snapshot.phase) {
            RunPhase.ChoosingUpgrade -> {
                audioController?.play(SoundEvent.LevelUp)
                hapticsController?.pulse(HapticPulse.Medium)
            }
            RunPhase.GameOver -> {
                audioController?.play(SoundEvent.GameOver)
                hapticsController?.pulse(HapticPulse.Strong)
            }
            else -> Unit
        }
        lastPhase = snapshot.phase
    }
}
