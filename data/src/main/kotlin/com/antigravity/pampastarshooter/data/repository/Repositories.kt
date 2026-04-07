package com.antigravity.pampastarshooter.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.antigravity.pampastarshooter.core.content.ContentRepository
import com.antigravity.pampastarshooter.core.content.DefaultGameContent
import com.antigravity.pampastarshooter.core.content.upgradeCost
import com.antigravity.pampastarshooter.core.model.GameSettings
import com.antigravity.pampastarshooter.core.model.GraphicsProfile
import com.antigravity.pampastarshooter.core.model.HudLayout
import com.antigravity.pampastarshooter.core.model.PlayerProfile
import com.antigravity.pampastarshooter.core.model.RunHistoryEntry
import com.antigravity.pampastarshooter.core.model.RunResult
import com.antigravity.pampastarshooter.core.model.applyRunResult
import com.antigravity.pampastarshooter.core.model.isPermanentModuleAvailable
import com.antigravity.pampastarshooter.core.model.normalizeForContent
import com.antigravity.pampastarshooter.data.local.AppDatabase
import com.antigravity.pampastarshooter.data.local.MIGRATION_1_2
import com.antigravity.pampastarshooter.data.local.entity.toDomain
import com.antigravity.pampastarshooter.data.local.entity.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface ProfileRepository {
    val profile: Flow<PlayerProfile>
    suspend fun refresh()
    suspend fun setSelectedShip(shipId: String)
    suspend fun markTutorialSeen()
    suspend fun upgradeModule(moduleId: String): Boolean
    suspend fun setEquippedPerks(ids: List<String>)
    suspend fun applyRunResult(result: RunResult)
}

