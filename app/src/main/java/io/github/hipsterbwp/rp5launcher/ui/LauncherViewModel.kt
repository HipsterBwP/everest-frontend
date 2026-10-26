package io.github.hipsterbwp.rp5launcher.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hipsterbwp.rp5launcher.data.CommandDef
import io.github.hipsterbwp.rp5launcher.data.ConfigStore
import io.github.hipsterbwp.rp5launcher.data.EsdeCatalog
import io.github.hipsterbwp.rp5launcher.data.Game
import io.github.hipsterbwp.rp5launcher.data.GameStats
import io.github.hipsterbwp.rp5launcher.data.LauncherConfig
import io.github.hipsterbwp.rp5launcher.data.LibraryCache
import io.github.hipsterbwp.rp5launcher.data.LibraryScanner
import io.github.hipsterbwp.rp5launcher.data.LibraryStore
import io.github.hipsterbwp.rp5launcher.data.StatsFile
import io.github.hipsterbwp.rp5launcher.data.SystemDef
import io.github.hipsterbwp.rp5launcher.data.SystemInfo
import io.github.hipsterbwp.rp5launcher.data.SystemMeta
import io.github.hipsterbwp.rp5launcher.launch.GameLauncher
import io.github.hipsterbwp.rp5launcher.launch.LaunchProblem
import io.github.hipsterbwp.rp5launcher.launch.LaunchResult
import io.github.hipsterbwp.rp5launcher.ui.theme.SystemColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data class Setup(val step: Int) : Screen
    data object Home : Screen
    data class Games(val system: String) : Screen
    data class Details(val system: String, val gameId: String) : Screen
    data object Settings : Screen
}

sealed interface Dialog {
    data class EmulatorPicker(val system: String) : Dialog
    data class LaunchError(val system: String, val game: Game?, val problem: LaunchProblem) : Dialog
    data class Message(val title: String, val body: String) : Dialog
}

data class SystemEntry(
    val def: SystemDef,
    val info: SystemInfo,
    val color: Color,
    val folderName: String,
    val games: List<Game>,
) {
    val name: String get() = def.name
}

