package io.github.hipsterbwp.rp5launcher.data

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The user-editable launcher config, stored as JSON at
 * Android/data/<package>/files/launcher-config.json (reachable over USB).
 *
 * Example:
 * {
 *   "systems": {
 *     "snes": { "emulator": "Snes9x - Current" },
 *     "ps2":  { "emulator": "NetherSX2 (Standalone)", "color": "#1F4BA5" },
 *     "gba":  { "commands": [ { "label": "My mGBA", "command": "%EMULATOR_RETROARCH% ..." } ] }
 *   },
 *   "emulators": { "RETROARCH": ["com.retroarch.aarch64/com.retroarch.browser.retroactivity.RetroActivityFuture"] }
 * }
 *
 * "emulator" is a command label from ES-DE's es_systems.xml (or one added under "commands").
 * "emulators" adds or overrides ES-DE find rules: emulator name -> "package/activity" entries.
 * "hiddenSystems" lists ES-DE system names to leave off the console list
 * (default: androidapps, androidgames, emulators).
 */
@Serializable
data class LauncherConfig(
    @SerialName("romsRoot") val romsRoot: String? = null,
    val systems: Map<String, SystemConfig> = emptyMap(),
    val emulators: Map<String, List<String>> = emptyMap(),
    /** Download box art and info for new games after each library scan. */
    val autoScrape: Boolean = false,
    /** Navigation sounds on/off and volume level 1 (low) to 3 (high). */
    val navigationSounds: Boolean = true,
    val soundVolume: Int = 2,
    /** Play gameplay videos with sound (muted by default). */
    val videoSound: Boolean = false,
    /** ES-DE system names left off the console list even when their folder has files. */
    val hiddenSystems: Set<String> = DEFAULT_HIDDEN_SYSTEMS,
) {
    fun system(name: String): SystemConfig = systems[name] ?: SystemConfig()

    fun withSystem(name: String, update: (SystemConfig) -> SystemConfig): LauncherConfig =
        copy(systems = systems + (name to update(system(name))))

    companion object {
        /** ES-DE's shortcut folders for Android apps aren't consoles. */
        val DEFAULT_HIDDEN_SYSTEMS = setOf("androidapps", "androidgames", "emulators")

        /** First-run defaults: the emulators this device's owner uses. */
        val defaults = LauncherConfig(
            systems = mapOf(
                "snes" to SystemConfig(emulator = "Snes9x - Current"),
                "gba" to SystemConfig(emulator = "mGBA"),
                "ps2" to SystemConfig(emulator = "NetherSX2 (Standalone)"),
            ),
        )
    }
}

@Serializable
data class SystemConfig(
    /** Chosen command label; null = first installed option. */
    val emulator: String? = null,
    /** "#RRGGBB" override for the system color. */
    val color: String? = null,
    /** Extra launch commands in ES-DE syntax, offered alongside ES-DE's own. */
    val commands: List<CommandConfig> = emptyList(),
)

@Serializable
data class CommandConfig(val label: String, val command: String)

class ConfigStore(context: Context) {
    val file: File = File(context.getExternalFilesDir(null) ?: context.filesDir, "launcher-config.json")

    /** Returns the config, or the defaults if the file is missing. Throws on malformed JSON. */
    fun load(): LauncherConfig =
        if (file.exists()) PrettyJson.decodeFromString(LauncherConfig.serializer(), file.readText())
        else LauncherConfig.defaults

    fun save(config: LauncherConfig) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(PrettyJson.encodeToString(LauncherConfig.serializer(), config))
        tmp.renameTo(file)
    }
}

internal val PrettyJson = Json {
    prettyPrint = true
    encodeDefaults = false
    ignoreUnknownKeys = true
}

internal val CompactJson = Json {
    ignoreUnknownKeys = true
}
