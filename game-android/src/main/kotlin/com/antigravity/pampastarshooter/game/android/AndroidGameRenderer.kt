package com.antigravity.pampastarshooter.game.android

import android.graphics.*
import com.antigravity.pampastarshooter.core.model.FrameSnapshot
import com.antigravity.pampastarshooter.core.model.GameSettings
import com.antigravity.pampastarshooter.core.model.PlayerSnapshot
import com.antigravity.pampastarshooter.core.model.Vector2
import kotlin.math.max
import kotlin.math.min

private data class RenderPalette(
    val top: Int,
    val bottom: Int,
    val grid: Int,
    val glow: Int,
    val accent: Int,
)

class AndroidGameRenderer {
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
    private val path = Path()
    private val rect = RectF()

    fun render(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        settings: GameSettings,
        surfaceWidth: Int,
        surfaceHeight: Int,
    ) {
        val palette = paletteFor(snapshot.hud.biomeLabel)
        drawBackdrop(canvas, surfaceWidth, surfaceHeight, palette, settings)

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

        drawArenaPlane(canvas, snapshot, scale, offsetX, offsetY, palette, settings)
        drawPickups(canvas, snapshot, scale, offsetX, offsetY)
        drawProjectiles(canvas, snapshot, scale, offsetX, offsetY)
        drawEnemies(canvas, snapshot, scale, offsetX, offsetY)
        snapshot.player?.let { drawPlayer(canvas, it, scale, offsetX, offsetY, palette) }
        drawParticles(canvas, snapshot, scale, offsetX, offsetY)
        drawVignette(canvas, surfaceWidth, surfaceHeight)
    }

