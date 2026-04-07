package com.antigravity.pampastarshooter.game.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.antigravity.pampastarshooter.core.model.FrameSnapshot
import com.antigravity.pampastarshooter.core.model.GameSettings
import com.antigravity.pampastarshooter.core.model.GraphicsProfile
import com.antigravity.pampastarshooter.core.model.PlayerSnapshot
import com.antigravity.pampastarshooter.core.model.Vector2
import com.antigravity.pampastarshooter.core.model.VisualEffectKind
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private data class RenderPalette(
    val top: Int,
    val bottom: Int,
    val grid: Int,
    val glow: Int,
    val accent: Int,
    val warning: Int,
)

private data class RenderQuality(
    val starCount: Int,
    val ambientGlowAlpha: Float,
    val borderAlpha: Int,
    val particleScale: Float,
    val scanlineAlpha: Int,
)

class AndroidGameRenderer(context: Context) {
    private val assets = GameAssetCatalog(context.applicationContext)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val additivePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        blendMode = BlendMode.SCREEN
    }
    private val path = Path()
    private val rect = RectF()
    private val matrix = Matrix()

    fun render(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        settings: GameSettings,
        surfaceWidth: Int,
        surfaceHeight: Int,
    ) {
        val palette = paletteFor(snapshot.hud.biomeLabel)
        val quality = qualityFor(settings, surfaceWidth, surfaceHeight)
        val fitScale = min(
            surfaceWidth / snapshot.bounds.width,
            surfaceHeight / snapshot.bounds.height,
        )
        val fillScale = max(
            surfaceWidth / snapshot.bounds.width,
            surfaceHeight / snapshot.bounds.height,
        )
        val scale = fitScale + (fillScale - fitScale) * if (surfaceWidth < surfaceHeight) 0.34f else 0f
        val offsetX = (surfaceWidth - snapshot.bounds.width * scale) * 0.5f
        val offsetY = (surfaceHeight - snapshot.bounds.height * scale) * if (surfaceWidth < surfaceHeight) 0.42f else 0.5f

        canvas.save()
        if (settings.screenShakeEnabled) {
            val shake = snapshot.visualFx.cameraShake.coerceIn(0f, 1f)
            if (shake > 0f) {
                val phase = snapshot.elapsedSeconds * 54f
                canvas.translate(cos(phase) * shake * 10f, sin(phase * 1.37f) * shake * 8f)
            }
        }

        drawBackdrop(canvas, surfaceWidth, surfaceHeight, snapshot, palette, quality)
        drawArenaPlane(canvas, snapshot, scale, offsetX, offsetY, palette, quality)
        drawVisualEffects(canvas, snapshot, scale, offsetX, offsetY, behindEntities = true)
        drawPickups(canvas, snapshot, scale, offsetX, offsetY)
        drawProjectiles(canvas, snapshot, scale, offsetX, offsetY)
        drawEnemies(canvas, snapshot, scale, offsetX, offsetY)
        snapshot.player?.let { drawPlayer(canvas, it, snapshot, scale, offsetX, offsetY, palette) }
        drawParticles(canvas, snapshot, scale, offsetX, offsetY, quality)
        drawVisualEffects(canvas, snapshot, scale, offsetX, offsetY, behindEntities = false)
        drawDamageFlash(canvas, snapshot, surfaceWidth, surfaceHeight, palette)
        drawVignette(canvas, surfaceWidth, surfaceHeight)
        canvas.restore()
    }

    private fun drawBackdrop(
        canvas: Canvas,
        width: Int,
        height: Int,
        snapshot: FrameSnapshot,
        palette: RenderPalette,
        quality: RenderQuality,
    ) {
        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            palette.top,
            palette.bottom,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        drawAmbientGlow(canvas, width * 0.16f, height * 0.15f, width * 0.52f, palette.glow, quality.ambientGlowAlpha)
        drawAmbientGlow(canvas, width * 0.82f, height * 0.76f, width * 0.44f, palette.accent, quality.ambientGlowAlpha * 0.8f)
        drawAmbientGlow(canvas, width * 0.52f, height * 0.36f, width * 0.32f, Color.WHITE, quality.ambientGlowAlpha * 0.25f)

        repeat(quality.starCount) { index ->
            val baseX = ((index * 71) % width).toFloat()
            val drift = ((snapshot.elapsedSeconds * (4f + index % 5)) + index * 13f) % (height + 120f)
            val x = (baseX + (index % 4) * 17f + sin(snapshot.elapsedSeconds * 0.4f + index) * 6f).coerceIn(0f, width.toFloat())
            val y = (drift - 60f).coerceIn(-40f, height.toFloat() + 20f)
            val radius = 1.2f + (index % 4) * 0.45f
            fillPaint.color = Color.argb(64 + (index % 3) * 32, 218, 244, 255)
            canvas.drawCircle(x, y, radius, fillPaint)
        }

        if (quality.scanlineAlpha > 0) {
            fillPaint.color = Color.argb(quality.scanlineAlpha, 255, 255, 255)
            var scanY = 0f
            while (scanY < height) {
                canvas.drawRect(0f, scanY, width.toFloat(), scanY + 1.2f, fillPaint)
                scanY += 16f
            }
        }
    }

    private fun drawArenaPlane(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        palette: RenderPalette,
        quality: RenderQuality,
    ) {
        rect.set(
            offsetX - 34f,
            offsetY - 34f,
            offsetX + snapshot.bounds.width * scale + 34f,
            offsetY + snapshot.bounds.height * scale + 34f,
        )
        fillPaint.color = Color.argb(145, 8, 13, 22)
        canvas.drawRoundRect(rect, 34f, 34f, fillPaint)

        gridPaint.color = palette.grid
        val majorStep = 120f
        var x = 0f
        while (x <= snapshot.bounds.width) {
            val px = offsetX + x * scale
            canvas.drawLine(px, offsetY, px, offsetY + snapshot.bounds.height * scale, gridPaint)
            x += majorStep
        }
        var y = 0f
        while (y <= snapshot.bounds.height) {
            val py = offsetY + y * scale
            canvas.drawLine(offsetX, py, offsetX + snapshot.bounds.width * scale, py, gridPaint)
            y += majorStep
        }

        strokePaint.color = palette.accent.withAlpha(quality.borderAlpha)
        strokePaint.strokeWidth = 3f
        canvas.drawRoundRect(rect, 34f, 34f, strokePaint)
        strokePaint.strokeWidth = 2.5f
    }

    private fun drawPickups(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
    ) {
        snapshot.pickups.forEach { pickup ->
            val p = project(pickup.position, scale, offsetX, offsetY)
            val radius = pickup.radius * scale * 2.2f
            val glow = if (pickup.kind == "xp") assets.pickupXp else assets.pickupCredit
            drawBitmapCentered(canvas, glow, p.x, p.y, radius * 2.4f, alpha = 0.9f)
            fillPaint.color = pickup.color.toAndroidColor()
            path.reset()
            path.moveTo(p.x, p.y - pickup.radius * scale * 1.6f)
            path.lineTo(p.x + pickup.radius * scale * 1.16f, p.y)
            path.lineTo(p.x, p.y + pickup.radius * scale * 1.6f)
            path.lineTo(p.x - pickup.radius * scale * 1.16f, p.y)
            path.close()
            canvas.drawPath(path, fillPaint)
        }
    }

    private fun drawProjectiles(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
    ) {
        snapshot.projectiles.forEach { projectile ->
            val p = project(projectile.position, scale, offsetX, offsetY)
            val radius = projectile.radius * scale
            val color = projectile.color.toAndroidColor()
            glowPaint.color = color
            glowPaint.alpha = if (projectile.friendly) 210 else 185
            canvas.drawCircle(p.x, p.y, radius * 2.05f, glowPaint)
            fillPaint.color = color
            canvas.drawCircle(p.x, p.y, radius, fillPaint)
        }
    }

    private fun drawEnemies(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
    ) {
        snapshot.enemies.forEach { enemy ->
            val p = project(enemy.position, scale, offsetX, offsetY)
            val radius = enemy.radius * scale
            val color = enemy.color.toAndroidColor()

            glowPaint.color = color
            glowPaint.alpha = if (enemy.isElite) 150 else 88
            canvas.drawCircle(p.x, p.y, radius * 1.7f, glowPaint)
            drawBitmapCentered(canvas, assets.enemySprite(enemy.kindId), p.x, p.y, radius * 2.15f)

            if (enemy.isElite) {
                drawBitmapCentered(canvas, assets.eliteSigil, p.x, p.y, radius * 2.8f, alpha = 0.86f)
            }
            if (enemy.kindId == "relay" || enemy.kindId == "lich" || enemy.kindId == "hydra") {
                drawBitmapCentered(canvas, assets.bossSigil, p.x, p.y, radius * 3.2f, alpha = 0.92f)
            }

            if (enemy.telegraphAlpha > 0f) {
                strokePaint.color = Color.argb((enemy.telegraphAlpha * 185).toInt(), 255, 236, 160)
                strokePaint.strokeWidth = 5f
                canvas.drawCircle(p.x, p.y, radius + 22f, strokePaint)
                strokePaint.strokeWidth = 2.5f
            }

            rect.set(p.x - radius, p.y + radius + 11f, p.x + radius, p.y + radius + 18f)
            fillPaint.color = Color.argb(128, 7, 12, 18)
            canvas.drawRoundRect(rect, 9f, 9f, fillPaint)
            rect.right = rect.left + rect.width() * enemy.hpRatio.coerceIn(0f, 1f)
            fillPaint.color = color
            canvas.drawRoundRect(rect, 9f, 9f, fillPaint)
        }
    }

    private fun drawPlayer(
        canvas: Canvas,
        player: PlayerSnapshot,
        snapshot: FrameSnapshot,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        palette: RenderPalette,
    ) {
        val p = project(player.position, scale, offsetX, offsetY)
        val radius = player.radius * scale
        val thrusterAlpha = (0.4f + snapshot.visualFx.overdriveAlpha * 0.6f).coerceIn(0f, 1f)
        if (snapshot.visualFx.dashAlpha > 0f || thrusterAlpha > 0f) {
            drawBitmapCentered(
                canvas = canvas,
                bitmap = assets.thrusterTrail,
                centerX = p.x,
                centerY = p.y + radius * 0.5f,
                size = radius * 2.8f,
                alpha = max(snapshot.visualFx.dashAlpha, thrusterAlpha),
            )
        }

        glowPaint.color = player.activeShipColor.toAndroidColor()
        glowPaint.alpha = (150 + snapshot.visualFx.overdriveAlpha * 65f).toInt().coerceIn(0, 255)
        canvas.drawCircle(p.x, p.y, radius * 2.15f, glowPaint)
        drawBitmapCentered(canvas, assets.shipSprite(player.shipId), p.x, p.y, radius * 2.3f)

        if (snapshot.visualFx.shieldAlpha > 0f || player.shield > 0f) {
            drawBitmapCentered(
                canvas = canvas,
                bitmap = assets.effectSprite(VisualEffectKind.Shield),
                centerX = p.x,
                centerY = p.y,
                size = radius * 4.6f,
                alpha = max(snapshot.visualFx.shieldAlpha, 0.32f),
            )
        }

        strokePaint.color = palette.accent
        strokePaint.alpha = (110 + snapshot.visualFx.overdriveAlpha * 70f).toInt().coerceIn(0, 255)
        canvas.drawCircle(p.x, p.y, radius * 2.5f, strokePaint)
        strokePaint.alpha = 255
    }

    private fun drawParticles(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        quality: RenderQuality,
    ) {
        snapshot.particles.forEach { particle ->
            val p = project(particle.position, scale, offsetX, offsetY)
            fillPaint.color = particle.color.toAndroidColor()
            fillPaint.alpha = (particle.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, particle.radius * scale * quality.particleScale, fillPaint)
            fillPaint.alpha = 255
        }
    }

    private fun drawVisualEffects(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        behindEntities: Boolean,
    ) {
        snapshot.visualFx.effects.forEach { effect ->
            val isBehind = effect.kind == VisualEffectKind.Pulse ||
                effect.kind == VisualEffectKind.Shield ||
                effect.kind == VisualEffectKind.Mine ||
                effect.kind == VisualEffectKind.Death
            if (isBehind != behindEntities) return@forEach
            val p = project(effect.position, scale, offsetX, offsetY)
            val baseSize = effect.radius * scale * 2f
            val sprite = assets.effectSprite(effect.kind)
            drawBitmapCentered(
                canvas = canvas,
                bitmap = sprite,
                centerX = p.x,
                centerY = p.y,
                size = baseSize,
                alpha = effect.alpha.coerceIn(0f, 1f),
                rotationDegrees = effect.rotationDegrees,
                tint = effect.color.toAndroidColor(),
                additive = true,
            )
        }
    }

    private fun drawDamageFlash(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        width: Int,
        height: Int,
        palette: RenderPalette,
    ) {
        val alpha = (snapshot.visualFx.damageFlash * 78f).toInt().coerceIn(0, 78)
        if (alpha <= 0) return
        fillPaint.color = palette.warning.withAlpha(alpha)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)
    }

    private fun drawVignette(canvas: Canvas, width: Int, height: Int) {
        vignettePaint.shader = RadialGradient(
            width * 0.5f,
            height * 0.48f,
            max(width, height).toFloat() * 0.82f,
            intArrayOf(Color.TRANSPARENT, Color.argb(92, 4, 6, 10), Color.argb(186, 3, 4, 8)),
            floatArrayOf(0.45f, 0.82f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        vignettePaint.shader = null
    }

    private fun drawAmbientGlow(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int, alphaFactor: Float) {
        glowPaint.shader = RadialGradient(
            x,
            y,
            radius,
            intArrayOf(color.withAlpha((255 * alphaFactor).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(x, y, radius, glowPaint)
        glowPaint.shader = null
    }

    private fun drawBitmapCentered(
        canvas: Canvas,
        bitmap: Bitmap,
        centerX: Float,
        centerY: Float,
        size: Float,
        alpha: Float = 1f,
        rotationDegrees: Float = 0f,
        tint: Int? = null,
        additive: Boolean = false,
    ) {
        val drawPaint = if (additive) additivePaint else bitmapPaint
        drawPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        drawPaint.colorFilter = tint?.let { BlendModeColorFilter(it, BlendMode.SRC_ATOP) }
        matrix.reset()
        val scale = size / max(bitmap.width, bitmap.height).toFloat()
        matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
        matrix.postScale(scale, scale)
        matrix.postRotate(rotationDegrees)
        matrix.postTranslate(centerX, centerY)
        canvas.drawBitmap(bitmap, matrix, drawPaint)
        drawPaint.alpha = 255
        drawPaint.colorFilter = null
    }

    private fun project(world: Vector2, scale: Float, offsetX: Float, offsetY: Float): PointF =
        PointF(offsetX + world.x * scale, offsetY + world.y * scale)

    private fun paletteFor(label: String): RenderPalette = when {
        label.contains("Obsidian", ignoreCase = true) -> RenderPalette(
            top = Color.parseColor("#060816"),
            bottom = Color.parseColor("#131935"),
            grid = Color.argb(46, 140, 156, 255),
            glow = Color.parseColor("#3849B4"),
            accent = Color.parseColor("#B7BEFF"),
            warning = Color.parseColor("#B64B78"),
        )
        label.contains("Amber", ignoreCase = true) -> RenderPalette(
            top = Color.parseColor("#110907"),
            bottom = Color.parseColor("#24160F"),
            grid = Color.argb(46, 255, 189, 94),
            glow = Color.parseColor("#72310E"),
            accent = Color.parseColor("#FFB85A"),
            warning = Color.parseColor("#C65543"),
        )
        else -> RenderPalette(
            top = Color.parseColor("#050911"),
            bottom = Color.parseColor("#0A1623"),
            grid = Color.argb(44, 86, 198, 255),
            glow = Color.parseColor("#143E59"),
            accent = Color.parseColor("#4CE5FF"),
            warning = Color.parseColor("#B3486F"),
        )
    }

    private fun qualityFor(settings: GameSettings, width: Int, height: Int): RenderQuality {
        if (settings.lowVfx) {
            return RenderQuality(
                starCount = 24,
                ambientGlowAlpha = 0.12f,
                borderAlpha = 48,
                particleScale = 0.72f,
                scanlineAlpha = 0,
            )
        }
        return when (settings.graphicsProfile) {
            GraphicsProfile.Clean -> RenderQuality(
                starCount = 30,
                ambientGlowAlpha = 0.16f,
                borderAlpha = 64,
                particleScale = 0.8f,
                scanlineAlpha = 10,
            )
            GraphicsProfile.Dense -> RenderQuality(
                starCount = 52,
                ambientGlowAlpha = 0.24f,
                borderAlpha = 86,
                particleScale = 0.92f,
                scanlineAlpha = 18,
            )
            GraphicsProfile.Auto -> {
                val dense = width * height <= 2_000_000
                if (dense) {
                    RenderQuality(44, 0.22f, 80, 0.88f, 14)
                } else {
                    RenderQuality(34, 0.18f, 70, 0.84f, 12)
                }
            }
        }
    }

    private fun Long.toAndroidColor(): Int = Color.argb(
        ((this shr 24) and 0xFF).toInt(),
        ((this shr 16) and 0xFF).toInt(),
        ((this shr 8) and 0xFF).toInt(),
        (this and 0xFF).toInt(),
    )

    private fun Int.withAlpha(alpha: Int): Int = Color.argb(
        alpha.coerceIn(0, 255),
        Color.red(this),
        Color.green(this),
        Color.blue(this),
    )
}
