package io.github.hipsterbwp.everest.data

import android.content.res.AssetManager
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Systems and emulator launch rules read from ES-DE's Android configuration files, bundled
 * unmodified in assets/esde/ (MIT, see assets/esde/LICENSE-ES-DE.txt).
 *
 * es_systems.xml gives each system its folder, file extensions and launch commands;
 * es_find_rules.xml maps emulator names used in those commands to package/activity pairs.
 */
class EsdeCatalog(
    val systems: List<SystemDef>,
    val emulators: Map<String, List<String>>,
) {
    private val byFolder = systems.associateBy { it.folder.lowercase() }
    private val byName = systems.associateBy { it.name }

    fun system(name: String): SystemDef? = byName[name]

    /** Matches a ROMs subfolder name to a system, accepting a few common alternative names. */
    fun systemForFolder(folder: String): SystemDef? {
        val key = folder.lowercase()
        return byFolder[key] ?: SystemMeta.folderAliases[key]?.let { byName[it] }
    }

    companion object {
        fun load(assets: AssetManager): EsdeCatalog {
            val systems = assets.open("esde/es_systems.xml").use(::parseSystems)
            val emulators = assets.open("esde/es_find_rules.xml").use(::parseFindRules)
            return EsdeCatalog(systems, emulators)
        }

        private fun parser(input: InputStream): XmlPullParser =
            Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(input, null)
            }

        private fun parseSystems(input: InputStream): List<SystemDef> {
            val p = parser(input)
            val out = ArrayList<SystemDef>()
            var name = ""
            var fullName = ""
            var path = ""
            var extensions = ""
            var commands = ArrayList<CommandDef>()
            var commandLabel: String? = null
            var text = StringBuilder()
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                when (p.eventType) {
                    XmlPullParser.START_TAG -> {
                        text = StringBuilder()
                        when (p.name) {
                            "system" -> {
                                name = ""; fullName = ""; path = ""; extensions = ""
                                commands = ArrayList()
                            }
                            "command" -> commandLabel = p.getAttributeValue(null, "label")
                        }
                    }
                    XmlPullParser.TEXT -> text.append(p.text)
                    XmlPullParser.END_TAG -> {
                        val value = text.toString().trim()
                        when (p.name) {
                            "name" -> name = value
                            "fullname" -> fullName = value
                            "path" -> path = value
                            "extension" -> extensions = value
                            "command" -> commands += CommandDef(commandLabel ?: "Default", value)
                            "system" -> if (name.isNotEmpty()) {
                                out += SystemDef(
                                    name = name,
                                    fullName = fullName,
                                    folder = path.substringAfterLast('/'),
                                    extensions = extensions.split(' ')
                                        .filter { it.isNotBlank() }
                                        .map { it.trimStart('.').lowercase() }
                                        .toSet(),
                                    commands = commands,
                                )
                            }
                        }
                    }
                }
            }
            return out
        }

        private fun parseFindRules(input: InputStream): Map<String, List<String>> {
            val p = parser(input)
            val out = LinkedHashMap<String, MutableList<String>>()
            var emulator: String? = null
            var inPackageRule = false
            var text = StringBuilder()
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                when (p.eventType) {
                    XmlPullParser.START_TAG -> {
                        text = StringBuilder()
                        when (p.name) {
                            "emulator" -> emulator = p.getAttributeValue(null, "name")
                            "rule" -> inPackageRule = p.getAttributeValue(null, "type") == "androidpackage"
                        }
                    }
                    XmlPullParser.TEXT -> text.append(p.text)
                    XmlPullParser.END_TAG -> when (p.name) {
                        "entry" -> if (inPackageRule && emulator != null) {
                            out.getOrPut(emulator) { ArrayList() } += text.toString().trim()
                        }
                        "rule" -> inPackageRule = false
                        "emulator" -> emulator = null
                    }
                }
            }
            return out
        }
    }
}

data class SystemDef(
    /** ES-DE system name, e.g. "snes". */
    val name: String,
    val fullName: String,
    /** ROMs subfolder name, e.g. "snes". */
    val folder: String,
    /** Lower-case file extensions without the dot. */
    val extensions: Set<String>,
    val commands: List<CommandDef>,
)

/** One launch option: a label like "Snes9x - Current" and an ES-DE command template. */
data class CommandDef(val label: String, val template: String) {
    /** The emulator name from the leading %EMULATOR_X% token, e.g. "RETROARCH". */
    val emulatorName: String? =
        EMULATOR_TOKEN.find(template)?.groupValues?.get(1)

    /** ES-DE's %ANDROIDAPP% commands open another Android app instead of an emulator. */
    val isAndroidApp: Boolean = template.trimStart().startsWith("%ANDROIDAPP%")

    /** Human label: "RetroArch · Snes9x - Current" or "NetherSX2". */
    val displayName: String
        get() = when (emulatorName) {
            "RETROARCH" -> "RetroArch · $label"
            else -> label.removeSuffix(" (Standalone)")
        }

    /** Core or app name for compact stats, e.g. "Snes9x - Current" / "NetherSX2". */
    val shortName: String get() = label.removeSuffix(" (Standalone)")

    /** "via RetroArch" or "standalone". */
    val hostName: String
        get() = when {
            emulatorName == "RETROARCH" -> "via RetroArch"
            isAndroidApp -> "Android app"
            else -> "standalone app"
        }

    companion object {
        private val EMULATOR_TOKEN = Regex("""^\s*%EMULATOR_([A-Za-z0-9_\-]+)%""")
    }
}
