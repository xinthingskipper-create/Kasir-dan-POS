package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyReport
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) Room untuk tabel `daily_reports`.
 * Mengelola query reaktif Flow dan fungsi suspend untuk laporan penjualan lokal.
 */
@Dao
interface DailyReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyReport(report: DailyReport): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyReports(reports: List<DailyReport>)

    @Update
    suspend fun updateDailyReport(report: DailyReport)

    @Query("SELECT * FROM daily_reports ORDER BY dateTimestamp DESC")
    fun getAllDailyReports(): Flow<List<DailyReport>>

    @Query("SELECT * FROM daily_reports ORDER BY dateTimestamp DESC")
    suspend fun getAllDailyReportsList(): List<DailyReport>

    @Query("SELECT * FROM daily_reports WHERE date = :date LIMIT 1")
    fun getDailyReportByDate(date: String): Flow<DailyReport?>

    @Query("SELECT * FROM daily_reports WHERE date = :date LIMIT 1")
    suspend fun getDailyReportByDateDirect(date: String): DailyReport?

    @Query("SELECT * FROM daily_reports WHERE dateTimestamp >= :startTime AND dateTimestamp <= :endTime ORDER BY dateTimestamp DESC")
    fun getDailyReportsBetween(startTime: Long, endTime: Long): Flow<List<DailyReport>>

    @Query("SELECT * FROM daily_reports WHERE id = :id LIMIT 1")
    suspend fun getDailyReportById(id: Long): DailyReport?

    @Query("DELETE FROM daily_reports WHERE id = :id")
    suspend fun deleteDailyReportById(id: Long)

    @Query("DELETE FROM daily_reports WHERE date = :date")
    suspend fun deleteDailyReportByDate(date: String)

    @Query("DELETE FROM daily_reports")
    suspend fun deleteAllDailyReports()

    @Query("SELECT COUNT(*) FROM daily_reports")
    fun getDailyReportsCount(): Flow<Int>

    @Query("SELECT SUM(totalRevenue) FROM daily_reports")
    fun getTotalArchivedRevenue(): Flow<Double?>

    // Pelacakan sinkronisasi Cloud
    @Query("SELECT * FROM daily_reports WHERE isSynced = 0 ORDER BY dateTimestamp DESC")
    fun getUnsyncedDailyReports(): Flow<List<DailyReport>>

    @Query("UPDATE daily_reports SET isSynced = 1, syncedAt = :syncedAt WHERE date = :date")
    suspend fun markDailyReportAsSynced(date: String, syncedAt: Long = System.currentTimeMillis())
}
