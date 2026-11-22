package com.example

import android.Manifest
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import com.example.service.AutoCloseAlarmReceiver
import com.example.service.AutoStoreCloseManager
import com.example.service.PosBackgroundService
import com.example.service.PosBootReceiver
import com.example.service.PosNotificationActionReceiver
import com.example.service.PosNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackgroundCrashProtectionTest {

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        PosApplication.resetActivityCountForTest()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Shadows.shadowOf(app).clearStartedServices()
    }

    @Test
    fun `PosBackgroundService returns START_NOT_STICKY and handles onTimeout safely`() {
        val controller = Robolectric.buildService(
            PosBackgroundService::class.java,
            Intent(app, PosBackgroundService::class.java).apply {
                action = PosBackgroundService.ACTION_START
            }
        ).create()
        val service = controller.get()

        val startResult = service.onStartCommand(
            Intent(app, PosBackgroundService::class.java).apply {
                action = PosBackgroundService.ACTION_START
            },
            0,
            1
        )
        assertEquals(Service.START_NOT_STICKY, startResult)
        assertTrue(PosBackgroundService.isRunning.value)

        // Simulasikan Android 15/16 memanggil onTimeout setelah kuota 6 jam dataSync habis
        service.onTimeout(1, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        assertFalse(PosBackgroundService.isRunning.value)

        // Null-intent restart di background pada API 35 harus kembali START_NOT_STICKY tanpa crash
        val nullRestartResult = service.onStartCommand(null, 0, 2)
        assertEquals(Service.START_NOT_STICKY, nullRestartResult)
        assertFalse(PosBackgroundService.isRunning.value)

        controller.destroy()
    }

    @Test
    fun `PosBackgroundService start and stop when app is closed never calls background startService`() {
        // Aplikasi sedang tidak dibuka (tidak ada Activity foreground)
        assertFalse(PosApplication.hasStartedActivity())

        PosBackgroundService.start(app)
        // Tidak boleh memanggil startForegroundService dari background pada API 31+
        assertNull(Shadows.shadowOf(app).nextStartedService)

        PosBackgroundService.checkStockNow(app)
        assertNull(Shadows.shadowOf(app).nextStartedService)

        PosBackgroundService.stop(app)
        // stop() wajib memakai stopService() dan tidak boleh memanggil startService()
        assertNull(Shadows.shadowOf(app).nextStartedService)
        assertFalse(PosBackgroundService.isRunning.value)
    }

    @Test
    fun `foreground notification actions use Broadcast PendingIntents instead of getService`() {
        val notif = PosNotificationHelper.buildForegroundServiceNotification(
            context = app,
            storeName = "Toko POS",
            lowStockCount = 2,
            outOfStockCount = 1
        )
        assertEquals(2, notif.actions.size)
        notif.actions.forEach { action ->
            val shadowPi = Shadows.shadowOf(action.actionIntent)
            assertTrue(
                "Action '${action.title}' must use broadcast PendingIntent",
                shadowPi.isBroadcastIntent
            )
            assertFalse(
                "Action '${action.title}' must NOT use service PendingIntent",
                shadowPi.isServiceIntent
            )
            assertEquals(
                PosNotificationActionReceiver::class.java.name,
                shadowPi.savedIntent?.component?.className
            )
        }
    }

    @Test
    fun `PosNotificationActionReceiver handles STOP and CHECK_STOCK safely while app is closed`() {
        val prefs = app.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PosBackgroundService.KEY_BG_SERVICE_ENABLED, true).apply()

        val receiver = PosNotificationActionReceiver()
        receiver.onReceive(
            app,
            Intent(app, PosNotificationActionReceiver::class.java).apply {
                action = PosBackgroundService.ACTION_STOP
                putExtra(PosBackgroundService.EXTRA_FROM_NOTIFICATION, true)
            }
        )
        val deadline = System.currentTimeMillis() + 3_000L
        while (prefs.getBoolean(PosBackgroundService.KEY_BG_SERVICE_ENABLED, true) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50L)
        }
        assertFalse(prefs.getBoolean(PosBackgroundService.KEY_BG_SERVICE_ENABLED, true))
        assertNull(Shadows.shadowOf(app).nextStartedService)

        receiver.onReceive(
            app,
            Intent(app, PosNotificationActionReceiver::class.java).apply {
                action = PosBackgroundService.ACTION_CHECK_STOCK
            }
        )
        assertNull(Shadows.shadowOf(app).nextStartedService)
    }

    @Test
    fun `AutoCloseAlarmReceiver and PosBootReceiver run safely in background on SDK 35`() {
        val prefs = app.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(AutoStoreCloseManager.KEY_AUTO_CLOSE_ENABLED, false)
            .putBoolean(PosBackgroundService.KEY_BG_SERVICE_ENABLED, true)
            .apply()

        val autoCloseReceiver = AutoCloseAlarmReceiver()
        autoCloseReceiver.onReceive(
            app,
            Intent(app, AutoCloseAlarmReceiver::class.java).apply {
                action = AutoCloseAlarmReceiver.ACTION_AUTO_CLOSE_TRIGGERED
            }
        )

        val bootReceiver = PosBootReceiver()
        bootReceiver.onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))
        bootReceiver.onReceive(app, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        // Tidak boleh memanggil startForegroundService saat boot atau package replaced di API 35
        assertNull(Shadows.shadowOf(app).nextStartedService)
    }
}
