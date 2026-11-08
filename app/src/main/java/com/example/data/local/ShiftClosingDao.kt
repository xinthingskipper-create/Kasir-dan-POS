package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ShiftClosing
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftClosingDao {
    @Query("SELECT * FROM shift_closings ORDER BY closingTime DESC")
    fun getAllShiftClosings(): Flow<List<ShiftClosing>>

    @Query("SELECT * FROM shift_closings ORDER BY closingTime ASC")
    suspend fun getAllShiftClosingsList(): List<ShiftClosing>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftClosing(closing: ShiftClosing): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftClosings(closings: List<ShiftClosing>)

    @Query("SELECT * FROM shift_closings WHERE id = :id LIMIT 1")
    suspend fun getShiftClosingById(id: Long): ShiftClosing?
}
