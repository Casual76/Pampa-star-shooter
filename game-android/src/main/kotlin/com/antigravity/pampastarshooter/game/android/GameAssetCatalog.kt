package com.antigravity.pampastarshooter.game.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.antigravity.pampastarshooter.core.content.DefaultGameContent
import com.antigravity.pampastarshooter.core.model.VisualEffectKind

internal class GameAssetCatalog(context: Context) {
    val shipSprites: Map<String, Bitmap> = mapOf(
        DefaultGameContent.ShipStriker to bitmap(context, R.drawable.fx_ship_striker),
        DefaultGameContent.ShipWarden to bitmap(context, R.drawable.fx_ship_warden),
        DefaultGameContent.ShipSpecter to bitmap(context, R.drawable.fx_ship_specter),
    )

    val enemySprites: Map<String, Bitmap> = mapOf(
        "drone" to bitmap(context, R.drawable.fx_enemy_chaser),
        "gunner" to bitmap(context, R.drawable.fx_enemy_shooter),
        "brute" to bitmap(context, R.drawable.fx_enemy_tank),
        "splitter" to bitmap(context, R.drawable.fx_enemy_splitter),
        "wisp" to bitmap(context, R.drawable.fx_enemy_swarm),
        "sniper" to bitmap(context, R.drawable.fx_enemy_shooter),
        "minotaur" to bitmap(context, R.drawable.fx_enemy_tank),
        "relay" to bitmap(context, R.drawable.fx_enemy_shooter),
        "lich" to bitmap(context, R.drawable.fx_enemy_splitter),
        "hydra" to bitmap(context, R.drawable.fx_enemy_tank),
    )

    val effectSprites: Map<VisualEffectKind, Bitmap> = mapOf(
        VisualEffectKind.Dash to bitmap(context, R.drawable.fx_dash_streak),
        VisualEffectKind.Pulse to bitmap(context, R.drawable.fx_pulse_burst),
        VisualEffectKind.Shield to bitmap(context, R.drawable.fx_shield_ring),
        VisualEffectKind.Mine to bitmap(context, R.drawable.fx_mine_burst),
        VisualEffectKind.Hit to bitmap(context, R.drawable.fx_hit_spark),
        VisualEffectKind.Death to bitmap(context, R.drawable.fx_death_bloom),
        VisualEffectKind.Pickup to bitmap(context, R.drawable.fx_pickup_glow_xp),
    )

    val thrusterTrail: Bitmap = bitmap(context, R.drawable.fx_trail_thruster)
    val eliteSigil: Bitmap = bitmap(context, R.drawable.fx_sigil_elite)
    val bossSigil: Bitmap = bitmap(context, R.drawable.fx_sigil_boss)
    val pickupXp: Bitmap = bitmap(context, R.drawable.fx_pickup_glow_xp)
    val pickupCredit: Bitmap = bitmap(context, R.drawable.fx_pickup_glow_credit)

    fun shipSprite(shipId: String): Bitmap = shipSprites[shipId] ?: shipSprites.getValue(DefaultGameContent.ShipStriker)

    fun enemySprite(kindId: String): Bitmap = enemySprites[kindId] ?: enemySprites.getValue("drone")

    fun effectSprite(kind: VisualEffectKind): Bitmap = effectSprites[kind] ?: effectSprites.getValue(VisualEffectKind.Hit)

    companion object {
        private fun bitmap(context: Context, resId: Int): Bitmap =
            BitmapFactory.decodeResource(
                context.resources,
                resId,
                BitmapFactory.Options().apply { inScaled = false },
            )
    }
}
