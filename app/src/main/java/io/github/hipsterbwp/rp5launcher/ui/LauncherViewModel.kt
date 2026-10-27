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
import io.github.hipsterbwp.rp5launcher.art.CustomArt
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
    data object Search : Screen
    data class Collection(val key: String) : Screen
}

sealed interface Dialog {
    /** Emulator for a whole system, or for one game when [gameId] is set. */
    data class EmulatorPicker(val system: String, val gameId: String? = null) : Dialog
    data class CollectionPicker(val gameId: String) : Dialog
    data class MatchPicker(val system: String, val gameId: String, val candidates: List<String>) : Dialog
    data class LaunchError(val system: String, val game: Game?, val problem: LaunchProblem) : Dialog
    data class Message(val title: String, val body: String) : Dialog
    data object Update : Dialog
    /** On-screen-keyboard entry for one setting. */
    data class TextInput(val field: TextField, val title: String, val initial: String, val secret: Boolean) : Dialog
}

enum class TextField { SsUser, SsPassword, SsDevId, SsDevPassword, Search, NewCollection }

/** A named list of games: one you made, or one ARCROW builds from your library. */
data class GameCollection(
    val key: String,
    val name: String,
    val entries: List<Pair<SystemEntry, Game>>,
    val custom: Boolean,
)

data class PlaySummary(
    val totalMs: Long,
    val gamesPlayed: Int,
    val launches: Int,
    val top: List<Pair<Game, GameStats>>,
    val currentStreak: Int,
    val longestStreak: Int,
)

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

