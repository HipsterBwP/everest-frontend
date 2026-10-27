# ARCROW

ARCROW is a controller-first Android game launcher for the Retroid Pocket 5 (Android 13, 1920×1080), built with
Kotlin and Jetpack Compose. The UI follows [DESIGN.md](DESIGN.md) (an Apple-TV-inspired dark style); every design
token lives in [`ui/theme/Theme.kt`](app/src/main/java/io/github/hipsterbwp/rp5launcher/ui/theme/Theme.kt).

## Getting the APK

Every push to `main` publishes a GitHub release (`build-<n>`) with `rp5-launcher.apk` attached. Install
it once by hand; after that the launcher checks for a newer release every time it starts (and from
**Settings › About › Check for updates**), downloads it and hands it to Android to install. The repo
must be public for the launcher to see releases.

Pushes to other branches still build and upload the APK as a workflow artifact.

To build locally (needs JDK 17 and the Android SDK): `./gradlew assembleDebug`.

## First run

1. Pick your ROMs folder in the Android folder picker. Each subfolder is a system, named like
   ES-DE's folders: `snes`, `gba`, `ps2`, `psx`, `n64`, … (a few aliases such as `ps1` and `sfc` work too).
2. The launcher scans in the background and shows each system with its matched emulator.
   Missing emulators are flagged in red; press **A** to pick another, **X** to open its store page.

## Controls

