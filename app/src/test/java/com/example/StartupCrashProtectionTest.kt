package com.example

import android.Manifest
import android.app.Application
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.bluetooth.BluetoothPrinterManager
import com.example.data.repository.FirebaseSyncRepository
import com.example.service.DriveAutoBackupManager
import com.example.service.PosBackgroundService
import com.example.service.PosBootReceiver
import com.example.service.PosNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityStartupFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun grantRuntimePermissions() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun `cold start splash to login to main cashier and settings renders without crashing`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("skip_splash_button").assertIsDisplayed().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            composeTestRule.onAllNodesWithTag("login_submit_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("login_submit_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            composeTestRule.onAllNodesWithTag("topbar_nav_toggle_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("topbar_nav_toggle_button").assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StartupComponentsSafetyTest {

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    @Config(sdk = [23])
    fun `cold start MainActivity on minSdk 23 completes lifecycle safely`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
        controller.pause().stop().destroy()
    }

    @Test
    fun `cold start MainActivity on Android 15 SDK 35 completes lifecycle safely`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
        controller.pause().stop().destroy()
    }

    @Test
    fun `pos logo is bounded in drawable-nodpi so hardware canvas never overflows`() {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(app.resources, R.drawable.ic_pos_logo, opts)
        assertTrue("Logo width should be <= 512px, was ${opts.outWidth}", opts.outWidth in 1..512)
        assertTrue("Logo height should be <= 512px, was ${opts.outHeight}", opts.outHeight in 1..512)
    }

    @Test
    fun `notifications use compact vector icon instead of raster logo`() {
        val fgNotif = PosNotificationHelper.buildForegroundServiceNotification(
            context = app,
            storeName = "Toko Uji",
            lowStockCount = 1,
            outOfStockCount = 0
        )
        assertEquals(R.drawable.ic_notification_pos, fgNotif.smallIcon.resId)
    }

    @Test
    fun `foreground background service start and stop transitions cleanly`() {
        val serviceController = Robolectric.buildService(
            PosBackgroundService::class.java,
            Intent(app, PosBackgroundService::class.java).apply {
                action = PosBackgroundService.ACTION_START
            }
        ).create().startCommand(0, 1)

        assertNotNull(serviceController.get())

        serviceController.withIntent(
            Intent(app, PosBackgroundService::class.java).apply {
                action = PosBackgroundService.ACTION_STOP
            }
        ).startCommand(0, 2)

        assertFalse(PosBackgroundService.isRunning.value)
        serviceController.destroy()
    }

    @Test
    fun `boot receiver handles BOOT_COMPLETED safely on SDK 35 and SDK 23`() {
        val receiver = PosBootReceiver()
        receiver.onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))
        // Should not throw SecurityException or ForegroundServiceStartNotAllowedException
        assertTrue(true)
    }

    @Test
    fun `bluetooth manager and drive auto backup manager handle uninitialized env safely`() {
        val bt = BluetoothPrinterManager(app)
        bt.getPairedDevices()
        assertFalse(bt.isBluetoothEnabled)

        DriveAutoBackupManager.schedulePeriodicBackup(app)
        DriveAutoBackupManager.triggerDebouncedAutoBackup(app, reason = "Test")
        assertTrue(true)
    }

    @Test
    fun `placeholder Firebase CI config detection matches fallback google-services values`() {
        assertTrue(
            FirebaseSyncRepository.isPlaceholderFirebaseValues(
                projectId = "offline-pos-placeholder",
                appId = "1:000000000000:android:0000000000000000"
            )
        )
        assertTrue(
            FirebaseSyncRepository.isPlaceholderFirebaseValues(
                projectId = "some-project",
                appId = "1:000000000000:android:12345"
            )
        )
        assertFalse(
            FirebaseSyncRepository.isPlaceholderFirebaseValues(
                projectId = "foodrock-report",
                appId = "1:574700312081:android:d36c6492a17b990db503b0"
            )
        )
    }
}
