package com.example.data.repository

import android.content.Context
import org.json.JSONObject
import java.util.Locale

/**
 * Menyimpan jejak item dan pengaturan yang pernah diedit atau dihapus secara lokal
 * agar proses restore backup offline (.csv / .json) tidak mengembalikan data yang
 * sudah dihapus maupun menimpa perubahan yang telah dilakukan pengguna.
 */
object RestoreGuardTracker {

    const val GUARD_PREFS_NAME = "pos_restore_guard_prefs"

    private const val KEY_DELETED_PRODUCT_IDS = "deleted_product_ids"
    private const val KEY_DELETED_PRODUCT_NAMES = "deleted_product_names"
    private const val KEY_EDITED_PRODUCT_IDS = "edited_product_ids"

    private const val KEY_DELETED_USER_IDS = "deleted_user_ids"
    private const val KEY_DELETED_USERNAMES = "deleted_usernames"
    private const val KEY_EDITED_USER_IDS = "edited_user_ids"
    private const val KEY_EDITED_USERNAMES = "edited_usernames"

    private const val KEY_DELETED_EXPENSE_IDS = "deleted_expense_ids"
    private const val KEY_DELETED_EXPENSE_FPS = "deleted_expense_fps"
    private const val KEY_EDITED_EXPENSE_IDS = "edited_expense_ids"

    private const val KEY_DELETED_TRANSACTION_IDS = "deleted_transaction_ids"
    private const val KEY_DELETED_TRANSACTION_INVOICES = "deleted_transaction_invoices"

    private const val KEY_DELETED_DAILY_REPORT_IDS = "deleted_daily_report_ids"
    private const val KEY_DELETED_DAILY_REPORT_DATES = "deleted_daily_report_dates"

    private const val KEY_DELETED_CATEGORIES = "deleted_categories"
    private const val KEY_RENAMED_CATEGORIES_JSON = "renamed_categories_json"
    private const val KEY_DELETED_UNITS = "deleted_units"

    private const val KEY_EDITED_SETTINGS = "edited_settings"

    // Identifier kunci pengaturan granular
    const val SETTING_STORE_PROFILE = "store_profile"
    const val SETTING_CLOUD_HOSTING = "cloud_hosting"
    const val SETTING_CUSTOM_CATEGORIES = "custom_categories"
    const val SETTING_CUSTOM_UNITS = "custom_units"
    const val SETTING_FOOD_DEFAULT_UNIT = "food_default_unit"
    const val SETTING_DRINK_DEFAULT_UNIT = "drink_default_unit"
    const val SETTING_TABLE_COUNT = "table_count"
    const val SETTING_FIREBASE_PROJECT_ID = "firebase_project_id"
    const val SETTING_FIREBASE_DATABASE_ID = "firebase_database_id"
    const val SETTING_WEB_HOSTING_URL = "web_hosting_url"
    const val SETTING_BG_SERVICE_ENABLED = "bg_service_enabled"
    const val SETTING_LOW_STOCK_NOTIF_ENABLED = "low_stock_notif_enabled"
    const val SETTING_AUTO_CLOSE = "auto_close"
    const val SETTING_DRIVE_AUTO_BACKUP_ENABLED = "drive_auto_backup_enabled"
    const val SETTING_DRIVE_AUTO_BACKUP_FREQUENCY = "drive_auto_backup_frequency"
    const val SETTING_DRIVE_AUTO_BACKUP_TIME = "drive_auto_backup_time"
    const val SETTING_DRIVE_AUTO_BACKUP_WIFI_ONLY = "drive_auto_backup_wifi_only"
    const val SETTING_DRIVE_AUTO_BACKUP_ON_SHIFT_CLOSE = "drive_auto_backup_on_shift_close"
    const val SETTING_DRIVE_AUTO_BACKUP_KEEP_COUNT = "drive_auto_backup_keep_count"

    private val lock = Any()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(GUARD_PREFS_NAME, Context.MODE_PRIVATE)

    private fun normalize(raw: String): String = raw.trim().lowercase(Locale.ROOT)

    fun expenseFingerprint(title: String, timestamp: Long): String =
        "${normalize(title)}|$timestamp"

    private fun addToStringSet(context: Context, key: String, values: Collection<String>) {
        if (values.isEmpty()) return
        synchronized(lock) {
            val p = prefs(context)
            val updated = (p.getStringSet(key, null) ?: emptySet()).toMutableSet()
            values.forEach { v ->
                if (v.isNotBlank()) updated.add(v)
            }
            p.edit().putStringSet(key, updated).apply()
        }
    }

    private fun removeFromStringSet(context: Context, key: String, value: String) {
        if (value.isBlank()) return
        synchronized(lock) {
            val p = prefs(context)
            val current = p.getStringSet(key, null) ?: return
            if (!current.contains(value)) return
            val updated = current.toMutableSet()
            updated.remove(value)
            p.edit().putStringSet(key, updated).apply()
        }
    }

