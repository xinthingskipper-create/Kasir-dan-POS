package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.example.data.repository.FirebaseSyncRepository
import com.google.firebase.FirebaseApp
import java.util.concurrent.atomic.AtomicInteger

/**
 * Custom Application class for Kasir POS.
 * Ensures critical SDKs like Firebase are initialized safely without crashing the app,
 * tracks foreground Activity visibility for Android 12+ foreground-service compliance,
 * and prevents third-party background SDK threads from crashing the closed app.
 */
class PosApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        registerForegroundTracker()
        installCrashShield()
        initializeFirebaseSafely()
    }

    private fun registerForegroundTracker() {
        try {
            registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {
                    startedActivityCount.incrementAndGet()
                }
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {
                    val current = startedActivityCount.decrementAndGet()
                    if (current < 0) startedActivityCount.set(0)
                }
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            })
        } catch (t: Throwable) {
            Log.w("PosApplication", "Lifecycle tracker unavailable: ${t.message}")
        }
    }

    private fun installCrashShield() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(
                "PosCrashShield",
                "Intercepted uncaught exception on thread ${thread.name}: ${throwable.message}",
                throwable
            )
            val mainThread = try {
                Looper.getMainLooper().thread
            } catch (_: Throwable) {
                null
            }
            val isMainThread = mainThread != null && thread.id == mainThread.id
            val isTestThread = thread.name.contains("Test worker", ignoreCase = true) ||
                thread.name.contains("Instr:", ignoreCase = true)

            if (!isMainThread && !isTestThread && throwable !is VirtualMachineError) {
                val isKnownBackgroundThread =
                    thread.name.contains("DefaultDispatcher", ignoreCase = true) ||
                        thread.name.contains("Firestore", ignoreCase = true) ||
                        thread.name.contains("Firebase", ignoreCase = true) ||
                        thread.name.contains("OkHttp", ignoreCase = true) ||
                        thread.name.contains("grpc", ignoreCase = true) ||
                        thread.name.contains("WM.task", ignoreCase = true) ||
                        thread.name.contains("androidx.work", ignoreCase = true) ||
                        thread.name.contains("arch_disk_io", ignoreCase = true) ||
                        thread.name.contains("AsyncTask", ignoreCase = true) ||
                        thread.name.contains("GoogleApi", ignoreCase = true) ||
                        thread.name.contains("Gms", ignoreCase = true) ||
                        thread.name.contains("PlayServices", ignoreCase = true) ||
                        thread.name.contains("LocalWebReportServer", ignoreCase = true) ||
                        thread.name.contains("Binder", ignoreCase = true) ||
                        thread.name.contains("pool-", ignoreCase = true)

                if (isKnownBackgroundThread) {
                    Log.w("PosCrashShield", "Suppressed non-fatal background crash on thread: ${thread.name}")
                    return@setDefaultUncaughtExceptionHandler
                }
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun initializeFirebaseSafely() {
        try {
            if (FirebaseSyncRepository.isPlaceholderFirebaseConfig(this)) {
                Log.i("PosApplication", "Placeholder google-services config detected; skipping FirebaseApp initialization")
                return
            }
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                Log.i("PosApplication", "FirebaseApp initialized successfully: ${app?.name}")
            } else {
                Log.i("PosApplication", "FirebaseApp was already initialized by Provider")
            }
        } catch (t: Throwable) {
            Log.e("PosApplication", "Non-fatal: Failed to initialize FirebaseApp during startup", t)
        }
    }

    companion object {
        private val startedActivityCount = AtomicInteger(0)

        fun hasStartedActivity(): Boolean = startedActivityCount.get() > 0

        fun resetActivityCountForTest() {
            startedActivityCount.set(0)
        }
    }
}