data class ScanProgress(val done: Int, val total: Int)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val configStore = ConfigStore(app)
    private val libraryStore = LibraryStore(app)
    private val launcher = GameLauncher(app)
    private var catalog: EsdeCatalog? = null

    var ready by mutableStateOf(false)
        private set
    var config by mutableStateOf(LauncherConfig.defaults)
        private set
    var library by mutableStateOf<LibraryCache?>(null)
        private set
    var systems by mutableStateOf<List<SystemEntry>>(emptyList())
        private set
    var stats by mutableStateOf(StatsFile())
        private set
    var scan by mutableStateOf<ScanProgress?>(null)
        private set

    /** Bumped when installed emulators may have changed, so status labels recompute. */
    var emulatorsVersion by mutableIntStateOf(0)
        private set

    val backStack = mutableStateListOf<Screen>()
    val screen: Screen? get() = backStack.lastOrNull()
    var dialog by mutableStateOf<Dialog?>(null)

    // Per-screen selection memory, so going back lands where you were.
    var homeTab by mutableIntStateOf(0)
    var homeSystemIndex by mutableIntStateOf(0)
    val homeGameIndex = mutableStateMapOf<Int, Int>()
    val gridIndex = mutableStateMapOf<String, Int>()
    val gridFilter = mutableStateMapOf<String, Int>()
    var setupIndex by mutableIntStateOf(0)

    val configPath: String get() = configStore.file.absolutePath

    private var scanJob: Job? = null
    private var session: Pair<String, Long>? = null

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val catalog = EsdeCatalog.load(getApplication<Application>().assets)
                val (config, configProblem) = loadConfig()
                Loaded(catalog, config, configProblem, libraryStore.loadLibrary(), libraryStore.loadStats())
            }
            catalog = loaded.catalog
            applyConfig(loaded.config)
            stats = loaded.stats
            library = loaded.library?.takeIf { it.rootUri == loaded.config.romsRoot }
            rebuildSystems()
            val root = rootUri()
            if (root == null || !hasAccess(root)) {
                backStack += Screen.Setup(1)
            } else {
                backStack += Screen.Home
                rescan()
            }
            ready = true
            loaded.configProblem?.let { dialog = Dialog.Message("Config file has an error", it) }
        }
    }

    private data class Loaded(
        val catalog: EsdeCatalog,
        val config: LauncherConfig,
        val configProblem: String?,
        val library: LibraryCache?,
        val stats: StatsFile,
    )

    private fun loadConfig(): Pair<LauncherConfig, String?> = try {
        val existed = configStore.file.exists()
        val config = configStore.load()
        if (!existed) configStore.save(config)
        config to null
    } catch (e: Exception) {
        val previous = config
        previous to "${configStore.file.name} couldn't be read, so the launcher is using its last settings.\n\n" +
            "${e.message?.lineSequence()?.firstOrNull() ?: e.javaClass.simpleName}\n\n" +
            "Fix the file, then use Settings › About › Reload config file."
    }

    private fun applyConfig(newConfig: LauncherConfig) {
        config = newConfig
        val rules = LinkedHashMap(catalog?.emulators.orEmpty())
        rules.putAll(newConfig.emulators)
        launcher.findRules = rules
        launcher.clearCache()
        emulatorsVersion++
    }

    private fun updateConfig(update: (LauncherConfig) -> LauncherConfig) {
        val updated = update(config)
        applyConfig(updated)
        rebuildSystems()
        viewModelScope.launch(Dispatchers.IO) { configStore.save(updated) }
    }

    fun reloadConfig() {
        viewModelScope.launch {
            val (loaded, problem) = withContext(Dispatchers.IO) { loadConfig() }
            if (problem != null) {
                dialog = Dialog.Message("Config file has an error", problem)
                return@launch
            }
            val rootChanged = loaded.romsRoot != config.romsRoot
            applyConfig(loaded)
            rebuildSystems()
            if (rootChanged) rescan()
            dialog = Dialog.Message("Config reloaded", "Settings from ${configStore.file.name} are now in use.")
        }
    }

    private fun rebuildSystems() {
        val catalog = catalog ?: return
        val colors = SystemColors.assign(library?.systems.orEmpty().map { it.name })
        systems = library?.systems.orEmpty().mapNotNull { scanned ->
            val def = catalog.system(scanned.name) ?: return@mapNotNull null
            SystemEntry(
                def = def,
                info = SystemMeta.info(def),
                color = parseColor(config.system(def.name).color) ?: colors.getValue(def.name),
                folderName = scanned.folderName,
                games = scanned.games,
            )
        }
    }

    private fun parseColor(hex: String?): Color? = hex?.removePrefix("#")?.takeIf { it.length == 6 }
        ?.toLongOrNull(16)?.let { Color(0xFF000000 or it) }

    // -----------------------------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------------------------

    fun push(screen: Screen) {
        backStack += screen
    }

    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    private fun resetTo(screen: Screen) {
        backStack.clear()
        backStack += screen
    }

    fun finishSetup() = resetTo(Screen.Home)

    // -----------------------------------------------------------------------------------------
    // Library
    // -----------------------------------------------------------------------------------------

    fun rootUri(): Uri? = config.romsRoot?.let(Uri::parse)

    private fun hasAccess(uri: Uri): Boolean =
        getApplication<Application>().contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }

    /** Human-readable ROMs folder, e.g. "SD card / ROMs". */
    fun rootLabel(): String? {
        val uri = rootUri() ?: return null
        val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return uri.toString()
        val volume = docId.substringBefore(':', "")
        val path = docId.substringAfter(':').replace("/", " / ")
        val place = when (volume) {
            "primary" -> "Internal storage"
            "" -> "Folder"
            else -> "SD card"
        }
        return if (path.isEmpty()) place else "$place / $path"
    }

    fun onFolderPicked(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { resolver.takePersistableUriPermission(uri, flags) }
            .recoverCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val old = config.romsRoot
        if (old != null && old != uri.toString()) {
            runCatching { resolver.releasePersistableUriPermission(Uri.parse(old), flags) }
        }
        updateConfig { it.copy(romsRoot = uri.toString()) }
        library = null
        rebuildSystems()
        if (screen is Screen.Setup) {
            backStack[backStack.lastIndex] = Screen.Setup(2)
            setupIndex = 0
        }
        rescan()
    }

    fun rescan() {
        val root = rootUri() ?: return
        val catalog = catalog ?: return
        scanJob?.cancel()
        scan = ScanProgress(0, 0)
        scanJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    LibraryScanner(getApplication<Application>().contentResolver, catalog).scan(root) { done, total ->
                        viewModelScope.launch { scan = ScanProgress(done, total) }
                    }.also(libraryStore::saveLibrary)
                }
            }
            scan = null
            result.onSuccess {
                library = it
                rebuildSystems()
                homeSystemIndex = homeSystemIndex.coerceIn(0, (systems.size - 1).coerceAtLeast(0))
            }.onFailure {
                dialog = Dialog.Message(
                    "Couldn't read your ROMs folder",
                    "${it.message ?: it.javaClass.simpleName}\n\nPick the folder again in Settings › Library.",
                )
            }
        }
    }

    fun system(name: String): SystemEntry? = systems.firstOrNull { it.name == name }

    fun game(system: String, id: String): Game? = system(system)?.games?.firstOrNull { it.id == id }

    fun stats(game: Game): GameStats = stats.games[game.id] ?: GameStats()

    fun favorites(): List<Pair<SystemEntry, Game>> =
        systems.flatMap { s -> s.games.filter { stats(it).favorite }.map { s to it } }
            .sortedBy { it.second.title.lowercase() }

    fun recent(): List<Pair<SystemEntry, Game>> =
        systems.flatMap { s -> s.games.filter { stats(it).lastPlayed > 0 }.map { s to it } }
            .sortedByDescending { stats(it.second).lastPlayed }

    fun lastPlayed(system: SystemEntry): Game? =
        system.games.filter { stats(it).lastPlayed > 0 }.maxByOrNull { stats(it).lastPlayed }

    private fun updateStats(game: Game, update: (GameStats) -> GameStats) {
        val updated = StatsFile(stats.games + (game.id to update(stats(game))))
        stats = updated
        viewModelScope.launch(Dispatchers.IO) { libraryStore.saveStats(updated) }
    }

    fun toggleFavorite(game: Game) = updateStats(game) { it.copy(favorite = !it.favorite) }

    // -----------------------------------------------------------------------------------------
    // Emulators
    // -----------------------------------------------------------------------------------------

    fun commands(system: String): List<CommandDef> {
        val def = catalog?.system(system) ?: return emptyList()
        return def.commands + config.system(system).commands.map { CommandDef(it.label, it.command) }
    }

    /** The command a system launches with: the configured one, else the first installed, else the first. */
    fun selectedCommand(system: String): CommandDef? {
        val all = commands(system)
        val chosen = config.system(system).emulator
        return all.firstOrNull { it.label == chosen }
            ?: all.firstOrNull { launcher.isInstalled(it) }
            ?: all.firstOrNull()
    }

    fun isInstalled(command: CommandDef): Boolean = launcher.isInstalled(command)

    fun chooseEmulator(system: String, command: CommandDef) =
        updateConfig { cfg -> cfg.withSystem(system) { it.copy(emulator = command.label) } }

    fun redetectEmulators() {
        launcher.clearCache()
        emulatorsVersion++
    }

    /** Opens the store page for an emulator package. */
    fun openStore(packageName: String) {
        val app = getApplication<Application>()
        val storePackage = if (packageName.startsWith("com.retroarch")) "com.retroarch" else packageName
        val intents = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$storePackage")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$storePackage")),
        )
        val opened = intents.any { intent ->
            runCatching { app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        }
        if (!opened) {
            dialog = Dialog.Message("No app store found", "Install the emulator ($packageName) manually, then come back.")
        }
    }

    fun suggestedPackage(command: CommandDef): String? = launcher.suggestedPackage(command)

    // -----------------------------------------------------------------------------------------
    // Launching
    // -----------------------------------------------------------------------------------------

    fun play(system: String, game: Game) {
        val root = rootUri()
        val command = selectedCommand(system)
        val result = when {
            root == null -> LaunchResult.Failed(
                LaunchProblem("No ROMs folder", "The launcher doesn't know where your games are.", "Pick a folder in Settings › Library."),
            )
            command == null -> LaunchResult.Failed(
                LaunchProblem(
                    "No emulator for this system",
                    "ES-DE has no Android launch command for \"$system\".",
                    "Add one under \"systems\" › \"$system\" › \"commands\" in launcher-config.json.",
                ),
            )
            // A failed launch must explain itself, never crash the launcher.
            else -> try {
                launcher.launch(game, command, root)
            } catch (e: Exception) {
                LaunchResult.Failed(
                    LaunchProblem(
                        "${command.shortName} couldn't be started",
                        e.message ?: e.javaClass.simpleName,
                        "Try another emulator for this system (X on the game page).",
                    ),
                )
            }
        }
        when (result) {
            LaunchResult.Started -> {
                val now = System.currentTimeMillis()
                session = game.id to now
                updateStats(game) { it.copy(lastPlayed = now, playCount = it.playCount + 1) }
            }
            is LaunchResult.Failed -> dialog = Dialog.LaunchError(system, game, result.problem)
        }
    }

    /** Test-launch from settings: the system's first game. */
    fun testLaunch(system: String) {
        val entry = system(system) ?: return
        entry.games.firstOrNull()?.let { play(system, it) }
    }

    /** Called when the launcher comes back to the foreground. */
    fun onResumed() {
        session?.let { (id, start) ->
            val elapsed = (System.currentTimeMillis() - start).coerceIn(0, MAX_SESSION_MS)
            val game = systems.firstNotNullOfOrNull { s -> s.games.firstOrNull { it.id == id } }
            if (game != null) updateStats(game) { it.copy(playtimeMs = it.playtimeMs + elapsed) }
        }
        session = null
        redetectEmulators()
    }

    companion object {
        private const val MAX_SESSION_MS = 12L * 60 * 60 * 1000
    }
}
