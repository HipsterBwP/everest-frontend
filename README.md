# RP5 Launcher

A controller-first Android game launcher for the Retroid Pocket 5 (Android 13, 1920×1080), built with
Kotlin and Jetpack Compose. The UI follows [DESIGN.md](DESIGN.md) ("Cartridge" style); every design
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
| Home | ←/→ choose · **A** open · **L1/R1** tabs (Systems, Favorites, Recent, Collections) · **START** settings |
| Game grid | D-pad move (hold to accelerate) · **A** details · **B** back · **Y** favorite · **L1/R1** filter · **L2/R2** jump by letter |
| Game details | **A** play · **B** back · **X** change emulator · **Y** favorite |
| Settings | ↑/↓ pages and rows · →/**A** enter · **A** change · **X** test-launch · **B** back |

The hint bar at the bottom of every screen always shows what the buttons do right now.

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
- Console logos for the system tiles from the RetroPie Carbon theme

Games are matched by file name, so No-Intro / Redump names (e.g. `Chrono Trigger (USA).sfc`) match best.
Art is stored in the app's private storage, downsized for the screen.

## Not built yet

Game descriptions, search, collections, Resume save, the Appearance/Controls settings pages and
navigation sounds.

## Credits

- Emulator launch configuration: [ES-DE](https://gitlab.com/es-de/emulationstation-de) Android
  `es_systems.xml` / `es_find_rules.xml`, MIT License — see
  [`assets/esde/LICENSE-ES-DE.txt`](app/src/main/assets/esde/LICENSE-ES-DE.txt) and
  [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
- Font: [Bricolage Grotesque](https://github.com/ateliertriay/bricolage), SIL Open Font License 1.1.