    private fun getLongSet(context: Context, key: String): Set<Long> {
        synchronized(lock) {
            return (prefs(context).getStringSet(key, null) ?: emptySet())
                .mapNotNull { it.toLongOrNull() }
                .toSet()
        }
    }

    private fun getStringSetCopy(context: Context, key: String): Set<String> {
        synchronized(lock) {
            return (prefs(context).getStringSet(key, null) ?: emptySet()).toSet()
        }
    }

    // --- Products ---
    fun recordProductDeleted(context: Context, id: Long, name: String) {
        if (id > 0L) addToStringSet(context, KEY_DELETED_PRODUCT_IDS, listOf(id.toString()))
        val norm = normalize(name)
        if (norm.isNotBlank()) addToStringSet(context, KEY_DELETED_PRODUCT_NAMES, listOf(norm))
    }

    fun clearProductNameDeleted(context: Context, name: String) {
        val norm = normalize(name)
        if (norm.isNotBlank()) removeFromStringSet(context, KEY_DELETED_PRODUCT_NAMES, norm)
    }

    fun recordProductEdited(context: Context, id: Long) {
        if (id > 0L) addToStringSet(context, KEY_EDITED_PRODUCT_IDS, listOf(id.toString()))
    }

    fun recordProductsEdited(context: Context, ids: Collection<Long>) {
        val valid = ids.filter { it > 0L }.map { it.toString() }
        addToStringSet(context, KEY_EDITED_PRODUCT_IDS, valid)
    }

    fun getDeletedProductIds(context: Context): Set<Long> = getLongSet(context, KEY_DELETED_PRODUCT_IDS)
    fun getDeletedProductNames(context: Context): Set<String> = getStringSetCopy(context, KEY_DELETED_PRODUCT_NAMES)
    fun getEditedProductIds(context: Context): Set<Long> = getLongSet(context, KEY_EDITED_PRODUCT_IDS)

    // --- Users ---
    fun recordUserDeleted(context: Context, id: Long, username: String) {
        if (id > 0L) addToStringSet(context, KEY_DELETED_USER_IDS, listOf(id.toString()))
        val norm = normalize(username)
        if (norm.isNotBlank()) addToStringSet(context, KEY_DELETED_USERNAMES, listOf(norm))
    }

    fun clearUsernameDeleted(context: Context, username: String) {
        val norm = normalize(username)
        if (norm.isNotBlank()) removeFromStringSet(context, KEY_DELETED_USERNAMES, norm)
    }

    fun recordUserEdited(context: Context, id: Long, username: String) {
        if (id > 0L) addToStringSet(context, KEY_EDITED_USER_IDS, listOf(id.toString()))
        val norm = normalize(username)
        if (norm.isNotBlank()) addToStringSet(context, KEY_EDITED_USERNAMES, listOf(norm))
    }

    fun getDeletedUserIds(context: Context): Set<Long> = getLongSet(context, KEY_DELETED_USER_IDS)
    fun getDeletedUsernames(context: Context): Set<String> = getStringSetCopy(context, KEY_DELETED_USERNAMES)
    fun getEditedUserIds(context: Context): Set<Long> = getLongSet(context, KEY_EDITED_USER_IDS)
    fun getEditedUsernames(context: Context): Set<String> = getStringSetCopy(context, KEY_EDITED_USERNAMES)

    // --- Expenses ---
    fun recordExpenseDeleted(context: Context, id: Long, title: String, timestamp: Long) {
        if (id > 0L) addToStringSet(context, KEY_DELETED_EXPENSE_IDS, listOf(id.toString()))
        if (title.isNotBlank()) {
            addToStringSet(context, KEY_DELETED_EXPENSE_FPS, listOf(expenseFingerprint(title, timestamp)))
        }
    }

    fun recordExpenseEdited(context: Context, id: Long) {
        if (id > 0L) addToStringSet(context, KEY_EDITED_EXPENSE_IDS, listOf(id.toString()))
    }

    fun getDeletedExpenseIds(context: Context): Set<Long> = getLongSet(context, KEY_DELETED_EXPENSE_IDS)
    fun getDeletedExpenseFingerprints(context: Context): Set<String> = getStringSetCopy(context, KEY_DELETED_EXPENSE_FPS)
    fun getEditedExpenseIds(context: Context): Set<Long> = getLongSet(context, KEY_EDITED_EXPENSE_IDS)

    // --- Transactions ---
    fun recordTransactionDeleted(context: Context, id: Long, invoiceNumber: String) {
        if (id > 0L) addToStringSet(context, KEY_DELETED_TRANSACTION_IDS, listOf(id.toString()))
        val normInv = normalize(invoiceNumber)
        if (normInv.isNotBlank()) addToStringSet(context, KEY_DELETED_TRANSACTION_INVOICES, listOf(normInv))
    }

    fun recordTransactionsDeleted(context: Context, entries: List<Pair<Long, String>>) {
        if (entries.isEmpty()) return
        val ids = entries.mapNotNull { if (it.first > 0L) it.first.toString() else null }
        val invoices = entries.mapNotNull { val n = normalize(it.second); if (n.isNotBlank()) n else null }
        addToStringSet(context, KEY_DELETED_TRANSACTION_IDS, ids)
        addToStringSet(context, KEY_DELETED_TRANSACTION_INVOICES, invoices)
    }

