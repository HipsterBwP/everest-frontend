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
import io.github.hipsterbwp.rp5launcher.art.GameArt
import io.github.hipsterbwp.rp5launcher.art.ArtIndex
import io.github.hipsterbwp.rp5launcher.art.LogoStore
import io.github.hipsterbwp.rp5launcher.art.Scraper
import io.github.hipsterbwp.rp5launcher.art.ScreenScraper
import io.github.hipsterbwp.rp5launcher.art.ScreenScraperAccount
import io.github.hipsterbwp.rp5launcher.art.ScreenScraperSession
import io.github.hipsterbwp.rp5launcher.art.ScreenScraperStop
import io.github.hipsterbwp.rp5launcher.art.SystemArtStore
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
import io.github.hipsterbwp.rp5launcher.update.UpdateInfo
import io.github.hipsterbwp.rp5launcher.update.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    data object Update : Dialog
    /** On-screen-keyboard entry for one setting. */
    data class TextInput(val field: TextField, val title: String, val initial: String, val secret: Boolean) : Dialog
}

enum class TextField { SsUser, SsPassword, SsDevId, SsDevPassword }

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class NeedsPermission(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    data class Installing(val info: UpdateInfo) : UpdateState
    data class Failed(val message: String, val info: UpdateInfo?) : UpdateState
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

data class ScrapeProgress(val done: Int, val total: Int, val system: String)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val configStore = ConfigStore(app)
    private val libraryStore = LibraryStore(app)
    private val launcher = GameLauncher(app)
    private val updater = Updater(app)
    private val logoStore = LogoStore(app)
    private val scraper = Scraper(app)
    private val systemArtStore = SystemArtStore(app)
    private val ssAccount = ScreenScraperAccount(app)
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

    /** Scraped art and info per game id. */
    val art = mutableStateMapOf<String, GameArt>()
    /** Downloaded console logo masks per system. */
    val logos = mutableStateMapOf<String, File>()
    var scrape by mutableStateOf<ScrapeProgress?>(null)
        private set
    /** Console illustrations and fact sheets per system. */
    val systemImages = mutableStateMapOf<String, File>()
    val systemFacts = mutableStateMapOf<String, List<Pair<String, String>>>()

    var screenScraper by mutableStateOf(ScreenScraperAccount.Credentials("", "", "", ""))
        private set
    /** Last ScreenScraper message: login check result or why lookups stopped. */
    var screenScraperStatus by mutableStateOf<String?>(null)
        private set
    /** True while a text field has the keyboard, so the activity lets key presses through. */
    var textInputActive by mutableStateOf(false)
    private var scrapeJob: Job? = null

    var update by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set
    val currentVersion: Int get() = updater.currentVersion

    /** Set by the activity: plays a navigation sound if sounds are on. */
    var soundPlayer: ((UiSound) -> Unit)? = null

    fun sound(sound: UiSound) {
        if (config.navigationSounds) soundPlayer?.invoke(sound)
    }

    fun setNavigationSounds(enabled: Boolean) = updateConfig { it.copy(navigationSounds = enabled) }

    fun cycleSoundVolume() = updateConfig { it.copy(soundVolume = it.soundVolume % 3 + 1) }

    fun setVideoSound(enabled: Boolean) = updateConfig { it.copy(videoSound = enabled) }

    val configPath: String get() = configStore.file.absolutePath

    private var scanJob: Job? = null
    private var session: Pair<String, Long>? = null

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val catalog = EsdeCatalog.load(getApplication<Application>().assets)
                val (config, configProblem) = loadConfig()
                Loaded(
                    catalog, config, configProblem, libraryStore.loadLibrary(), libraryStore.loadStats(), scraper.load(),
                )
            }
            art.putAll(loaded.art.games)
            screenScraper = withContext(Dispatchers.IO) { ssAccount.load() }
            catalog = loaded.catalog
            applyConfig(loaded.config)
            stats = loaded.stats
            library = loaded.library?.takeIf { it.rootUri == loaded.config.romsRoot }
            rebuildSystems()
            refreshLogos()
            val root = rootUri()
            if (root == null || !hasAccess(root)) {
                backStack += Screen.Setup(1)
            } else {
                backStack += Screen.Home
                rescan()
            }
            ready = true
            loaded.configProblem?.let { dialog = Dialog.Message("Config file has an error", it) }
            checkForUpdate(userInitiated = false)
        }
        viewModelScope.launch {
            Updater.installError.collect { error ->
                if (error != null) {
                    update = UpdateState.Failed(error, (update as? UpdateState.Installing)?.info)
                    Updater.installError.value = null
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Self-update
    // -----------------------------------------------------------------------------------------

    /** Startup checks stay silent unless there's an update; a check from Settings always reports. */
    fun checkForUpdate(userInitiated: Boolean) {
        if (update is UpdateState.Checking || update is UpdateState.Downloading || update is UpdateState.Installing) return
        update = UpdateState.Checking
        viewModelScope.launch {
            update = try {
                updater.check()?.let { UpdateState.Available(it) } ?: UpdateState.UpToDate
            } catch (e: Exception) {
                UpdateState.Failed(e.message ?: "Couldn't reach GitHub. Check Wi-Fi and try again.", null)
            }
            val found = update is UpdateState.Available
            if ((found && dialog == null && screen !is Screen.Setup) || userInitiated) dialog = Dialog.Update
        }
    }

    fun installUpdate() {
        val info = when (val state = update) {
            is UpdateState.Available -> state.info
            is UpdateState.NeedsPermission -> state.info
            is UpdateState.Failed -> state.info ?: return checkForUpdate(userInitiated = true)
            else -> return
        }
        if (!updater.canInstall()) {
            update = UpdateState.NeedsPermission(info)
            runCatching { updater.openInstallPermissionSettings() }
            return
        }
        viewModelScope.launch {
            try {
                update = UpdateState.Downloading(info, 0f)
                val file = updater.download(info) { p -> viewModelScope.launch { update = UpdateState.Downloading(info, p) } }
                update = UpdateState.Installing(info)
                updater.install(file)
            } catch (e: Exception) {
                update = UpdateState.Failed(e.message ?: e.javaClass.simpleName, info)
            }
        }
    }

    fun dismissUpdate() {
        if (update !is UpdateState.Downloading && update !is UpdateState.Installing) dialog = null
    }

    private data class Loaded(
        val catalog: EsdeCatalog,
        val config: LauncherConfig,
        val configProblem: String?,
        val library: LibraryCache?,
        val stats: StatsFile,
        val art: ArtIndex,
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
                refreshLogos()
                if (config.autoScrape) scrapeArt(force = false)
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
    // Art
    // -----------------------------------------------------------------------------------------

    fun boxArt(game: Game): File? = scraper.file(art[game.id]?.box)

    fun snapArt(game: Game): File? = scraper.file(art[game.id]?.snap)

    fun artFor(game: Game): GameArt? = art[game.id]

    /** Downloads logos for the library's systems that don't have one yet. */
    private fun refreshLogos(force: Boolean = false) {
        val names = systems.map { it.name }
        fun publish(name: String) {
            logoStore.file(name)?.let { logos[name] = it }
            systemArtStore.image(name)?.let { systemImages[name] = it }
            systemArtStore.facts(name).takeIf { it.isNotEmpty() }?.let { systemFacts[name] = it }
        }
        names.forEach(::publish)
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                logoStore.fetchMissing(names, force) + systemArtStore.fetchMissing(names, force)
            }
            added.distinct().forEach(::publish)
        }
    }

    fun fanart(game: Game): File? = scraper.file(art[game.id]?.fanart)

    /** Hero art for a console: fanart of the last played game, else of any game that has some. */
    fun systemHero(system: SystemEntry): File? {
        val withFanart = system.games.filter { art[it.id]?.fanart != null }
        val pick = withFanart.maxByOrNull { stats(it).lastPlayed } ?: return null
        return fanart(pick)
    }

    // -----------------------------------------------------------------------------------------
    // ScreenScraper
    // -----------------------------------------------------------------------------------------

    fun editScreenScraper(field: TextField) {
        val c = screenScraper
        dialog = when (field) {
            TextField.SsUser -> Dialog.TextInput(field, "ScreenScraper username", c.user, secret = false)
            TextField.SsPassword -> Dialog.TextInput(field, "ScreenScraper password", c.password, secret = true)
            TextField.SsDevId -> Dialog.TextInput(field, "Developer ID", c.devId, secret = false)
            TextField.SsDevPassword -> Dialog.TextInput(field, "Developer password", c.devPassword, secret = true)
        }
    }

    fun submitText(field: TextField, value: String) {
        val c = screenScraper
        val updated = when (field) {
            TextField.SsUser -> c.copy(user = value.trim())
            TextField.SsPassword -> c.copy(password = value)
            TextField.SsDevId -> c.copy(devId = value.trim())
            TextField.SsDevPassword -> c.copy(devPassword = value)
        }
        screenScraper = updated
        screenScraperStatus = null
        dialog = null
        viewModelScope.launch(Dispatchers.IO) { ssAccount.save(updated) }
    }

    fun testScreenScraper() {
        val c = screenScraper
        if (!c.complete) {
            screenScraperStatus = "Fill in all four fields first."
            return
        }
        screenScraperStatus = "Checking…"
        viewModelScope.launch {
            screenScraperStatus = withContext(Dispatchers.IO) {
                try {
                    ScreenScraper(c).testLogin()
                } catch (e: ScreenScraperStop) {
                    e.message
                } catch (e: Exception) {
                    "Couldn't reach ScreenScraper: ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }
    }

    /** Games the scraper would fetch for: supported systems, not yet scraped (unless forced). */
    fun scrapeTodo(force: Boolean): Int = systems.filter { scraper.supports(it.name) }
        .sumOf { s -> if (force) s.games.size else s.games.count { scraper.needsArt(art[it.id], screenScraper.complete) } }

    fun setAutoScrape(enabled: Boolean) = updateConfig { it.copy(autoScrape = enabled) }

    fun scrapeArt(force: Boolean) {
        if (scrapeJob?.isActive == true) return
        val targets = systems.filter { scraper.supports(it.name) }
        val total = scrapeTodo(force)
        if (force) refreshLogos(force = true)
        if (total == 0) return
        scrape = ScrapeProgress(0, total, targets.first().info.displayName)
        val ss = screenScraper.takeIf { it.complete }?.let { ScreenScraperSession(ScreenScraper(it)) }
        scrapeJob = viewModelScope.launch {
            var done = 0
            try {
                for (system in targets) {
                    scrape = ScrapeProgress(done, total, system.info.displayName)
                    withContext(Dispatchers.IO) {
                        scraper.scrapeSystem(system.name, system.games, art.toMap(), force, ss) { game, result ->
                            viewModelScope.launch {
                                art[game.id] = result
                                done++
                                scrape = ScrapeProgress(done, total, system.info.displayName)
                                // Save as we go, so an update or restart mid-scrape doesn't lose finished art.
                                if (done % SAVE_EVERY == 0) {
                                    val snapshot = ArtIndex(art.toMap())
                                    launch(Dispatchers.IO) { scraper.save(snapshot) }
                                }
                            }
                        }
                    }
                }
            } finally {
                withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) { scraper.save(ArtIndex(art.toMap())) }
                scrape = null
                ss?.stopped?.let { screenScraperStatus = it }
            }
        }
    }

    fun stopScrape() {
        scrapeJob?.cancel()
    }

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
                sound(UiSound.Launch)
                val now = System.currentTimeMillis()
                session = game.id to now
                updateStats(game) { it.copy(lastPlayed = now, playCount = it.playCount + 1) }
            }
            is LaunchResult.Failed -> {
                sound(UiSound.Error)
                dialog = Dialog.LaunchError(system, game, result.problem)
            }
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
        // Back from granting "Install unknown apps": carry on with the update.
        (update as? UpdateState.NeedsPermission)?.let { if (updater.canInstall()) installUpdate() }
    }

    companion object {
        private const val MAX_SESSION_MS = 12L * 60 * 60 * 1000
        private const val SAVE_EVERY = 20
    }
}
