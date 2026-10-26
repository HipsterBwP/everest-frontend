package io.github.hipsterbwp.rp5launcher.launch

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import io.github.hipsterbwp.rp5launcher.data.CommandDef
import io.github.hipsterbwp.rp5launcher.data.Game
import io.github.hipsterbwp.rp5launcher.data.LibraryScanner

/** An emulator app found on the device for a find-rule entry. */
data class InstalledEmulator(val packageName: String, val activity: String)

/** Why a launch couldn't happen, phrased for the player, plus what fixes it. */
data class LaunchProblem(
    val title: String,
    val detail: String,
    val fix: String,
    /** Package to open in the store when the fix is installing an emulator. */
    val installPackage: String? = null,
)

sealed interface LaunchResult {
    data object Started : LaunchResult
    data class Failed(val problem: LaunchProblem) : LaunchResult
}

/**
 * Turns an ES-DE Android command (see es_systems.xml) into an Intent and starts it.
 *
 * Supported tokens: %EMULATOR_X%, %ACTION%, %CATEGORY%, %DATA%, %MIMETYPE%, %EXTRA_k%,
 * %EXTRABOOL_k%, %EXTRAINTEGER_k%, %EXTRAARRAY_k%, %ACTIVITY_CLEAR_TASK%, %ACTIVITY_CLEAR_TOP%,
 * %ACTIVITY_NO_HISTORY%, and the value variables %ROM%, %ROMRAW%, %ROMSAF%, %ROMPROVIDER%,
 * %ROMPATH%, %ROMPATHRAW%, %GAMEDIR%, %GAMEDIRRAW%, %BASENAME%, %FILENAME%, %ANDROIDPACKAGE%,
 * %EXTERNALDATA%, %INTERNALDATA%.
 */
class GameLauncher(private val context: Context) {

    /** Emulator name -> find-rule entries ("package/activity"); ES-DE rules merged with config. */
    var findRules: Map<String, List<String>> = emptyMap()

    private val installedCache = HashMap<String, InstalledEmulator?>()

    fun clearCache() = installedCache.clear()

    fun findInstalled(emulatorName: String): InstalledEmulator? =
        installedCache.getOrPut(emulatorName) {
            findRules[emulatorName].orEmpty().firstNotNullOfOrNull(::resolveEntry)
        }

    fun isInstalled(command: CommandDef): Boolean =
        command.emulatorName?.let { findInstalled(it) } != null

    /** First package listed for an emulator, used for "get it" store links. */
    fun suggestedPackage(command: CommandDef): String? =
        command.emulatorName?.let { findRules[it]?.firstOrNull()?.substringBefore('/') }

