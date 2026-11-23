package com.example.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * Helper aman untuk menormalkan URL hosting laporan dan membukanya di Google Chrome
 * (atau browser default perangkat jika Chrome tidak terpasang) pada Android 11–16.
 */
object BrowserLauncher {

    private const val TAG = "BrowserLauncher"
    private const val CHROME_PACKAGE = "com.android.chrome"

    fun normalizeHttpUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim().replace("\n", "").replace("\r", "")
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        val isLocal = trimmed.startsWith("127.0.0.1") ||
            trimmed.startsWith("localhost", ignoreCase = true) ||
            trimmed.startsWith("192.168.") ||
            trimmed.startsWith("10.")
        val scheme = if (isLocal) "http://" else "https://"
        return "$scheme$trimmed"
    }

    fun openInChrome(
        context: Context,
        rawUrl: String,
        showFallbackToast: Boolean = true
    ): Boolean {
        val cleanUrl = normalizeHttpUrl(rawUrl)
        if (cleanUrl.isBlank()) {
            if (showFallbackToast) {
                Toast.makeText(context, "URL hosting web belum diatur", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        val uri = try {
            Uri.parse(cleanUrl)
        } catch (t: Throwable) {
            Log.w(TAG, "Invalid URL '$cleanUrl': ${t.message}")
            copyToClipboard(context, cleanUrl)
            if (showFallbackToast) {
                Toast.makeText(context, "URL tidak valid; disalin: $cleanUrl", Toast.LENGTH_LONG).show()
            }
            return false
        }

        // 1. Coba buka langsung di Google Chrome
        val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(CHROME_PACKAGE)
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chromeIntent)
            return true
        } catch (_: ActivityNotFoundException) {
            // Lanjutkan ke browser default
        } catch (t: Throwable) {
            Log.w(TAG, "Chrome direct launch fallback: ${t.message}")
        }

        // 2. Fallback ke browser default sistem
        val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(browserIntent)
            return true
        } catch (_: ActivityNotFoundException) {
            // Lanjutkan ke chooser / clipboard fallback
        } catch (t: Throwable) {
            Log.w(TAG, "Default browser launch fallback: ${t.message}")
        }

        // 3. Fallback ke chooser jika dibutuhkan
        try {
            val chooser = Intent.createChooser(browserIntent, "Buka Halaman Laporan").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "Browser unavailable for URL '$cleanUrl': ${t.message}")
        }

        // 4. Fallback terakhir: salin ke clipboard agar user tetap bisa paste di Chrome
        copyToClipboard(context, cleanUrl)
        if (showFallbackToast) {
            Toast.makeText(context, "Browser tidak ditemukan, URL disalin: $cleanUrl", Toast.LENGTH_LONG).show()
        }
        return false
    }

    private fun copyToClipboard(context: Context, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Web Report URL", text))
        } catch (_: Throwable) {
        }
    }
}
