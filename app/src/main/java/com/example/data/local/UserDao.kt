package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY fullName ASC")
    fun getAllActiveUsers(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsersList(): List<User>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?

    @Query("SELECT * FROM users WHERE LOWER(TRIM(username)) = LOWER(TRIM(:username)) ORDER BY id ASC LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) AND pin = :pin AND isActive = 1 LIMIT 1")
    suspend fun authenticate(username: String, pin: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query(
        """
        DELETE FROM users
        WHERE id NOT IN (
            SELECT MIN(id)
            FROM users
            GROUP BY LOWER(TRIM(username))
        )
        """
    )
    suspend fun deduplicateUsersByUsername()
}