    fun getDeletedTransactionIds(context: Context): Set<Long> = getLongSet(context, KEY_DELETED_TRANSACTION_IDS)
    fun getDeletedTransactionInvoices(context: Context): Set<String> = getStringSetCopy(context, KEY_DELETED_TRANSACTION_INVOICES)

    // --- Daily Reports ---
    fun recordDailyReportDeleted(context: Context, id: Long, date: String) {
        if (id > 0L) addToStringSet(context, KEY_DELETED_DAILY_REPORT_IDS, listOf(id.toString()))
        val cleanDate = date.trim()
        if (cleanDate.isNotBlank()) addToStringSet(context, KEY_DELETED_DAILY_REPORT_DATES, listOf(cleanDate))
    }

    fun getDeletedDailyReportIds(context: Context): Set<Long> = getLongSet(context, KEY_DELETED_DAILY_REPORT_IDS)
    fun getDeletedDailyReportDates(context: Context): Set<String> = getStringSetCopy(context, KEY_DELETED_DAILY_REPORT_DATES)

    // --- Categories & Units ---
    fun recordCategoryAdded(context: Context, name: String) {
        val norm = normalize(name)
        if (norm.isNotBlank()) {
            removeFromStringSet(context, KEY_DELETED_CATEGORIES, norm)
        }
        markSettingEdited(context, SETTING_CUSTOM_CATEGORIES)
    }

    fun recordCategoryRenamed(context: Context, oldName: String, newName: String) {
        val normOld = normalize(oldName)
        val normNew = normalize(newName)
        val cleanNew = newName.trim()
        if (normOld.isBlank() || cleanNew.isBlank()) return
        synchronized(lock) {
            val p = prefs(context)
            val mapJson = try {
                JSONObject(p.getString(KEY_RENAMED_CATEGORIES_JSON, "{}") ?: "{}")
            } catch (_: Throwable) {
                JSONObject()
            }
            if (normOld != normNew) {
                mapJson.put(normOld, cleanNew)
            }
            p.edit().putString(KEY_RENAMED_CATEGORIES_JSON, mapJson.toString()).apply()
        }
        if (normOld != normNew) {
            addToStringSet(context, KEY_DELETED_CATEGORIES, listOf(normOld))
        }
        removeFromStringSet(context, KEY_DELETED_CATEGORIES, normNew)
        markSettingEdited(context, SETTING_CUSTOM_CATEGORIES)
    }

    fun recordCategoryDeleted(context: Context, name: String) {
        val norm = normalize(name)
        if (norm.isNotBlank()) {
            addToStringSet(context, KEY_DELETED_CATEGORIES, listOf(norm))
        }
        markSettingEdited(context, SETTING_CUSTOM_CATEGORIES)
    }

    fun recordUnitAdded(context: Context, name: String) {
        val norm = normalize(name)
        if (norm.isNotBlank()) {
            removeFromStringSet(context, KEY_DELETED_UNITS, norm)
        }
        markSettingEdited(context, SETTING_CUSTOM_UNITS)
    }

    fun recordUnitDeleted(context: Context, name: String) {
        val norm = normalize(name)
        if (norm.isNotBlank()) {
            addToStringSet(context, KEY_DELETED_UNITS, listOf(norm))
        }
        markSettingEdited(context, SETTING_CUSTOM_UNITS)
    }

    fun getDeletedCategories(context: Context): Set<String> = getStringSetCopy(context, KEY_DELETED_CATEGORIES)
    fun getDeletedUnits(context: Context): Set<String> = getStringSetCopy(context, KEY_DELETED_UNITS)

    fun getRenamedCategories(context: Context): Map<String, String> {
        synchronized(lock) {
            val raw = prefs(context).getString(KEY_RENAMED_CATEGORIES_JSON, "{}") ?: "{}"
            return try {
                val obj = JSONObject(raw)
                val out = mutableMapOf<String, String>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = obj.optString(k).trim()
                    if (k.isNotBlank() && v.isNotBlank()) {
                        out[normalize(k)] = v
                    }
                }
                out
            } catch (_: Throwable) {
                emptyMap()
            }
        }
    }

    // --- Granular Settings ---
    fun markSettingEdited(context: Context, settingKey: String) {
        if (settingKey.isNotBlank()) {
            addToStringSet(context, KEY_EDITED_SETTINGS, listOf(settingKey))
        }
    }

    fun markSettingsEdited(context: Context, vararg settingKeys: String) {
        addToStringSet(context, KEY_EDITED_SETTINGS, settingKeys.toList())
    }

    fun isSettingEdited(context: Context, settingKey: String): Boolean {
        return getStringSetCopy(context, KEY_EDITED_SETTINGS).contains(settingKey)
    }

    fun clearAll(context: Context) {
        synchronized(lock) {
            prefs(context).edit().clear().commit()
        }
    }
}
