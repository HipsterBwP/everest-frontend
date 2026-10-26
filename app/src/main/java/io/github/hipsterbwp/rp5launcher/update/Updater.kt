package io.github.hipsterbwp.rp5launcher.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.github.hipsterbwp.rp5launcher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A newer build published as a GitHub release by CI (tag "build-<versionCode>"). */
data class UpdateInfo(
    val versionCode: Int,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
)

class UpdateException(message: String) : Exception(message)

/**
 * Self-update from this repository's GitHub releases. Android always shows its own install
 * confirmation unless the launcher installed itself last time (Android 12+ skips it then).
 */
class Updater(private val context: Context) {

    val currentVersion: Int get() = BuildConfig.VERSION_CODE

    /** Returns the newer release, or null when up to date. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val conn = open(LATEST_RELEASE_URL).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            when (val code = conn.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> throw UpdateException(
                    "No builds are published yet, or the GitHub repository isn't public.",
                )
                else -> throw UpdateException("GitHub answered $code. Try again later.")
            }
            val release = Json.parseToJsonElement(conn.inputStream.bufferedReader().use { it.readText() }).jsonObject
            val tag = release["tag_name"]?.jsonPrimitive?.content.orEmpty()
            val version = tag.removePrefix(TAG_PREFIX).toIntOrNull() ?: return@withContext null
            if (version <= currentVersion) return@withContext null
            val apk = release["assets"]?.jsonArray
                ?.map { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true }
                ?: throw UpdateException("Build $version has no APK attached.")
            UpdateInfo(
                versionCode = version,
                title = release["name"]?.jsonPrimitive?.content ?: "Build $version",
                notes = release["body"]?.jsonPrimitive?.content.orEmpty().trim(),
                apkUrl = apk["browser_download_url"]!!.jsonPrimitive.content,
                sizeBytes = apk["size"]?.jsonPrimitive?.long ?: 0,
            )
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads the APK into the cache, reporting progress 0..1. */
    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "rp5-launcher-${info.versionCode}.apk")
        val conn = open(info.apkUrl)
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw UpdateException("Download failed (HTTP ${conn.responseCode}).")
            }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.sizeBytes
            conn.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            file
        } catch (e: IOException) {
            file.delete()
            throw UpdateException("Download interrupted: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            conn.disconnect()
        }
    }

    /** Whether Android lets this app install packages ("Install unknown apps" for the launcher). */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Hands the APK to Android's package installer. The result arrives in [UpdateReceiver]. */
    suspend fun install(file: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val callback = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, UpdateReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(callback.intentSender)
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "rp5-launcher/${BuildConfig.VERSION_NAME}")
        }

    companion object {
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/HipsterBwP/rp5-launcher/releases/latest"
        private const val TAG_PREFIX = "build-"
        private const val TIMEOUT_MS = 15_000

        /** Installer failures, shown by the UI. Null when nothing went wrong. */
        val installError = MutableStateFlow<String?>(null)
    }
}

/** Receives PackageInstaller results: shows Android's confirmation screen, or reports a failure. */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            PackageInstaller.STATUS_SUCCESS -> Unit // Android stops the old version; reopening starts the new one.
            else -> Updater.installError.value =
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Install failed (status $status)."
        }
    }
}
