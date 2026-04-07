package com.antigravity.pampastarshooter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.antigravity.pampastarshooter.core.model.CampaignState
import com.antigravity.pampastarshooter.core.model.MissionState
import com.antigravity.pampastarshooter.core.model.RunHistoryEntry
import com.antigravity.pampastarshooter.core.model.UnlockTree
import com.antigravity.pampastarshooter.data.local.dao.ProfileDao
import com.antigravity.pampastarshooter.data.local.dao.RunHistoryDao
import com.antigravity.pampastarshooter.data.local.entity.ProfileEntity
import com.antigravity.pampastarshooter.data.local.entity.RunHistoryEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Database(
    entities = [ProfileEntity::class, RunHistoryEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(AppDatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun runHistoryDao(): RunHistoryDao
}

class AppDatabaseConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun unlockTreeToString(value: UnlockTree): String = json.encodeToString(UnlockTree.serializer(), value)

    @TypeConverter
    fun stringToUnlockTree(value: String): UnlockTree = json.decodeFromString(UnlockTree.serializer(), value)

    @TypeConverter
    fun campaignStateToString(value: CampaignState): String = json.encodeToString(CampaignState.serializer(), value)

    @TypeConverter
    fun stringToCampaignState(value: String): CampaignState = json.decodeFromString(CampaignState.serializer(), value)

    @TypeConverter
    fun stringSetToString(value: Set<String>): String = json.encodeToString(ListSerializer(String.serializer()), value.toList())

    @TypeConverter
    fun stringToStringSet(value: String): Set<String> = json.decodeFromString(ListSerializer(String.serializer()), value).toSet()

    @TypeConverter
    fun missionStatesToString(value: List<MissionState>): String = json.encodeToString(ListSerializer(MissionState.serializer()), value)

    @TypeConverter
    fun stringToMissionStates(value: String): List<MissionState> = json.decodeFromString(ListSerializer(MissionState.serializer()), value)

    @TypeConverter
    fun stringListToString(value: List<String>): String = json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun stringToStringList(value: String): List<String> = json.decodeFromString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun runHistoryEntryToString(value: RunHistoryEntry): String = json.encodeToString(RunHistoryEntry.serializer(), value)

    @TypeConverter
    fun stringToRunHistoryEntry(value: String): RunHistoryEntry = json.decodeFromString(RunHistoryEntry.serializer(), value)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE profile
            ADD COLUMN campaignState TEXT NOT NULL DEFAULT '{"completedNodeIds":[],"endlessUnlocked":false}'
            """.trimIndent(),
        )
        db.execSQL(
            """
            ALTER TABLE profile
            ADD COLUMN unlockedPerkIds TEXT NOT NULL DEFAULT '[]'
            """.trimIndent(),
        )
        db.execSQL(
            """
            ALTER TABLE profile
            ADD COLUMN equippedPerkIds TEXT NOT NULL DEFAULT '[]'
            """.trimIndent(),
        )
    }
}
