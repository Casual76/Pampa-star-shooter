package com.antigravity.pampastarshooter.game.android

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.antigravity.pampastarshooter.core.model.GameSettings

enum class SoundEvent {
    Dash,
    Pulse,
    Shield,
    Hit,
    LevelUp,
    GameOver,
}

enum class HapticPulse {
    Soft,
    Medium,
    Strong,
}

interface AudioController {
    fun updateSettings(settings: GameSettings)
    fun play(event: SoundEvent)
}

interface HapticsController {
    fun updateSettings(settings: GameSettings)
    fun pulse(type: HapticPulse)
}

class AndroidAudioController : AudioController {
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    private var settings = GameSettings()

    override fun updateSettings(settings: GameSettings) {
        this.settings = settings
    }

    override fun play(event: SoundEvent) {
        if (settings.sfxVolume <= 0.01f) return
        when (event) {
            SoundEvent.Dash -> tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 40)
            SoundEvent.Pulse -> tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 60)
            SoundEvent.Shield -> tone.startTone(ToneGenerator.TONE_PROP_ACK, 45)
            SoundEvent.Hit -> tone.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP, 28)
            SoundEvent.LevelUp -> tone.startTone(ToneGenerator.TONE_PROP_NACK, 70)
            SoundEvent.GameOver -> tone.startTone(ToneGenerator.TONE_SUP_ERROR, 120)
        }
    }
}

class AndroidHapticsController(
    context: Context,
) : HapticsController {
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private var settings = GameSettings()

    override fun updateSettings(settings: GameSettings) {
        this.settings = settings
    }

    override fun pulse(type: HapticPulse) {
        if (!settings.hapticsEnabled || vibrator == null) return
        val duration = when (type) {
            HapticPulse.Soft -> 12L
            HapticPulse.Medium -> 22L
            HapticPulse.Strong -> 36L
        }
        val amplitude = when (type) {
            HapticPulse.Soft -> 80
            HapticPulse.Medium -> 150
            HapticPulse.Strong -> 220
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (_: SecurityException) {
            // If the permission is missing or revoked, haptics should silently disable instead of killing the render thread.
        } catch (_: RuntimeException) {
            // Some vendor implementations can still throw unexpectedly; gameplay should stay alive.
        }
    }
}