    private fun drawBackdrop(
        canvas: Canvas,
        width: Int,
        height: Int,
        palette: RenderPalette,
        settings: GameSettings,
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

        drawAmbientGlow(canvas, width * 0.18f, height * 0.14f, width * 0.52f, palette.glow, 0.22f)
        drawAmbientGlow(canvas, width * 0.82f, height * 0.72f, width * 0.46f, palette.accent, 0.16f)
        drawAmbientGlow(canvas, width * 0.55f, height * 0.38f, width * 0.34f, Color.WHITE, 0.05f)

        repeat(36) { index ->
            val x = ((index * 73) % width).toFloat() + (index % 3) * 11f
            val y = ((index * 149) % height).toFloat()
            val radius = 1.4f + (index % 4) * 0.4f
            fillPaint.color = Color.argb(70 + (index % 3) * 30, 220, 244, 255)
            canvas.drawCircle(x, y, radius, fillPaint)
        }

        if (!settings.lowVfx) {
            fillPaint.color = Color.argb(18, 255, 255, 255)
            var scanY = 0f
            while (scanY < height) {
                canvas.drawRect(0f, scanY, width.toFloat(), scanY + 1.4f, fillPaint)
                scanY += 18f
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
        settings: GameSettings,
    ) {
        rect.set(
            offsetX - 30f,
            offsetY - 30f,
            offsetX + snapshot.bounds.width * scale + 30f,
            offsetY + snapshot.bounds.height * scale + 30f,
        )
        fillPaint.color = Color.argb(140, 8, 14, 22)
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

        if (!settings.lowVfx) {
            strokePaint.color = Color.argb(55, Color.red(palette.accent), Color.green(palette.accent), Color.blue(palette.accent))
            strokePaint.strokeWidth = 3f
            canvas.drawRoundRect(rect, 34f, 34f, strokePaint)
            strokePaint.strokeWidth = 2.5f
        }
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
            fillPaint.color = pickup.color.toAndroidColor()
            path.reset()
            path.moveTo(p.x, p.y - pickup.radius * scale * 1.6f)
            path.lineTo(p.x + pickup.radius * scale * 1.2f, p.y)
            path.lineTo(p.x, p.y + pickup.radius * scale * 1.6f)
            path.lineTo(p.x - pickup.radius * scale * 1.2f, p.y)
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
            glowPaint.alpha = if (projectile.friendly) 220 else 190
            canvas.drawCircle(p.x, p.y, radius * 1.8f, glowPaint)
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
            glowPaint.alpha = if (enemy.isElite) 130 else 85
            canvas.drawCircle(p.x, p.y, radius * 1.6f, glowPaint)

            fillPaint.color = color
            canvas.drawCircle(p.x, p.y, radius, fillPaint)
            fillPaint.color = Color.argb(110, 7, 12, 18)
            canvas.drawCircle(p.x, p.y, radius * 0.52f, fillPaint)

            strokePaint.color = Color.argb(180, 255, 255, 255)
            strokePaint.alpha = if (enemy.isElite) 255 else 95
            canvas.drawCircle(p.x, p.y, radius + if (enemy.isElite) 5f else 2f, strokePaint)
            strokePaint.alpha = 255

            if (enemy.telegraphAlpha > 0f) {
                strokePaint.color = Color.argb((enemy.telegraphAlpha * 180).toInt(), 255, 236, 160)
                canvas.drawCircle(p.x, p.y, radius + 18f, strokePaint)
            }

            rect.set(p.x - radius, p.y + radius + 10f, p.x + radius, p.y + radius + 16f)
            fillPaint.color = Color.argb(120, 7, 12, 18)
            canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
            rect.right = rect.left + rect.width() * enemy.hpRatio.coerceIn(0f, 1f)
            fillPaint.color = color
            canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
        }
    }

    private fun drawPlayer(
        canvas: Canvas,
        player: PlayerSnapshot,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        palette: RenderPalette,
    ) {
        val p = project(player.position, scale, offsetX, offsetY)
        val radius = player.radius * scale

        glowPaint.color = player.activeShipColor.toAndroidColor()
        glowPaint.alpha = 150
        canvas.drawCircle(p.x, p.y, radius * 2f, glowPaint)

        fillPaint.color = player.activeShipColor.toAndroidColor()
        path.reset()
        path.moveTo(p.x, p.y - radius * 1.35f)
        path.lineTo(p.x + radius * 0.88f, p.y + radius * 0.98f)
        path.lineTo(p.x, p.y + radius * 0.35f)
        path.lineTo(p.x - radius * 0.88f, p.y + radius * 0.98f)
        path.close()
        canvas.drawPath(path, fillPaint)

        fillPaint.color = Color.WHITE
        canvas.drawCircle(p.x, p.y, radius * 0.28f, fillPaint)
        fillPaint.color = Color.argb(180, 255, 184, 90)
        canvas.drawRect(p.x - radius * 0.16f, p.y + radius * 0.88f, p.x + radius * 0.16f, p.y + radius * 1.45f, fillPaint)

        if (player.shield > 0f) {
            strokePaint.color = Color.argb(210, 126, 255, 212)
            strokePaint.strokeWidth = 4f
            canvas.drawCircle(p.x, p.y, radius * 1.55f, strokePaint)
            strokePaint.strokeWidth = 2.5f
        }

        strokePaint.color = palette.accent
        strokePaint.alpha = 110
        canvas.drawCircle(p.x, p.y, radius * 2.4f, strokePaint)
        strokePaint.alpha = 255
    }

    private fun drawParticles(
        canvas: Canvas,
        snapshot: FrameSnapshot,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
    ) {
        snapshot.particles.forEach { particle ->
            val p = project(particle.position, scale, offsetX, offsetY)
            fillPaint.color = particle.color.toAndroidColor()
            fillPaint.alpha = (particle.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, particle.radius * scale * 0.85f, fillPaint)
            fillPaint.alpha = 255
        }
    }

    private fun drawVignette(canvas: Canvas, width: Int, height: Int) {
        vignettePaint.shader = RadialGradient(
            width * 0.5f,
            height * 0.48f,
            max(width, height).toFloat() * 0.82f,
            intArrayOf(Color.TRANSPARENT, Color.argb(90, 4, 6, 10), Color.argb(180, 3, 4, 8)),
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

    private fun project(world: Vector2, scale: Float, offsetX: Float, offsetY: Float): PointF =
        PointF(offsetX + world.x * scale, offsetY + world.y * scale)

    private fun paletteFor(label: String): RenderPalette = when {
        label.contains("Obsidian", ignoreCase = true) -> RenderPalette(
            top = Color.parseColor("#070914"),
            bottom = Color.parseColor("#131935"),
            grid = Color.argb(44, 126, 150, 255),
            glow = Color.parseColor("#3B4FB9"),
            accent = Color.parseColor("#AAB6FF"),
        )
        label.contains("Amber", ignoreCase = true) -> RenderPalette(
            top = Color.parseColor("#110906"),
            bottom = Color.parseColor("#24140F"),
            grid = Color.argb(44, 255, 182, 90),
            glow = Color.parseColor("#6A2B0E"),
            accent = Color.parseColor("#FFB85A"),
        )
        else -> RenderPalette(
            top = Color.parseColor("#050911"),
            bottom = Color.parseColor("#0A1623"),
            grid = Color.argb(42, 78, 194, 255),
            glow = Color.parseColor("#143E59"),
            accent = Color.parseColor("#4CE5FF"),
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
}
