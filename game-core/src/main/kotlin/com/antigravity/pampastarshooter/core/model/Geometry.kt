package com.antigravity.pampastarshooter.core.model

import kotlin.math.hypot

data class Vector2(
    val x: Float = 0f,
    val y: Float = 0f,
) {
    fun length(): Float = hypot(x.toDouble(), y.toDouble()).toFloat()

    fun normalized(): Vector2 {
        val len = length()
        return if (len <= 0.0001f) Zero else Vector2(x / len, y / len)
    }

    fun clampLength(maxLength: Float): Vector2 {
        val len = length()
        return if (len <= maxLength || len <= 0.0001f) this else Vector2(x / len * maxLength, y / len * maxLength)
    }

    operator fun plus(other: Vector2): Vector2 = Vector2(x + other.x, y + other.y)

    operator fun minus(other: Vector2): Vector2 = Vector2(x - other.x, y - other.y)

    operator fun times(scale: Float): Vector2 = Vector2(x * scale, y * scale)

    fun distanceTo(other: Vector2): Float = (this - other).length()

    companion object {
        val Zero = Vector2()
    }
}

data class RectBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val center: Vector2 get() = Vector2((left + right) * 0.5f, (top + bottom) * 0.5f)

    fun clamp(point: Vector2, padding: Float = 0f): Vector2 = Vector2(
        x = point.x.coerceIn(left + padding, right - padding),
        y = point.y.coerceIn(top + padding, bottom - padding),
    )
}