interface SettingsRepository {
    val settings: Flow<GameSettings>
    suspend fun setGraphicsProfile(profile: GraphicsProfile)
    suspend fun setLowVfx(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setMusicVolume(volume: Float)
    suspend fun setSfxVolume(volume: Float)
    suspend fun setUiTextScale(scale: Float)
    suspend fun setHighContrastHud(enabled: Boolean)
    suspend fun setScreenShakeEnabled(enabled: Boolean)
    suspend fun setHudLayout(layout: HudLayout)
}

interface RunHistoryRepository {
    val history: Flow<List<RunHistoryEntry>>
    suspend fun record(entry: RunHistoryEntry)
}

class StaticContentRepository : ContentRepository {
    private val bundle = DefaultGameContent.create()
    override fun load() = bundle
}

class RoomProfileRepository(
    private val db: AppDatabase,
    private val contentRepository: ContentRepository,
) : ProfileRepository {
    override val profile: Flow<PlayerProfile> = db.profileDao().observe().map { entity ->
        (entity?.toDomain() ?: DefaultGameContent.starterProfile()).normalizeForContent(contentRepository.load())
    }

    override suspend fun refresh() {
        val existing = db.profileDao().get()
        if (existing == null) {
            db.profileDao().upsert(DefaultGameContent.starterProfile().toEntity())
            return
        }
        val normalized = existing.toDomain().normalizeForContent(contentRepository.load())
        if (normalized != existing.toDomain()) {
            db.profileDao().upsert(normalized.toEntity())
        }
    }

    override suspend fun setSelectedShip(shipId: String) {
        val current = profile.first().normalizeForContent(contentRepository.load())
        db.profileDao().upsert(current.copy(selectedShipId = shipId).toEntity())
    }

    override suspend fun markTutorialSeen() {
        val current = profile.first().normalizeForContent(contentRepository.load())
        db.profileDao().upsert(current.copy(tutorialSeen = true).toEntity())
    }

    override suspend fun upgradeModule(moduleId: String): Boolean {
        val current = profile.first().normalizeForContent(contentRepository.load())
        val module = contentRepository.load().permanentModules.firstOrNull { it.id == moduleId } ?: return false
        if (!current.isPermanentModuleAvailable(module)) return false
        val level = current.unlockTree.permanentModules[moduleId] ?: return false
        val cost = module.upgradeCost(level)
        if (current.credits < cost || level >= module.maxLevel) return false
        val updatedModules = current.unlockTree.permanentModules + (moduleId to (level + 1))
        db.profileDao().upsert(
            current.copy(
                credits = current.credits - cost,
                unlockTree = current.unlockTree.copy(permanentModules = updatedModules),
            ).toEntity(),
        )
        return true
    }

    override suspend fun setEquippedPerks(ids: List<String>) {
        val current = profile.first().normalizeForContent(contentRepository.load())
        val allowed = ids
            .filter { it in current.unlockedPerkIds }
            .distinct()
            .take(2)
        db.profileDao().upsert(current.copy(equippedPerkIds = allowed).toEntity())
    }

    override suspend fun applyRunResult(result: RunResult) {
        val current = profile.first().normalizeForContent(contentRepository.load())
        val updated = current.applyRunResult(result, contentRepository.load())
        db.profileDao().upsert(updated.toEntity())
    }
}

class RoomRunHistoryRepository(
    private val db: AppDatabase,
) : RunHistoryRepository {
    override val history: Flow<List<RunHistoryEntry>> = db.runHistoryDao().observeRecent().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun record(entry: RunHistoryEntry) {
        db.runHistoryDao().upsert(entry.toEntity())
    }
}

class DataStoreSettingsRepository(
    context: Context,
) : SettingsRepository {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("game_settings.preferences_pb") },
    )

    override val settings: Flow<GameSettings> = dataStore.data.map { prefs ->
        GameSettings(
            graphicsProfile = prefs[KEY_GRAPHICS_PROFILE]?.let(GraphicsProfile::valueOf) ?: GameSettings().graphicsProfile,
            lowVfx = prefs[KEY_LOW_VFX] ?: GameSettings().lowVfx,
            hapticsEnabled = prefs[KEY_HAPTICS] ?: GameSettings().hapticsEnabled,
            musicVolume = prefs[KEY_MUSIC] ?: GameSettings().musicVolume,
            sfxVolume = prefs[KEY_SFX] ?: GameSettings().sfxVolume,
            uiTextScale = prefs[KEY_TEXT_SCALE] ?: GameSettings().uiTextScale,
            highContrastHud = prefs[KEY_HIGH_CONTRAST] ?: GameSettings().highContrastHud,
            screenShakeEnabled = prefs[KEY_SHAKE] ?: GameSettings().screenShakeEnabled,
            hudLayout = HudLayout(
                flipped = prefs[KEY_HUD_FLIPPED] ?: GameSettings().hudLayout.flipped,
                controlScale = prefs[KEY_HUD_SCALE] ?: GameSettings().hudLayout.controlScale,
                actionColumnYOffset = prefs[KEY_HUD_OFFSET] ?: GameSettings().hudLayout.actionColumnYOffset,
            ),
        )
    }

    override suspend fun setGraphicsProfile(profile: GraphicsProfile) = edit { it[KEY_GRAPHICS_PROFILE] = profile.name }
    override suspend fun setLowVfx(enabled: Boolean) = edit { it[KEY_LOW_VFX] = enabled }
    override suspend fun setHapticsEnabled(enabled: Boolean) = edit { it[KEY_HAPTICS] = enabled }
    override suspend fun setMusicVolume(volume: Float) = edit { it[KEY_MUSIC] = volume.coerceIn(0f, 1f) }
    override suspend fun setSfxVolume(volume: Float) = edit { it[KEY_SFX] = volume.coerceIn(0f, 1f) }
    override suspend fun setUiTextScale(scale: Float) = edit { it[KEY_TEXT_SCALE] = scale.coerceIn(0.85f, 1.35f) }
    override suspend fun setHighContrastHud(enabled: Boolean) = edit { it[KEY_HIGH_CONTRAST] = enabled }
    override suspend fun setScreenShakeEnabled(enabled: Boolean) = edit { it[KEY_SHAKE] = enabled }

    override suspend fun setHudLayout(layout: HudLayout) = edit {
        it[KEY_HUD_FLIPPED] = layout.flipped
        it[KEY_HUD_SCALE] = layout.controlScale.coerceIn(0.8f, 1.4f)
        it[KEY_HUD_OFFSET] = layout.actionColumnYOffset.coerceIn(-1f, 1f)
    }

    private suspend fun edit(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { prefs -> block(prefs) }
    }

    companion object {
        private val KEY_GRAPHICS_PROFILE = stringPreferencesKey("graphics_profile")
        private val KEY_LOW_VFX = booleanPreferencesKey("low_vfx")
        private val KEY_HAPTICS = booleanPreferencesKey("haptics")
        private val KEY_MUSIC = floatPreferencesKey("music_volume")
        private val KEY_SFX = floatPreferencesKey("sfx_volume")
        private val KEY_TEXT_SCALE = floatPreferencesKey("ui_text_scale")
        private val KEY_HIGH_CONTRAST = booleanPreferencesKey("high_contrast_hud")
        private val KEY_SHAKE = booleanPreferencesKey("screen_shake")
        private val KEY_HUD_FLIPPED = booleanPreferencesKey("hud_flipped")
        private val KEY_HUD_SCALE = floatPreferencesKey("hud_scale")
        private val KEY_HUD_OFFSET = floatPreferencesKey("hud_offset")
    }
}

object RepositoryFactory {
    fun createDatabase(context: Context): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "pampa_star_shooter.db",
    ).addMigrations(MIGRATION_1_2).build()
}
