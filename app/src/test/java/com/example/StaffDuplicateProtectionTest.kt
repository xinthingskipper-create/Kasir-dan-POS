package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PosDatabase
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.BackupRestoreManager
import com.example.data.repository.PosRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StaffDuplicateProtectionTest {

    private lateinit var app: Application
    private lateinit var database: PosDatabase
    private lateinit var repository: PosRepository

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(app, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PosRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `concurrent seedDefaultUsersIfEmpty and resetDefaultUsers never create duplicate staff accounts`() = runBlocking {
        // Simulasikan beberapa coroutine memanggil seedDefaultUsersIfEmpty() bersamaan seperti startup race
        (1..5).map {
            async { repository.seedDefaultUsersIfEmpty() }
        }.awaitAll()

        val afterSeed = database.userDao().getAllUsersList()
        assertEquals(3, afterSeed.size)
        assertEquals(setOf("admin", "kasir1", "kasir2"), afterSeed.map { it.username }.toSet())

        // Panggil resetDefaultUsers berkali-kali -> tidak boleh menambah akun ganda
        repository.resetDefaultUsers()
        repository.resetDefaultUsers()

        val afterReset = database.userDao().getAllUsersList()
        assertEquals(3, afterReset.size)
    }

    @Test
    fun `insertUser prevents duplicate usernames and deduplicateUsersIfNeeded removes legacy duplicates`() = runBlocking {
        // Simulasikan database lama yang sudah terlanjur punya akun ganda
        database.userDao().insertUsers(
            listOf(
                User(username = "admin", pin = "1234", fullName = "Budi Santoso (Admin)", role = UserRole.ADMIN),
                User(username = "admin", pin = "1234", fullName = "Budi Santoso (Admin)", role = UserRole.ADMIN),
                User(username = "kasir1", pin = "1111", fullName = "Siti Aminah (Kasir 1)", role = UserRole.KASIR),
                User(username = "KASIR1 ", pin = "1111", fullName = "Siti Aminah (Kasir 1)", role = UserRole.KASIR)
            )
        )
        assertEquals(4, database.userDao().getAllUsersList().size)

        repository.deduplicateUsersIfNeeded()
        val deduped = repository.getAllActiveUsers().first()
        assertEquals(2, deduped.size)
        assertEquals(setOf("admin", "kasir1"), deduped.map { it.username }.toSet())

        // Coba tambahkan username yang sama (beda kapital/spasi) -> tetap ter-upsert tanpa duplikat
        repository.insertUser(
            User(username = " Kasir1 ", pin = "9999", fullName = "Siti Baru", role = UserRole.KASIR)
        )
        val afterUpsert = database.userDao().getAllUsersList()
        assertEquals(2, afterUpsert.size)
        assertEquals("Siti Baru", afterUpsert.first { it.username == "kasir1" }.fullName)
    }

    @Test
    fun `restoreFromJsonString reuses existing user IDs and avoids duplicate staff accounts`() = runBlocking {
        repository.seedDefaultUsersIfEmpty()
        val initialUsers = database.userDao().getAllUsersList()
        assertEquals(3, initialUsers.size)

        val backupManager = BackupRestoreManager(app, database)
        val exported = backupManager.exportBackup()

        val result = backupManager.restoreFromJsonString(exported.jsonString, preserveEditedAndDeleted = false)
        assertTrue(result.isSuccess)

        val afterRestore = database.userDao().getAllUsersList()
        assertEquals(3, afterRestore.size)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
