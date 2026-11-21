package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.service.PosBackgroundService
import com.example.ui.MainTab
import com.example.ui.PosViewModel
import com.example.ui.StoreSettingsCategory
import com.example.ui.screens.StoreSettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StoreSettingsCategoryTabsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PosBackgroundService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    @Config(sdk = [35], qualifiers = "w411dp-h915dp")
    fun `StoreSettingsScreen top category tabs separate settings by category`() {
        val viewModel = PosViewModel(context)

        composeTestRule.setContent {
            StoreSettingsScreen(viewModel = viewModel)
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("store_settings_tab_bar").assertIsDisplayed()

        // Default tab: PROFILE -> shows profile & receipt card, hides other category cards
        composeTestRule.onNodeWithTag("store_profile_settings_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("web_hosting_settings_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("google_drive_backup_card").assertDoesNotExist()

        // Switch to OPERATIONAL
        composeTestRule.onNodeWithTag("store_settings_tab_OPERATIONAL")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(StoreSettingsCategory.OPERATIONAL, viewModel.selectedStoreSettingsCategory.value)
        composeTestRule.onNodeWithTag("operational_settings_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("store_profile_settings_card").assertDoesNotExist()

        // Switch to AUTOMATION (Layanan & Jadwal)
        composeTestRule.onNodeWithTag("store_settings_tab_AUTOMATION")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(StoreSettingsCategory.AUTOMATION, viewModel.selectedStoreSettingsCategory.value)
        composeTestRule.onNodeWithTag("background_notification_settings_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auto_store_close_settings_card").assertExists()

        // Switch to HOSTING
        composeTestRule.onNodeWithTag("store_settings_tab_HOSTING")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(StoreSettingsCategory.HOSTING, viewModel.selectedStoreSettingsCategory.value)
        composeTestRule.onNodeWithTag("web_hosting_settings_card").assertIsDisplayed()

        // Switch to BACKUP -> shows Google Drive + Local JSON backup cards
        composeTestRule.onNodeWithTag("store_settings_tab_BACKUP")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(StoreSettingsCategory.BACKUP, viewModel.selectedStoreSettingsCategory.value)
        composeTestRule.onNodeWithTag("csv_backup_restore_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_drive_backup_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("backup_restore_card").assertExists()
    }

    @Test
    fun `openStoreSettings navigates to SETTINGS tab and selects target category`() {
        val viewModel = PosViewModel(context)

        viewModel.openStoreSettings(StoreSettingsCategory.BACKUP)
        assertEquals(MainTab.SETTINGS, viewModel.currentTab.value)
        assertEquals(StoreSettingsCategory.BACKUP, viewModel.selectedStoreSettingsCategory.value)

        viewModel.openStoreSettings(StoreSettingsCategory.OPERATIONAL)
        assertEquals(MainTab.SETTINGS, viewModel.currentTab.value)
        assertEquals(StoreSettingsCategory.OPERATIONAL, viewModel.selectedStoreSettingsCategory.value)
    }
}