data class LoadingStep(val progress: Float, val message: String)

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
    /** Progress shown on the ARCROW loading screen. */
    var loading by mutableStateOf(LoadingStep(0f, "Loading frontend…"))
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
    /** HD cover of each system's best-selling game, for the console screen. */
    val systemFeature = mutableStateMapOf<String, File>()
    private val featureTried = HashSet<String>()
    /** Your own backdrops from the ROMs folder's "_art" folder, per system. */
    val systemCustom = mutableStateMapOf<String, File>()

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

    fun setMusic(enabled: Boolean) = updateConfig { it.copy(music = enabled) }

    fun cycleMusicVolume() = updateConfig { it.copy(musicVolume = it.musicVolume % 3 + 1) }

    fun cycleBackdrop() = updateConfig {
        it.copy(backdrop = BACKDROPS[(BACKDROPS.indexOf(it.backdrop) + 1).mod(BACKDROPS.size)])
    }

    fun cycleBackdropBlur() = updateConfig { it.copy(backdropBlur = (it.backdropBlur + 1) % 3) }

    fun setBackdropTint(enabled: Boolean) = updateConfig { it.copy(backdropTint = enabled) }

    /** Set by the activity, which owns the music player. */
    var music: BackgroundMusic? = null

    val configPath: String get() = configStore.file.absolutePath

    private var scanJob: Job? = null
    private var session: Pair<String, Long>? = null

    init {
        viewModelScope.launch {
            // Real steps behind the loading screen's progress bar.
            loading = LoadingStep(0.1f, "Loading emulators…")
            val esde = withContext(Dispatchers.IO) { EsdeCatalog.load(getApplication<Application>().assets) }
            loading = LoadingStep(0.45f, "Loading settings…")
            val (loadedConfig, configProblem) = withContext(Dispatchers.IO) { loadConfig() }
            loading = LoadingStep(0.6f, "Loading systems…")
            val libraryCache = withContext(Dispatchers.IO) { libraryStore.loadLibrary() }
            val statsFile = withContext(Dispatchers.IO) { libraryStore.loadStats() }
            loading = LoadingStep(0.8f, "Loading artwork…")
            val artIndex = withContext(Dispatchers.IO) { scraper.load() }
            loading = LoadingStep(1f, "Ready")
            val loaded = Loaded(esde, loadedConfig, configProblem, libraryCache, statsFile, artIndex)
            art.putAll(loaded.art.games)
            screenScraper = withContext(Dispatchers.IO) { ssAccount.load() }
            catalog = loaded.catalog
            applyConfig(loaded.config)
            stats = loaded.stats
            library = loaded.library?.takeIf { it.rootUri == loaded.config.romsRoot }
            rebuildSystems()
            refreshLogos()
            refreshCustomArt()
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
        val visible = library?.systems.orEmpty().filter { it.name !in config.hiddenSystems }
        val colors = SystemColors.assign(visible.map { it.name })
        systems = visible.mapNotNull { scanned ->
            val def = catalog.system(scanned.name) ?: return@mapNotNull null
            SystemEntry(
                def = def,
                info = SystemMeta.info(def),
                color = parseColor(config.system(def.name).color) ?: colors.getValue(def.name),
                folderName = scanned.folderName,
                games = scanned.games.filter { stats.games[it.id]?.hidden != true },
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

    fun setSwapButtons(enabled: Boolean) = updateConfig { it.copy(swapButtons = enabled) }

    fun setButtonLabels(style: String) = updateConfig { it.copy(buttonLabels = style) }

    fun cycleScreensaver() = updateConfig { cfg ->
        val next = SCREENSAVER_STEPS[(SCREENSAVER_STEPS.indexOf(cfg.screensaverMinutes) + 1) % SCREENSAVER_STEPS.size]
        cfg.copy(screensaverMinutes = next)
    }

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
                refreshCustomArt()
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

    private fun updateStats(game: Game, update: (GameStats) -> GameStats) =
        updateStatsFile { it.copy(games = it.games + (game.id to update(stats(game)))) }

    private fun updateStatsFile(update: (StatsFile) -> StatsFile) {
        val updated = update(stats)
        stats = updated
        viewModelScope.launch(Dispatchers.IO) { libraryStore.saveStats(updated) }
    }

    fun toggleFavorite(game: Game) = updateStats(game) { it.copy(favorite = !it.favorite) }

    // -----------------------------------------------------------------------------------------
    // Hiding
    // -----------------------------------------------------------------------------------------

    fun hideGame(game: Game) {
        updateStats(game) { it.copy(hidden = true) }
        rebuildSystems()
    }

    fun hiddenGameCount(): Int = stats.games.count { it.value.hidden }

    fun unhideAllGames() {
        updateStatsFile { file -> file.copy(games = file.games.mapValues { it.value.copy(hidden = false) }) }
        rebuildSystems()
    }

    /** Every system found in the ROMs folder, hidden ones included, for the Library page. */
    fun scannedSystems(): List<Pair<String, String>> {
        val catalog = catalog ?: return emptyList()
        return library?.systems.orEmpty().mapNotNull { scanned ->
            catalog.system(scanned.name)?.let { scanned.name to SystemMeta.info(it).displayName }
        }
    }

    fun toggleSystemHidden(name: String) = updateConfig { cfg ->
        cfg.copy(hiddenSystems = if (name in cfg.hiddenSystems) cfg.hiddenSystems - name else cfg.hiddenSystems + name)
    }

    // -----------------------------------------------------------------------------------------
    // Collections, search, random, stats
    // -----------------------------------------------------------------------------------------

    private fun allGames(): List<Pair<SystemEntry, Game>> = systems.flatMap { s -> s.games.map { s to it } }

    fun collections(): List<GameCollection> {
        val all = allGames()
        val byId = all.associateBy { it.second.id }
        val custom = stats.collections.map { (name, ids) ->
            GameCollection("custom:$name", name, ids.mapNotNull(byId::get), custom = true)
        }
        val played = all.filter { stats(it.second).playCount > 0 }
            .sortedByDescending { stats(it.second).playtimeMs }.take(MOST_PLAYED)
        val auto = buildList {
            add(GameCollection("auto:most", "Most played", played, custom = false))
            add(GameCollection("auto:unplayed", "Not played yet", all.filter { stats(it.second).playCount == 0 }, custom = false))
            add(GameCollection("auto:multi", "Multiplayer", all.filter { maxPlayers(art[it.second.id]?.players) >= 2 }, custom = false))
            all.groupBy { art[it.second.id]?.genre?.let(::mainGenre) }
                .filterKeys { it != null }
                .filterValues { it.size >= MIN_GENRE_SIZE }
                .entries.sortedByDescending { it.value.size }.take(MAX_GENRES)
                .forEach { (genre, games) ->
                    add(GameCollection("genre:$genre", genre!!, games.sortedBy { it.second.title.lowercase() }, custom = false))
                }
        }
        return (custom + auto).filter { it.entries.isNotEmpty() || it.custom }
    }

    fun collection(key: String): GameCollection? = collections().firstOrNull { it.key == key }

    fun customCollectionNames(): List<String> = stats.collections.keys.toList()

    fun isInCollection(name: String, game: Game): Boolean = stats.collections[name]?.contains(game.id) == true

    /** Adds [game] to the collection, or takes it out if it's already there. */
    fun toggleInCollection(name: String, game: Game) = updateStatsFile { file ->
        val ids = file.collections[name].orEmpty()
        val updated = if (game.id in ids) ids - game.id else ids + game.id
        file.copy(collections = file.collections + (name to updated))
    }

    fun deleteCollection(name: String) = updateStatsFile { it.copy(collections = it.collections - name) }

    private var pendingCollectionGame: Game? = null

    fun newCollection(game: Game) {
        pendingCollectionGame = game
        dialog = Dialog.TextInput(TextField.NewCollection, "New collection", "", secret = false)
    }

    var searchQuery by mutableStateOf("")
        private set

    fun editSearch() {
        dialog = Dialog.TextInput(TextField.Search, "Search games", searchQuery, secret = false)
    }

    /** Games whose title contains every word of the query, best matches first. */
    fun searchResults(): List<Pair<SystemEntry, Game>> {
        val words = searchQuery.lowercase().split(' ').map { w -> w.filter(Char::isLetterOrDigit) }.filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        val first = words.first()
        return allGames()
            .filter { (_, g) ->
                val title = g.title.lowercase().filter { it.isLetterOrDigit() || it == ' ' }
                val compact = title.replace(" ", "")
                words.all { it in compact }
            }
            .sortedWith(
                compareBy<Pair<SystemEntry, Game>> { !it.second.title.lowercase().startsWith(first) }
                    .thenBy { it.second.title.lowercase() },
            )
    }

    fun playSummary(): PlaySummary {
        val played = stats.games.filterValues { it.playCount > 0 }
        val byId = allGames().associateBy { it.second.id }
        val top = played.entries.sortedByDescending { it.value.playtimeMs }
            .mapNotNull { (id, s) -> byId[id]?.let { it.second to s } }.take(TOP_GAMES)
        val days = stats.playDays.mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }.sorted()
        var longest = 0
        var run = 0
        var previous: java.time.LocalDate? = null
        days.forEach { day ->
            run = if (previous != null && previous!!.plusDays(1) == day) run + 1 else 1
            longest = maxOf(longest, run)
            previous = day
        }
        val today = java.time.LocalDate.now()
        val daySet = days.toHashSet()
        var current = 0
        var cursor = if (today in daySet) today else today.minusDays(1)
        while (cursor in daySet) {
            current++
            cursor = cursor.minusDays(1)
        }
        return PlaySummary(
            totalMs = played.values.sumOf { it.playtimeMs },
            gamesPlayed = played.size,
            launches = played.values.sumOf { it.playCount },
            top = top,
            currentStreak = current,
            longestStreak = longest,
        )
    }

    // -----------------------------------------------------------------------------------------
    // Fix a wrong match
    // -----------------------------------------------------------------------------------------

    fun fixMatch(system: String, game: Game) {
        viewModelScope.launch {
            val options = withContext(Dispatchers.IO) { scraper.candidates(system, game) }
            dialog = if (options.isEmpty()) {
                Dialog.Message("No other matches", "libretro has no other covers that look like \"${game.title}\". Try a ScreenScraper sign-in.")
            } else {
                Dialog.MatchPicker(system, game.id, options)
            }
        }
    }

    fun chooseMatch(system: String, game: Game, name: String) {
        dialog = null
        val entry = system(system) ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                scraper.rematch(system, game, name, art[game.id], entry.info.displayName)
            }
            art[game.id] = result
            val snapshot = ArtIndex(art.toMap())
            launch(Dispatchers.IO) { scraper.save(snapshot) }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Screensaver and Home button
    // -----------------------------------------------------------------------------------------

    var lastInputAt by mutableStateOf(System.currentTimeMillis())
        private set
    var screensaver by mutableStateOf(false)

    /** Any button press: resets the idle timer. Returns true if it only woke the screensaver. */
    fun onInput(): Boolean {
        lastInputAt = System.currentTimeMillis()
        if (screensaver) {
            screensaver = false
            return true
        }
        return false
    }

    /** The device's Home button: back to the console screen (unless still in setup). */
    fun goHome() {
        onInput()
        if (screen is Screen.Setup) return
        dialog = null
        resetTo(Screen.Home)
    }

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
            refreshFeatures()
        }
    }

    fun fanart(game: Game): File? = scraper.file(art[game.id]?.fanart)

    /** The best-selling game named in a system's fact sheet, without "(… million)" notes. */
    fun bestSeller(system: String): String? =
        systemFacts[system]?.firstOrNull { it.first.startsWith("Best-selling") }?.second
            ?.replace(Regex("""\s*\(.*?\)"""), "")?.trim()?.takeIf { it.isNotEmpty() }

    /** Fetches the best-seller cover for systems that don't have one yet (once per run each). */
    private fun refreshFeatures() {
        val wanted = systems.mapNotNull { s -> bestSeller(s.name)?.let { s.name to it } }
            .filter { (name, _) -> name !in systemFeature && featureTried.add(name) }
        if (wanted.isEmpty()) return
        viewModelScope.launch {
            wanted.forEach { (name, title) ->
                val file = withContext(Dispatchers.IO) {
                    runCatching { scraper.featureArt(name, title) }.getOrNull()?.let(scraper::file)
                }
                if (file != null) systemFeature[name] = file
            }
        }
    }

    fun titleArt(game: Game): File? = scraper.file(art[game.id]?.titleShot)

    /** Copies backdrops from the ROMs folder's "_art" folder; matched by system name or folder name. */
    private fun refreshCustomArt() {
        val root = rootUri() ?: return
        val app = getApplication<Application>()
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) {
                runCatching { CustomArt.sync(app.contentResolver, root, File(app.filesDir, "custom-art")) }.getOrNull()
            } ?: return@launch
            val matched = systems.mapNotNull { s ->
                (found[s.name] ?: found[s.folderName.lowercase()])?.let { s.name to it }
            }.toMap()
            (systemCustom.keys - matched.keys).forEach { systemCustom.remove(it) }
            systemCustom.putAll(matched)
        }
    }

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
            TextField.Search, TextField.NewCollection -> return
        }
    }

    fun submitText(field: TextField, value: String) {
        when (field) {
            TextField.Search -> {
                searchQuery = value.trim()
                dialog = null
                return
            }
            TextField.NewCollection -> {
                val name = value.trim()
                val game = pendingCollectionGame
                dialog = null
                if (name.isNotEmpty() && game != null && !isInCollection(name, game)) toggleInCollection(name, game)
                pendingCollectionGame = null
                return
            }
            else -> Unit
        }
        val c = screenScraper
        val updated = when (field) {
            TextField.SsUser -> c.copy(user = value.trim())
            TextField.SsPassword -> c.copy(password = value)
            TextField.SsDevId -> c.copy(devId = value.trim())
            TextField.SsDevPassword -> c.copy(devPassword = value)
            TextField.Search, TextField.NewCollection -> c
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
                        scraper.scrapeSystem(system.name, system.games, art.toMap(), force, ss, system.info.displayName) { game, result ->
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
    fun selectedCommand(system: String, game: Game? = null): CommandDef? {
        val all = commands(system)
        game?.let { g -> stats(g).emulator?.let { label -> all.firstOrNull { it.label == label } } }?.let { return it }
        val chosen = config.system(system).emulator
        return all.firstOrNull { it.label == chosen }
            ?: all.firstOrNull { launcher.isInstalled(it) }
            ?: all.firstOrNull()
    }

    fun isInstalled(command: CommandDef): Boolean = launcher.isInstalled(command)

    fun chooseEmulator(system: String, command: CommandDef) =
        updateConfig { cfg -> cfg.withSystem(system) { it.copy(emulator = command.label) } }

    /** Emulator for one game; null goes back to the system's choice. */
    fun chooseGameEmulator(game: Game, command: CommandDef?) = updateStats(game) { it.copy(emulator = command?.label) }

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
        val command = selectedCommand(system, game)
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
                val today = java.time.LocalDate.now().toString()
                if (today !in stats.playDays) updateStatsFile { it.copy(playDays = it.playDays + today) }
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
        private const val MOST_PLAYED = 50
        private const val MIN_GENRE_SIZE = 3
        private const val MAX_GENRES = 8
        private const val TOP_GAMES = 5
        val SCREENSAVER_STEPS = listOf(0, 2, 5, 10, 15)

        /** Largest number in "1-4", "2", "1 to 2"… (0 if unknown). */
        fun maxPlayers(players: String?): Int =
            players?.let { Regex("""\d+""").findAll(it).mapNotNull { m -> m.value.toIntOrNull() }.maxOrNull() } ?: 0

        /** "Role-playing (RPG)" -> "Role-playing"; "Action / Adventure" -> "Action". */
        fun mainGenre(genre: String): String =
            genre.substringBefore(" / ").substringBefore(',').substringBefore(" (").trim()
    }
}

/** Console screen backdrop choices, in the order Settings cycles through them. */
val BACKDROPS = listOf("auto", "cover", "console")
