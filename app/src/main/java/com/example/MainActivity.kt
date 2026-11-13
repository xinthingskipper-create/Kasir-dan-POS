package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserRole
import com.example.service.PosNotificationHelper
import com.example.ui.MainTab
import com.example.ui.PosViewModel
import com.example.ui.Screen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainAppContainer
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var pendingOpenTab by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        pendingOpenTab = intent?.getStringExtra(PosNotificationHelper.EXTRA_OPEN_TAB)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: PosViewModel = viewModel()
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val currentUser by viewModel.currentUser.collectAsState()

                    LaunchedEffect(pendingOpenTab, currentUser) {
                        if (pendingOpenTab == PosNotificationHelper.TAB_INVENTORY && currentUser != null) {
                            if (currentUser?.role == UserRole.ADMIN) {
                                viewModel.selectTab(MainTab.INVENTORY)
                            }
                            pendingOpenTab = null
                        }
                    }

                    when (currentScreen) {
                        Screen.SPLASH -> {
                            SplashScreen(
                                onNavigateNext = { nextScreen ->
                                    viewModel.navigateTo(nextScreen)
                                }
                            )
                        }
                        Screen.LOGIN -> {
                            LoginScreen(viewModel = viewModel)
                        }
                        Screen.MAIN -> {
                            MainAppContainer(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpenTab = intent.getStringExtra(PosNotificationHelper.EXTRA_OPEN_TAB)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    /**
     * Hides system bars (navigation bar and status bar) automatically for true fullscreen mode.
     * When the user swipes from the edge, transient bars appear briefly and auto-hide.
     */
    private fun hideSystemBars() {
        try {
            window.decorView.post {
                try {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                } catch (t: Throwable) {
                    // Safe ignore: system bars controller may be unsupported on some emulators
                }
            }
        } catch (t: Throwable) {
            // Safe ignore
        }
    }
}

