package com.antigravity.pampastarshooter.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.antigravity.pampastarshooter.core.model.GameSettings

@Immutable
data class PampaChrome(
    val highContrast: Boolean,
    val panelTopArgb: Long,
    val panelBottomArgb: Long,
    val inkArgb: Long,
    val cyanArgb: Long,
    val amberArgb: Long,
    val lavenderArgb: Long,
    val mintArgb: Long,
    val textArgb: Long,
    val mutedArgb: Long,
    val edgeAlpha: Float,
)

internal val LocalPampaChrome = compositionLocalOf { defaultChrome(false) }

@Composable
internal fun ProvidePampaChrome(
    settings: GameSettings,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val chrome = remember(settings.highContrastHud) { defaultChrome(settings.highContrastHud) }
    val scaledDensity = remember(density, settings.uiTextScale) {
        Density(density = density.density, fontScale = density.fontScale * settings.uiTextScale)
    }
    CompositionLocalProvider(
        LocalPampaChrome provides chrome,
        LocalDensity provides scaledDensity,
        content = content,
    )
}

internal fun defaultChrome(highContrast: Boolean): PampaChrome = if (highContrast) {
    PampaChrome(
        highContrast = true,
        panelTopArgb = 0xF61A2432,
        panelBottomArgb = 0xF20A1119,
        inkArgb = 0xFF03060C,
        cyanArgb = 0xFF8BF3FF,
        amberArgb = 0xFFFFCF75,
        lavenderArgb = 0xFFC3CBFF,
        mintArgb = 0xFFA6FFD7,
        textArgb = 0xFFFFFFFF,
        mutedArgb = 0xFFD2E1EC,
        edgeAlpha = 0.55f,
    )
} else {
    PampaChrome(
        highContrast = false,
        panelTopArgb = 0xF3141D29,
        panelBottomArgb = 0xED091018,
        inkArgb = 0xFF050911,
        cyanArgb = 0xFF63E7FF,
        amberArgb = 0xFFFFBF66,
        lavenderArgb = 0xFFAAB5FF,
        mintArgb = 0xFF88FFC8,
        textArgb = 0xFFF3FAFF,
        mutedArgb = 0xFFA7BBCC,
        edgeAlpha = 0.28f,
    )
}

@Composable
internal fun chrome(): PampaChrome = LocalPampaChrome.current

internal fun Long.asColor() = androidx.compose.ui.graphics.Color(this)