| Screen | Buttons |
|---|---|
| Home · Systems | ↑/↓ choose a console · **A** open · **X** continue the last game you played on it · **Y** search · **L1/R1** tabs (Systems, Favorites, Recent, Collections) · **START** settings |
| Anywhere | hold **SELECT** for the quick menu: brightness, volume, music, Wi-Fi, Bluetooth, Android settings |
| Game lists (a system, Favorites, Recent, a collection, search results) | D-pad move (hold to accelerate) · **A** play · **X** options · **Y** favorite · **SELECT** random game · **B** back · in a system: **L1/R1** filter, **L2/R2** jump by letter |
| Options | ↑/↓ choose · **A** do it: play, favorite, progress (backlog / playing / beaten / 100%), emulator for this game or the whole system, add to collection, fix artwork match, hide |
| Settings | ↑/↓ pages and rows · →/**A** enter · **A** change · **X** test-launch · **B** back |

The hint bar always shows what the buttons do right now. **Settings › Controls** swaps the confirm
button (bottom or right), switches hints to PlayStation symbols (✕ ○ □ △), sets the screensaver delay,
and opens Android's setting to make ARCROW the home screen (the Home button then returns to it).

## Library extras

- **Collections**: smart ones (Playing, Backlog, Beaten, 100% complete, Most played, Not played yet,
  Multiplayer, by genre) plus your own, made from a game's Options › Add to collection.
- **Progress**: mark games Backlog, Playing, Beaten or 100% complete from their Options. Starting an
  untracked or backlog game marks it Playing.
- **RetroAchievements**: sign in under Settings › RetroAchievements (username + web API key from
  retroachievements.org › Settings › Keys). Game pages show 🏆 unlocked/total; Settings › Stats shows
  your points, rank and latest unlocks. Games are matched by title; it refreshes after each game.
- **Search**: Y on the console screen.
- **Hiding**: hide a game from its Options; unhide, and show or hide whole systems, in Settings › Library.
  BIOS files are skipped, and discs/tracks listed by an `.m3u`, `.cue` or `.gdi` appear once.
- **Stats**: Settings › Stats shows total play time, most played games and day streaks.
- **Background music**: three original tracks play softly while you browse and pause while a game
  runs. Put your own MP3/OGG/FLAC files in a `music` folder inside the ROMs folder to play those
  instead. On/off, volume and next track are in Settings › Sound, where each track can be switched
  off (A) or put on repeat (X).
- **Console startup sounds**: name sound files after each system (`snes.ogg`, `ps2.mp3`…) and import
  them in Settings › Sound, or put them in a `_sounds` folder in the ROMs folder. They play when you
  land on a console, with the music dipping underneath.
- **Gameplay videos from ES-DE**: choose ES-DE's folder in Settings › Library and the videos ES-DE
  downloaded play in game lists, and as a montage on the console screen after a few seconds
  (Settings › Appearance turns the montage off).

## Your own console backdrops

Name one picture per system after its folder or ES-DE name: `snes.jpg`, `ps2.png`, `n64.webp`, `megadrive.jpg`…
Then either import them (or a .zip of them) from Settings › Appearance › Your own pictures, or put them in a folder
called `_art` inside your ROMs folder and rescan from Settings › Library. An `arcrow-art` folder or `arcrow-art.zip`
in the ROMs folder works too. The console screen shows them full screen. Wide 1920×1080 pictures look best; keep the main subject in the
right half, since the logo rail and info panel cover the left. Settings › Appearance switches between your
pictures, the best-selling game's cover and the console on its own, and sets the blur and colour tint.

## Emulators and the config file

Launch commands come straight from ES-DE's Android `es_systems.xml` and `es_find_rules.xml`, bundled
unmodified in [`app/src/main/assets/esde/`](app/src/main/assets/esde). The launcher understands their
`%EMULATOR_…%`, `%ACTION%`, `%DATA%`, `%EXTRA_…%`, `%ROM%`, `%ROMSAF%`, … syntax and turns it into
Android intents.

Per-system choices are saved to an editable JSON file:

```
Android/data/io.github.hipsterbwp.rp5launcher/files/launcher-config.json
```

```json
{
  "systems": {
    "snes": { "emulator": "Snes9x - Current" },
    "gba":  { "emulator": "mGBA" },
    "ps2":  { "emulator": "NetherSX2 (Standalone)", "color": "#1F4BA5" },
    "psp":  { "commands": [ { "label": "My PPSSPP", "command": "%EMULATOR_PPSSPP% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF%" } ] }
  },
  "emulators": {
    "PPSSPP": ["org.ppsspp.ppssppgold/org.ppsspp.ppsspp.PpssppActivity"]
  }
}
```

- `emulator` is a command label from ES-DE's `es_systems.xml` (or one of your own `commands`).
- `color` overrides the system color.
- `hiddenSystems` (top level) lists systems to leave off the console list, e.g.
  `"hiddenSystems": ["androidapps", "androidgames", "emulators"]` (the default).
- `commands` adds launch options in ES-DE syntax; `emulators` adds or overrides find rules
  (`package/activity`).

Edit it over USB, then use **Settings › About › Reload config file**. Choosing an emulator in the app
writes to the same file. Defaults on first run: SNES → RetroArch (Snes9x), GBA → RetroArch (mGBA),
PS2 → NetherSX2. Other systems use the first ES-DE option that's installed.

RetroArch is started with a file path (`%ROM%`), so RetroArch itself needs storage access to your ROMs
folder. Standalone emulators such as NetherSX2 get a content URI with a read grant.

## Art and game info

The launcher has its own scraper (no other frontend or account needed). Run it from the last setup
step or **Settings › Scraper**:

- Box art and screenshots from [libretro-thumbnails](https://github.com/libretro-thumbnails)
- Year, publisher, developer and genre from [libretro-database](https://github.com/libretro/libretro-database)
- Descriptions from English Wikipedia (only when the article is clearly about that game)
- Title screens from libretro-thumbnails, shown with the screenshot as a slow slideshow
- Console logos for the system tiles from the RetroPie Carbon theme, with ES-DE's Slate theme for
  newer systems (Switch, 3DS, Vita, …)

**ScreenScraper (optional):** sign in under **Settings › ScreenScraper** to add gameplay videos, fanart,
descriptions and ratings. It needs your free screenscraper.fr account plus a developer ID (requested on
the ScreenScraper forum). Credentials are kept in the app's private storage, not the config file.

Games are matched by file name, so No-Intro / Redump names (e.g. `Chrono Trigger (USA).sfc`) match best.
Art is stored in the app's private storage, downsized for the screen.

## Not built yet

Search, collections, Resume save and the Appearance/Controls settings pages.

## Credits

- Emulator launch configuration: [ES-DE](https://gitlab.com/es-de/emulationstation-de) Android
  `es_systems.xml` / `es_find_rules.xml`, MIT License — see
  [`assets/esde/LICENSE-ES-DE.txt`](app/src/main/assets/esde/LICENSE-ES-DE.txt) and
  [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
- Font: [Bricolage Grotesque](https://github.com/ateliertriay/bricolage), SIL Open Font License 1.1.
