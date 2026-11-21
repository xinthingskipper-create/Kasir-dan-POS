package com.example.data.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class DriveBackupFile(
    val id: String,
    val name: String,
    val sizeBytes: Long = 0L,
    val modifiedTime: String = "",
    val createdTime: String = ""
)

data class DriveAuthResult(
    val accessToken: String? = null,
    val pendingIntent: PendingIntent? = null,
    val email: String = "",
    val needsResolution: Boolean = false
)

class GoogleDriveBackupManager(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private fun requestedScopes(): List<Scope> = listOf(
        Scope(SCOPE_DRIVE_FILE),
        Scope(SCOPE_USERINFO_EMAIL)
    )

    suspend fun requestAuthorization(): Result<DriveAuthResult> = withContext(Dispatchers.IO) {
        try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes())
                .build()

            val result: AuthorizationResult = Identity.getAuthorizationClient(context)
                .authorize(request)
                .await()

            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                return@withContext Result.success(
                    DriveAuthResult(
                        pendingIntent = pendingIntent,
                        needsResolution = true
                    )
                )
            }

            val token = result.accessToken
            if (token.isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Token akses Google Drive kosong")
                )
            }

            val email = fetchGoogleUserEmail(token)
            Result.success(
                DriveAuthResult(
                    accessToken = token,
                    email = email,
                    needsResolution = false
                )
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Google Drive authorization error: ${t.message}", t)
            Result.failure(t)
        }
    }

    suspend fun handleAuthorizationIntent(data: Intent?): Result<DriveAuthResult> = withContext(Dispatchers.IO) {
        try {
            if (data == null) {
                return@withContext Result.failure(IllegalStateException("Otorisasi Google Drive dibatalkan"))
            }
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (token.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("Gagal memperoleh token Google Drive"))
            }
            val email = fetchGoogleUserEmail(token)
            Result.success(
                DriveAuthResult(
                    accessToken = token,
                    email = email,
                    needsResolution = false
                )
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Failed handling authorization intent: ${t.message}", t)
            Result.failure(t)
        }
    }

    suspend fun getSilentAccessToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes())
                .build()
            val result = Identity.getAuthorizationClient(context)
                .authorize(request)
                .await()

            if (result.hasResolution() || result.accessToken.isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Perlu hubungkan ulang akun Google Drive di Pengaturan Toko")
                )
            }
            Result.success(result.accessToken!!)
        } catch (t: Throwable) {
            Log.w(TAG, "Silent Google Drive token fetch failed: ${t.message}")
            Result.failure(t)
        }
    }

    suspend fun disconnectAccount() = withContext(Dispatchers.IO) {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (t: Throwable) {
            Log.w(TAG, "clearCredentialState note: ${t.message}")
        }
    }

    suspend fun fetchGoogleUserEmail(accessToken: String): String = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://www.googleapis.com/oauth2/v3/userinfo")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext ""
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext ""
                JSONObject(body).optString("email", "")
            }
        } catch (_: Throwable) {
            ""
        }
    }

    suspend fun findOrCreateBackupFolder(accessToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val query = "name='$BACKUP_FOLDER_NAME' and mimeType='$FOLDER_MIME_TYPE' and trashed=false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&spaces=drive&fields=files(id,name)&pageSize=5"

            val searchReq = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(searchReq).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Gagal memeriksa folder Drive (${resp.code}): ${extractApiError(rawBody)}")
                    )
                }
                val json = JSONObject(rawBody)
                val files = json.optJSONArray("files") ?: JSONArray()
                if (files.length() > 0) {
                    val existingId = files.getJSONObject(0).optString("id")
                    if (existingId.isNotBlank()) {
                        return@withContext Result.success(existingId)
                    }
                }
            }

            // Create new folder
            val metadataJson = JSONObject().apply {
                put("name", BACKUP_FOLDER_NAME)
                put("mimeType", FOLDER_MIME_TYPE)
                put("description", "Folder cadangan otomatis pengaturan & transaksi Kasir POS")
            }.toString()

            val createReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files?fields=id,name")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(metadataJson.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(createReq).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Gagal membuat folder Drive (${resp.code}): ${extractApiError(rawBody)}")
                    )
                }
                val createdId = JSONObject(rawBody).optString("id")
                if (createdId.isBlank()) {
                    Result.failure(IllegalStateException("ID folder Google Drive kosong"))
                } else {
                    Result.success(createdId)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "findOrCreateBackupFolder failed: ${t.message}", t)
            Result.failure(t)
        }
    }

    suspend fun uploadBackupJson(
        accessToken: String,
        fileName: String,
        jsonContent: String,
        keepCount: Int = 10
    ): Result<DriveBackupFile> = withContext(Dispatchers.IO) {
        try {
            val folderResult = findOrCreateBackupFolder(accessToken)
            val folderId = folderResult.getOrElse { return@withContext Result.failure(it) }

            val boundary = "kasirpos_drive_boundary_${System.currentTimeMillis()}"
            val metadataObj = JSONObject().apply {
                put("name", fileName)
                put("parents", JSONArray().apply { put(folderId) })
                put("mimeType", "application/json")
                put("description", "Auto backup pengaturan toko & transaksi Kasir POS")
            }

            val multipartBody = buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadataObj.toString()).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(jsonContent).append("\r\n")
                append("--").append(boundary).append("--\r\n")
            }

            val mediaType = "multipart/related; boundary=$boundary".toMediaType()
            val uploadReq = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,size,modifiedTime,createdTime")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(multipartBody.toRequestBody(mediaType))
                .build()

            val uploadedFile = client.newCall(uploadReq).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Gagal upload ke Google Drive (${resp.code}): ${extractApiError(rawBody)}")
                    )
                }
                val obj = JSONObject(rawBody)
                DriveBackupFile(
                    id = obj.optString("id"),
                    name = obj.optString("name", fileName),
                    sizeBytes = obj.optString("size", "0").toLongOrNull()
                        ?: jsonContent.toByteArray(Charsets.UTF_8).size.toLong(),
                    modifiedTime = obj.optString("modifiedTime", ""),
                    createdTime = obj.optString("createdTime", "")
                )
            }

            if (keepCount > 0) {
                pruneExcessBackups(accessToken, folderId, keepCount)
            }

            Result.success(uploadedFile)
        } catch (t: Throwable) {
            Log.e(TAG, "uploadBackupJson failed: ${t.message}", t)
            Result.failure(t)
        }
    }

    suspend fun listBackupFiles(accessToken: String): Result<List<DriveBackupFile>> = withContext(Dispatchers.IO) {
        try {
            val folderResult = findOrCreateBackupFolder(accessToken)
            val folderId = folderResult.getOrElse { return@withContext Result.failure(it) }

            val query = "'$folderId' in parents and trashed=false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val fields = URLEncoder.encode("files(id,name,size,modifiedTime,createdTime)", "UTF-8")
            val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&orderBy=createdTime%20desc&pageSize=30&fields=$fields"

            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Gagal memuat daftar backup Drive (${resp.code}): ${extractApiError(rawBody)}")
                    )
                }
                val filesArr = JSONObject(rawBody).optJSONArray("files") ?: JSONArray()
                val result = mutableListOf<DriveBackupFile>()
                for (i in 0 until filesArr.length()) {
                    val obj = filesArr.getJSONObject(i)
                    result.add(
                        DriveBackupFile(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            sizeBytes = obj.optString("size", "0").toLongOrNull() ?: 0L,
                            modifiedTime = obj.optString("modifiedTime", ""),
                            createdTime = obj.optString("createdTime", "")
                        )
                    )
                }
                Result.success(result)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "listBackupFiles failed: ${t.message}", t)
            Result.failure(t)
        }
    }

    suspend fun downloadBackupJson(accessToken: String, fileId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Gagal mengunduh file Drive (${resp.code}): ${extractApiError(rawBody)}")
                    )
                }
                if (rawBody.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Isi file backup Google Drive kosong"))
                }
                Result.success(rawBody)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "downloadBackupJson failed: ${t.message}", t)
            Result.failure(t)
        }
    }

    private fun pruneExcessBackups(accessToken: String, folderId: String, keepCount: Int) {
        try {
            val query = "'$folderId' in parents and trashed=false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&orderBy=createdTime%20desc&pageSize=50&fields=files(id,name)"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return
                val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("files") ?: return
                if (arr.length() <= keepCount) return
                for (i in keepCount until arr.length()) {
                    val oldId = arr.getJSONObject(i).optString("id")
                    if (oldId.isNotBlank()) {
                        val delReq = Request.Builder()
                            .url("https://www.googleapis.com/drive/v3/files/$oldId")
                            .addHeader("Authorization", "Bearer $accessToken")
                            .delete()
                            .build()
                        try {
                            client.newCall(delReq).execute().close()
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Prune excess Drive backups warning: ${t.message}")
        }
    }

    private fun extractApiError(rawBody: String): String {
        return try {
            val json = JSONObject(rawBody)
            json.optJSONObject("error")?.optString("message")?.ifBlank { rawBody } ?: rawBody
        } catch (_: Throwable) {
            rawBody.take(160).ifBlank { "Kesalahan jaringan" }
        }
    }

    companion object {
        private const val TAG = "GoogleDriveBackupMgr"
        const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        const val SCOPE_USERINFO_EMAIL = "https://www.googleapis.com/auth/userinfo.email"
        const val BACKUP_FOLDER_NAME = "Kasir POS Backup"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()

        fun isWifiConnected(context: Context): Boolean {
            return try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } catch (_: Throwable) {
                false
            }
        }

        fun isNetworkAvailable(context: Context): Boolean {
            return try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } catch (_: Throwable) {
                false
            }
        }
    }
}
