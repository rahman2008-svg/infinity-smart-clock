package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// ==========================================
// ENTITIES
// ==========================================

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val daysOfWeek: String = "", // Comma separated, e.g. "Mon,Wed,Fri" or "" for one-time
    val enabled: Boolean = true,
    val ringtoneName: String = "Infinity Pulse",
    val vibration: Boolean = true,
    val graduallyIncreaseVolume: Boolean = true,
    val dismissMission: String = "NONE", // NONE, MATH, QR, SHAKE, VOICE
    val isOneTime: Boolean = false,
    val snoozeCount: Int = 0
)

@Entity(tableName = "world_cities")
data class WorldCity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cityName: String,
    val countryName: String,
    val timeZoneId: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "planner_tasks")
data class PlannerTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val timeString: String, // e.g. "08:00"
    val type: String, // MEDICINE, WATER, MEETING, HABIT, SCHEDULE
    val isCompleted: Boolean = false,
    val dateString: String // e.g. "2026-07-13" or "RECURRING"
)

@Entity(tableName = "sleep_logs")
data class SleepLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,
    val bedTime: String, // e.g. "23:00"
    val wakeTime: String, // e.g. "07:00"
    val sleepDurationMinutes: Int,
    val sleepQualityScore: Int, // 1 to 5 stars or %
    val notes: String = ""
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val language: String = "English",
    val is24HourFormat: Boolean = false,
    val themeName: String = "Bento Grid",
    val amoledDark: Boolean = false,
    val hasCompletedWelcome: Boolean = false,
    val pinLock: String = "",
    val snoozeTotalCount: Int = 0,
    val onTimeWakeCount: Int = 0,
    val isPremium: Boolean = false
)

// ==========================================
// DAOS
// ==========================================

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarmsFlow(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms")
    suspend fun getAllAlarmsDirect(): List<Alarm>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: Int): Alarm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Int)
}

@Dao
interface WorldCityDao {
    @Query("SELECT * FROM world_cities")
    fun getAllCitiesFlow(): Flow<List<WorldCity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: WorldCity): Long

    @Query("DELETE FROM world_cities WHERE id = :id")
    suspend fun deleteCityById(id: Int)
}

@Dao
interface PlannerTaskDao {
    @Query("SELECT * FROM planner_tasks ORDER BY timeString ASC")
    fun getAllTasksFlow(): Flow<List<PlannerTask>>

    @Query("SELECT * FROM planner_tasks WHERE dateString = :dateString ORDER BY timeString ASC")
    fun getTasksByDateFlow(dateString: String): Flow<List<PlannerTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PlannerTask): Long

    @Update
    suspend fun updateTask(task: PlannerTask)

    @Query("DELETE FROM planner_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)
}

@Dao
interface SleepLogDao {
    @Query("SELECT * FROM sleep_logs ORDER BY dateString DESC")
    fun getAllSleepLogsFlow(): Flow<List<SleepLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(log: SleepLog): Long

    @Query("DELETE FROM sleep_logs WHERE id = :id")
    suspend fun deleteSleepLogById(id: Int)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getAppSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)
}

// ==========================================
// DATABASE
// ==========================================

@Database(
    entities = [Alarm::class, WorldCity::class, PlannerTask::class, SleepLog::class, AppSettings::class],
    version = 1,
    exportSchema = false
)
abstract class ClockDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun worldCityDao(): WorldCityDao
    abstract fun plannerTaskDao(): PlannerTaskDao
    abstract fun sleepLogDao(): SleepLogDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: ClockDatabase? = null

        fun getDatabase(context: Context): ClockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClockDatabase::class.java,
                    "infinity_clock_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// REPOSITORY
// ==========================================

class ClockRepository(private val db: ClockDatabase) {
    val alarmsFlow: Flow<List<Alarm>> = db.alarmDao().getAllAlarmsFlow()
    val citiesFlow: Flow<List<WorldCity>> = db.worldCityDao().getAllCitiesFlow()
    val tasksFlow: Flow<List<PlannerTask>> = db.plannerTaskDao().getAllTasksFlow()
    val sleepLogsFlow: Flow<List<SleepLog>> = db.sleepLogDao().getAllSleepLogsFlow()
    val settingsFlow: Flow<AppSettings?> = db.appSettingsDao().getAppSettingsFlow()

    fun getTasksByDateFlow(dateString: String): Flow<List<PlannerTask>> =
        db.plannerTaskDao().getTasksByDateFlow(dateString)

    suspend fun getSettingsDirect(): AppSettings {
        return db.appSettingsDao().getAppSettingsDirect() ?: AppSettings().also {
            db.appSettingsDao().insertSettings(it)
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        db.appSettingsDao().insertSettings(settings)
    }

    suspend fun insertAlarm(alarm: Alarm) = db.alarmDao().insertAlarm(alarm)
    suspend fun updateAlarm(alarm: Alarm) = db.alarmDao().updateAlarm(alarm)
    suspend fun deleteAlarm(id: Int) = db.alarmDao().deleteAlarmById(id)

    suspend fun insertCity(city: WorldCity) = db.worldCityDao().insertCity(city)
    suspend fun deleteCity(id: Int) = db.worldCityDao().deleteCityById(id)

    suspend fun insertTask(task: PlannerTask) = db.plannerTaskDao().insertTask(task)
    suspend fun updateTask(task: PlannerTask) = db.plannerTaskDao().updateTask(task)
    suspend fun deleteTask(id: Int) = db.plannerTaskDao().deleteTaskById(id)

    suspend fun insertSleepLog(log: SleepLog) = db.sleepLogDao().insertSleepLog(log)
    suspend fun deleteSleepLog(id: Int) = db.sleepLogDao().deleteSleepLogById(id)
}