    private fun resolveEntry(entry: String): InstalledEmulator? {
        val pkg = entry.substringBefore('/')
        val rawActivity = entry.substringAfter('/', "")
        if (pkg.isEmpty() || rawActivity.isEmpty()) return null
        val activity = if (rawActivity.startsWith(".")) pkg + rawActivity else rawActivity
        return try {
            context.packageManager.getActivityInfo(ComponentName(pkg, activity), 0)
            InstalledEmulator(pkg, activity)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun launch(game: Game, command: CommandDef, romsRoot: Uri): LaunchResult {
        val emulatorName = command.emulatorName
            ?: return fail(
                "This launch command isn't supported",
                "\"${command.label}\" doesn't start with an %EMULATOR_…% token.",
                "Pick another emulator for this system, or fix the command in launcher-config.json.",
            )
        val emulator = findInstalled(emulatorName)
            ?: return fail(
                "${command.shortName} isn't installed",
                "None of the apps ES-DE lists for $emulatorName were found on this device.",
                "Install it, or choose another emulator for this system.",
                installPackage = suggestedPackage(command),
            )

        val tokens = tokenize(command.template).drop(1) // first token is %EMULATOR_X%
        val vars = variables(game, emulator, romsRoot)
        val intent = Intent().setComponent(ComponentName(emulator.packageName, emulator.activity))
        intent.action = Intent.ACTION_MAIN
        var data: String? = null
        var mime: String? = null
        val grants = LinkedHashSet<Uri>()

        for (token in tokens) {
            val key = token.substringBefore('=')
            val hasValue = token.contains('=')
            val value = if (hasValue) expand(unquote(token.substringAfter('=')), vars) else ""
            if (value.startsWith("content://")) grants += Uri.parse(value)
            when {
                key == "%ACTION%" -> intent.action = value
                key == "%CATEGORY%" -> intent.addCategory(value)
                key == "%DATA%" -> data = value
                key == "%MIMETYPE%" -> mime = value
                key == "%ACTIVITY_CLEAR_TASK%" ->
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                key == "%ACTIVITY_CLEAR_TOP%" -> intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                key == "%ACTIVITY_NO_HISTORY%" -> intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                key.startsWith("%EXTRABOOL_") -> intent.putExtra(extraName(key, "%EXTRABOOL_"), value.toBoolean())
                key.startsWith("%EXTRAINTEGER_") -> {
                    val number = value.toIntOrNull() ?: return unsupported(command, token)
                    intent.putExtra(extraName(key, "%EXTRAINTEGER_"), number)
                }
                key.startsWith("%EXTRAARRAY_") ->
                    intent.putExtra(extraName(key, "%EXTRAARRAY_"), value.split(',').toTypedArray())
                key.startsWith("%EXTRA_") -> intent.putExtra(extraName(key, "%EXTRA_"), value)
                else -> return unsupported(command, token)
            }
        }

        if (data != null || mime != null) {
            intent.setDataAndType(data?.let(Uri::parse), mime)
        }
        if (grants.isNotEmpty()) {
            // Extras don't carry URI grants on their own; a ClipData does.
            val clip = ClipData.newRawUri("rom", grants.first())
            grants.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            intent.clipData = clip
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        val needsPath = command.template.contains("%ROM%") || command.template.contains("%ROMRAW%") ||
            command.template.contains("%GAMEDIR")
        if (needsPath && game.path == null) {
            return fail(
                "${command.shortName} needs a file path",
                "This emulator opens games by file path, but your ROMs folder is in a location without one.",
                "Pick a ROMs folder on internal storage or the SD card in Settings › Library.",
            )
        }

        return try {
            context.startActivity(intent)
            LaunchResult.Started
        } catch (e: ActivityNotFoundException) {
            fail(
                "${command.shortName} couldn't be opened",
                "Android couldn't find ${emulator.activity}. The app may have been updated or removed.",
                "Re-detect emulators in Settings › Emulators, or reinstall the emulator.",
            )
        } catch (e: SecurityException) {
            fail(
                "${command.shortName} refused to start",
                e.message ?: "The emulator doesn't allow other apps to open it this way.",
                "Choose another emulator for this system.",
            )
        }
    }

    private fun variables(game: Game, emulator: InstalledEmulator, romsRoot: Uri): Map<String, String> {
        val romPath = game.path.orEmpty()
        val rootPath = LibraryScanner.realPath(DocumentsContract.getTreeDocumentId(romsRoot)).orEmpty()
        val gameDir = romPath.substringBeforeLast('/', rootPath)
        return mapOf(
            "%ROM%" to romPath,
            "%ROMRAW%" to romPath,
            "%ROMSAF%" to game.id,
            "%ROMPROVIDER%" to game.id,
            "%ROMPATH%" to rootPath,
            "%ROMPATHRAW%" to rootPath,
            "%GAMEDIR%" to gameDir,
            "%GAMEDIRRAW%" to gameDir,
            "%BASENAME%" to game.fileName.substringBeforeLast('.'),
            "%FILENAME%" to game.fileName,
            "%ANDROIDPACKAGE%" to emulator.packageName,
            "%EXTERNALDATA%" to Environment.getExternalStorageDirectory().path,
            "%INTERNALDATA%" to "/data/data",
        )
    }

    private fun expand(value: String, vars: Map<String, String>): String =
        VARIABLE.replace(value) { m -> vars[m.value] ?: m.value }

    private fun extraName(key: String, prefix: String) = key.removePrefix(prefix).removeSuffix("%")

    private fun unquote(s: String) = if (s.length >= 2 && s.startsWith('"') && s.endsWith('"')) s.substring(1, s.length - 1) else s

    private fun unsupported(command: CommandDef, token: String) = fail(
        "This launch command isn't supported yet",
        "\"${command.label}\" uses ${token.substringBefore('=')}, which this launcher can't handle.",
        "Choose another emulator for this system.",
    )

    private fun fail(title: String, detail: String, fix: String, installPackage: String? = null) =
        LaunchResult.Failed(LaunchProblem(title, detail, fix, installPackage))

    companion object {
        private val VARIABLE = Regex("""%[A-Z]+%""")

        /** Splits on whitespace outside double quotes. */
        fun tokenize(command: String): List<String> {
            val out = ArrayList<String>()
            val cur = StringBuilder()
            var quoted = false
            for (ch in command.trim()) {
                when {
                    ch == '"' -> { quoted = !quoted; cur.append(ch) }
                    ch.isWhitespace() && !quoted -> if (cur.isNotEmpty()) { out += cur.toString(); cur.clear() }
                    else -> cur.append(ch)
                }
            }
            if (cur.isNotEmpty()) out += cur.toString()
            return out
        }
    }
}
