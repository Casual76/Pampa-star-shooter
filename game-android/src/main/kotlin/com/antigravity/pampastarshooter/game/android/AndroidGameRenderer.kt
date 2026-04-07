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
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.antigravity.pampastarshooter.core.model.FrameSnapshot
import com.antigravity.pampastarshooter.core.model.GameSettings
import com.antigravity.pampastarshooter.core.model.GraphicsProfile
import com.antigravity.pampastarshooter.core.model.PlayerSnapshot
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
    val particleStride: Int,
    val effectStride: Int,
)

private data class CachedAmbientGlow(
    val x: Float,
    val y: Float,
    val radius: Float,
    val shader: Shader,
)

private data class BackdropCache(
    val width: Int,
    val height: Int,
    val palette: RenderPalette,
    val backgroundShader: Shader,
    val vignetteShader: Shader,
    val ambientGlows: List<CachedAmbientGlow>,
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
    private val tintFilters = mutableMapOf<Int, BlendModeColorFilter>()
    private var backdropCache: BackdropCache? = null

    fun render(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        settings: GameSettings,
        surfaceWidth: Int,
        surfaceHeight: Int,
    ) {
        val palette = paletteFor(snapshot.hud.biomeLabel)
        val quality = qualityFor(settings, surfaceWidth, surfaceHeight, snapshot)
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

        val backdrop = backdropFor(surfaceWidth, surfaceHeight, palette)
        drawBackdrop(canvas, surfaceWidth, surfaceHeight, snapshot, backdrop, quality)
        drawArenaPlane(canvas, snapshot, scale, offsetX, offsetY, palette, quality)
        drawVisualEffects(canvas, snapshot, scale, offsetX, offsetY, quality, behindEntities = true)
        drawPickups(canvas, snapshot, scale, offsetX, offsetY)
        drawProjectiles(canvas, snapshot, scale, offsetX, offsetY)
        drawEnemies(canvas, snapshot, scale, offsetX, offsetY)
        snapshot.player?.let { drawPlayer(canvas, it, snapshot, scale, offsetX, offsetY, palette) }
        drawParticles(canvas, snapshot, scale, offsetX, offsetY, quality)
        drawVisualEffects(canvas, snapshot, scale, offsetX, offsetY, quality, behindEntities = false)
        drawDamageFlash(canvas, snapshot, surfaceWidth, surfaceHeight, palette)
        drawVignette(canvas, surfaceWidth, surfaceHeight, backdrop)
        canvas.restore()
    }

    private fun drawBackdrop(
        canvas: Canvas,
        width: Int,
        height: Int,
        snapshot: FrameSnapshot,
        backdrop: BackdropCache,
        quality: RenderQuality,
    ) {
        backgroundPaint.shader = backdrop.backgroundShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        drawAmbientGlow(canvas, backdrop.ambientGlows[0], quality.ambientGlowAlpha)
        drawAmbientGlow(canvas, backdrop.ambientGlows[1], quality.ambientGlowAlpha * 0.8f)
        drawAmbientGlow(canvas, backdrop.ambientGlows[2], quality.ambientGlowAlpha * 0.25f)

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
            val px = offsetX + pickup.position.x * scale
            val py = offsetY + pickup.position.y * scale
            val radius = pickup.radius * scale * 2.2f
            val glow = if (pickup.kind == "xp") assets.pickupXp else assets.pickupCredit
            drawBitmapCentered(canvas, glow, px, py, radius * 2.4f, alpha = 0.9f)
            fillPaint.color = pickup.color.toAndroidColor()
            path.reset()
            path.moveTo(px, py - pickup.radius * scale * 1.6f)
            path.lineTo(px + pickup.radius * scale * 1.16f, py)
            path.lineTo(px, py + pickup.radius * scale * 1.6f)
            path.lineTo(px - pickup.radius * scale * 1.16f, py)
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
            val px = offsetX + projectile.position.x * scale
            val py = offsetY + projectile.position.y * scale
            val radius = projectile.radius * scale
            val color = projectile.color.toAndroidColor()
            glowPaint.color = color
            glowPaint.alpha = if (projectile.friendly) 210 else 185
            canvas.drawCircle(px, py, radius * 2.05f, glowPaint)
            fillPaint.color = color
            canvas.drawCircle(px, py, radius, fillPaint)
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
            val px = offsetX + enemy.position.x * scale
            val py = offsetY + enemy.position.y * scale
            val radius = enemy.radius * scale
            val color = enemy.color.toAndroidColor()

            glowPaint.color = color
            glowPaint.alpha = if (enemy.isElite) 150 else 88
            canvas.drawCircle(px, py, radius * 1.7f, glowPaint)
            drawBitmapCentered(canvas, assets.enemySprite(enemy.kindId), px, py, radius * 2.15f)

            if (enemy.isElite) {
                drawBitmapCentered(canvas, assets.eliteSigil, px, py, radius * 2.8f, alpha = 0.86f)
            }
            if (enemy.kindId == "relay" || enemy.kindId == "lich" || enemy.kindId == "hydra") {
                drawBitmapCentered(canvas, assets.bossSigil, px, py, radius * 3.2f, alpha = 0.92f)
            }

            if (enemy.telegraphAlpha > 0f) {
                strokePaint.color = Color.argb((enemy.telegraphAlpha * 185).toInt(), 255, 236, 160)
                strokePaint.strokeWidth = 5f
                canvas.drawCircle(px, py, radius + 22f, strokePaint)
                strokePaint.strokeWidth = 2.5f
            }

            rect.set(px - radius, py + radius + 11f, px + radius, py + radius + 18f)
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
        val px = offsetX + player.position.x * scale
        val py = offsetY + player.position.y * scale
        val radius = player.radius * scale
        val thrusterAlpha = (0.4f + snapshot.visualFx.overdriveAlpha * 0.6f).coerceIn(0f, 1f)
        if (snapshot.visualFx.dashAlpha > 0f || thrusterAlpha > 0f) {
            drawBitmapCentered(
                canvas = canvas,
                bitmap = assets.thrusterTrail,
                centerX = px,
                centerY = py + radius * 0.5f,
                size = radius * 2.8f,
                alpha = max(snapshot.visualFx.dashAlpha, thrusterAlpha),
            )
        }

        glowPaint.color = player.activeShipColor.toAndroidColor()
        glowPaint.alpha = (150 + snapshot.visualFx.overdriveAlpha * 65f).toInt().coerceIn(0, 255)
        canvas.drawCircle(px, py, radius * 2.15f, glowPaint)
        drawBitmapCentered(canvas, assets.shipSprite(player.shipId), px, py, radius * 2.3f)

        if (snapshot.visualFx.shieldAlpha > 0f || player.shield > 0f) {
            drawBitmapCentered(
                canvas = canvas,
                bitmap = assets.effectSprite(VisualEffectKind.Shield),
                centerX = px,
                centerY = py,
                size = radius * 4.6f,
                alpha = max(snapshot.visualFx.shieldAlpha, 0.32f),
            )
        }

        strokePaint.color = palette.accent
        strokePaint.alpha = (110 + snapshot.visualFx.overdriveAlpha * 70f).toInt().coerceIn(0, 255)
        canvas.drawCircle(px, py, radius * 2.5f, strokePaint)
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
        var index = 0
        snapshot.particles.forEach { particle ->
            if (index++ % quality.particleStride != 0) return@forEach
            val px = offsetX + particle.position.x * scale
            val py = offsetY + particle.position.y * scale
            fillPaint.color = particle.color.toAndroidColor()
            fillPaint.alpha = (particle.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(px, py, particle.radius * scale * quality.particleScale, fillPaint)
            fillPaint.alpha = 255
        }
    }

    private fun drawVisualEffects(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        quality: RenderQuality,
        behindEntities: Boolean,
    ) {
        var index = 0
        snapshot.visualFx.effects.forEach { effect ->
            val isBehind = effect.kind == VisualEffectKind.Pulse ||
                effect.kind == VisualEffectKind.Shield ||
                effect.kind == VisualEffectKind.Mine ||
                effect.kind == VisualEffectKind.Death
            if (isBehind != behindEntities) return@forEach
            if (index++ % quality.effectStride != 0) return@forEach
            val px = offsetX + effect.position.x * scale
            val py = offsetY + effect.position.y * scale
            val baseSize = effect.radius * scale * 2f
            val sprite = assets.effectSprite(effect.kind)
            drawBitmapCentered(
                canvas = canvas,
                bitmap = sprite,
                centerX = px,
                centerY = py,
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

    private fun drawVignette(canvas: Canvas, width: Int, height: Int, backdrop: BackdropCache) {
        vignettePaint.shader = backdrop.vignetteShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        vignettePaint.shader = null
    }

    private fun drawAmbientGlow(canvas: Canvas, glow: CachedAmbientGlow, alphaFactor: Float) {
        glowPaint.shader = glow.shader
        glowPaint.alpha = (255 * alphaFactor).toInt().coerceIn(0, 255)
        canvas.drawCircle(glow.x, glow.y, glow.radius, glowPaint)
        glowPaint.alpha = 255
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
        drawPaint.colorFilter = tint?.let { tintFilters.getOrPut(it) { BlendModeColorFilter(it, BlendMode.SRC_ATOP) } }
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

    private fun paletteFor(label: String): RenderPalette = when {
        label.contains("Obsidian", ignoreCase = true) -> ObsidianPalette
        label.contains("Amber", ignoreCase = true) -> AmberPalette
        else -> DefaultPalette
    }

    private fun backdropFor(width: Int, height: Int, palette: RenderPalette): BackdropCache {
        val cached = backdropCache
        if (cached != null && cached.width == width && cached.height == height && cached.palette == palette) {
            return cached
        }
        val ambientGlows = listOf(
            CachedAmbientGlow(
                x = width * 0.16f,
                y = height * 0.15f,
                radius = width * 0.52f,
                shader = RadialGradient(
                    width * 0.16f,
                    height * 0.15f,
                    width * 0.52f,
                    intArrayOf(palette.glow, Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                ),
            ),
            CachedAmbientGlow(
                x = width * 0.82f,
                y = height * 0.76f,
                radius = width * 0.44f,
                shader = RadialGradient(
                    width * 0.82f,
                    height * 0.76f,
                    width * 0.44f,
                    intArrayOf(palette.accent, Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                ),
            ),
            CachedAmbientGlow(
                x = width * 0.52f,
                y = height * 0.36f,
                radius = width * 0.32f,
                shader = RadialGradient(
                    width * 0.52f,
                    height * 0.36f,
                    width * 0.32f,
                    intArrayOf(Color.WHITE, Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                ),
            ),
        )
        return BackdropCache(
            width = width,
            height = height,
            palette = palette,
            backgroundShader = LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                palette.top,
                palette.bottom,
                Shader.TileMode.CLAMP,
            ),
            vignetteShader = RadialGradient(
                width * 0.5f,
                height * 0.48f,
                max(width, height).toFloat() * 0.82f,
                intArrayOf(Color.TRANSPARENT, Color.argb(92, 4, 6, 10), Color.argb(186, 3, 4, 8)),
                floatArrayOf(0.45f, 0.82f, 1f),
                Shader.TileMode.CLAMP,
            ),
            ambientGlows = ambientGlows,
        ).also { backdropCache = it }
    }

    private fun qualityFor(settings: GameSettings, width: Int, height: Int, snapshot: FrameSnapshot): RenderQuality {
        val base = when {
            settings.lowVfx -> RenderQuality(
                starCount = 18,
                ambientGlowAlpha = 0.1f,
                borderAlpha = 40,
                particleScale = 0.64f,
                scanlineAlpha = 0,
                particleStride = 2,
                effectStride = 2,
            )
            settings.graphicsProfile == GraphicsProfile.Clean -> RenderQuality(
                starCount = 24,
                ambientGlowAlpha = 0.14f,
                borderAlpha = 56,
                particleScale = 0.74f,
                scanlineAlpha = 6,
                particleStride = 1,
                effectStride = 1,
            )
            settings.graphicsProfile == GraphicsProfile.Dense -> RenderQuality(
                starCount = 44,
                ambientGlowAlpha = 0.22f,
                borderAlpha = 84,
                particleScale = 0.9f,
                scanlineAlpha = 14,
                particleStride = 1,
                effectStride = 1,
            )
            else -> {
                val dense = width * height <= 2_000_000
                if (dense) {
                    RenderQuality(34, 0.18f, 72, 0.82f, 10, 1, 1)
                } else {
                    RenderQuality(28, 0.16f, 64, 0.78f, 8, 1, 1)
                }
            }
        }
        val sceneLoad = snapshot.enemies.size + snapshot.projectiles.size + snapshot.particles.size + snapshot.visualFx.effects.size * 2
        if (sceneLoad < 90) return base
        val severeLoad = sceneLoad >= 180
        return base.copy(
            starCount = (base.starCount * if (severeLoad) 0.55f else 0.72f).toInt().coerceAtLeast(12),
            ambientGlowAlpha = base.ambientGlowAlpha * if (severeLoad) 0.55f else 0.78f,
            borderAlpha = (base.borderAlpha * if (severeLoad) 0.8f else 0.9f).toInt().coerceAtLeast(32),
            particleScale = base.particleScale * if (severeLoad) 0.72f else 0.86f,
            scanlineAlpha = if (severeLoad) 0 else (base.scanlineAlpha * 0.5f).toInt(),
            particleStride = when {
                settings.lowVfx || snapshot.particles.size >= 100 -> 3
                snapshot.particles.size >= 56 -> 2
                else -> base.particleStride
            },
            effectStride = when {
                settings.lowVfx || snapshot.visualFx.effects.size >= 36 -> 2
                else -> base.effectStride
            },
        )
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

    private companion object {
        val DefaultPalette = RenderPalette(
            top = Color.parseColor("#050911"),
            bottom = Color.parseColor("#0A1623"),
            grid = Color.argb(44, 86, 198, 255),
            glow = Color.parseColor("#143E59"),
            accent = Color.parseColor("#4CE5FF"),
            warning = Color.parseColor("#B3486F"),
        )
        val ObsidianPalette = RenderPalette(
            top = Color.parseColor("#060816"),
            bottom = Color.parseColor("#131935"),
            grid = Color.argb(46, 140, 156, 255),
            glow = Color.parseColor("#3849B4"),
            accent = Color.parseColor("#B7BEFF"),
            warning = Color.parseColor("#B64B78"),
        )
        val AmberPalette = RenderPalette(
            top = Color.parseColor("#110907"),
            bottom = Color.parseColor("#24160F"),
            grid = Color.argb(46, 255, 189, 94),
            glow = Color.parseColor("#72310E"),
            accent = Color.parseColor("#FFB85A"),
            warning = Color.parseColor("#C65543"),
        )
    }
}
