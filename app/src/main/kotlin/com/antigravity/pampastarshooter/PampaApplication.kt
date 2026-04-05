package com.antigravity.pampastarshooter

import android.app.Application
import com.antigravity.pampastarshooter.data.repository.DataStoreSettingsRepository
import com.antigravity.pampastarshooter.data.repository.ProfileRepository
import com.antigravity.pampastarshooter.data.repository.RepositoryFactory
import com.antigravity.pampastarshooter.data.repository.RoomProfileRepository
import com.antigravity.pampastarshooter.data.repository.RoomRunHistoryRepository
import com.antigravity.pampastarshooter.data.repository.RunHistoryRepository
import com.antigravity.pampastarshooter.data.repository.SettingsRepository
import com.antigravity.pampastarshooter.data.repository.StaticContentRepository
import com.antigravity.pampastarshooter.game.android.AndroidAudioController
import com.antigravity.pampastarshooter.game.android.AndroidHapticsController
import com.antigravity.pampastarshooter.game.android.AudioController
import com.antigravity.pampastarshooter.game.android.HapticsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PampaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.profileRepository.refresh()
        }
    }
}

class AppContainer(
    application: Application,
) {
    val contentRepository = StaticContentRepository()
    private val database = RepositoryFactory.createDatabase(application)

    val profileRepository: ProfileRepository = RoomProfileRepository(database, contentRepository)
    val historyRepository: RunHistoryRepository = RoomRunHistoryRepository(database)
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(application)
    val audioController: AudioController = AndroidAudioController()
    val hapticsController: HapticsController = AndroidHapticsController(application)
}

