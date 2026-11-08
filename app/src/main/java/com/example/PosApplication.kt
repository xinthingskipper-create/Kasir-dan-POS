package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Custom Application class for Kasir POS.
 * Ensures critical SDKs like Firebase are initialized safely without crashing the app.
 */
class PosApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        installCrashShield()
        initializeFirebaseSafely()
    }

    private fun installCrashShield() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("PosCrashShield", "Intercepted uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            // If the crash happens on a non-main thread or background sync/coroutine, log and prevent hard crash
            if (thread.name.contains("DefaultDispatcher") || thread.name.contains("Firestore") || thread.name.contains("OkHttp") || thread.name.contains("grpc")) {
                Log.w("PosCrashShield", "Suppressed non-fatal background/network crash on thread: ${thread.name}")
                return@setDefaultUncaughtExceptionHandler
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun initializeFirebaseSafely() {
        try {
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
}
