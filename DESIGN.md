# DESIGN.md — Everest Frontend

Design source of truth for the launcher. Every screen and feature must follow this file.
Visual reference: the original Cartridge mockups (https://claude.ai/artifact/TZq3HWB8GAsEpvCGSR4asb, **B** row) for proportions;
the visual style below supersedes their colors, type and focus treatment.

## Target device

- Retroid Pocket 5: Android 13, 5.5" 1920×1080 AMOLED, landscape, 16:9.
- Mockups are drawn at 960×540 (half resolution). Treat 1 mockup px ≈ 1 dp-like unit and scale ×2 to device pixels.
- Controller-first. Every action must be reachable with D-pad, A/B/X/Y, L1/R1, L2/R2, Start, Select. Touch is a bonus, never required.

## Visual style: retro arcade / synthwave (Apple-TV style retired Oct 2026)

A neon arcade at night: deep purple sky, hot pink and cyan neon, sunset-gradient titles in a pixel
font, a perspective grid floor, arcade-button hints and faint CRT scanlines. Game art still does the
talking; the chrome is neon-framed panels over it. Layout proportions are unchanged.

### Color

- Night `#0B0420` (pages), deep `#160935`, violet `#2A0E5C`; list rows `#170A36`, chips `#24104A`, sidebars `#0E0526`.
- Neon: pink `#FF2A9D` (soft `#FF6FC3`) for focus, frames and accents; cyan `#2DE2E6` (soft `#9FF7F8`) for
  labels, corner brackets and the selected console.
- Sunset gradient, top to bottom: `#FFE45E` → `#FF9A3D` → `#FF3C8E` → `#C32AFF` → `#6A2CFF`, with a hard
  `#2A0650` drop shadow, for big titles (`ChromeTxt`), the sun and the loading bar.
- Text: primary `#F6ECFF`, secondary `#BDA6E6` (≥4.5:1 on rows), faint `#7F6AA8` for values. Status: OK `#2BD96B`,
  problem `#FF4D6D`, stars/scores `#FFE45E`.
- Arcade buttons: with PlayStation glyphs, the colours on the RP5's face buttons — ✕ blue `#3C8DFF`, ○ orange-red
  `#FF7A2E`, □ red `#FF3B5C`, △ green `#2BD96B`; with letters, A red, B yellow, X blue, Y green. Other keys lavender.
- System colors stay editable per system but are only a light tint on backdrops (`BACKDROP_TINT`).
- Backdrop art gets a pink-to-cyan colour wash (blend mode Color) and a night-coloured shade behind panels.

### Type

- **Press Start 2P** (pixel): titles, tabs, menus, hints, small caps labels. Always upper case. Crispest at
  multiples of 4 device px; sizes run smaller than before because the face is wide (page 20, section 18,
  menu 9, labels 7, badges 5–6 units).
- **Chakra Petch** (400/600/700): body text, descriptions, list-row titles and values.
- **VT323**: CRT numbers — clock, scoreboard counts, loading status.
- Eyebrow labels: pixel 7 units, +0.12 em, cyan with a soft glow.

### Shape, depth and glow

- Radii: rows 14, cards 16, panels 22; pixel tags and key caps 8.
- Main panels (`neonPanel`): dark violet fill, 1.5-unit pink edge with a soft glow, cyan corner brackets
  top-left and bottom-right. Small surfaces (`glass`) use a thin pink edge without glow.
- Backgrounds without art use `RetroBackdrop`: stars, pink horizon haze, cyan-edged mountains, pink grid
  floor (the loading screen adds the striped sun). Settings and setup dim it.
- `CrtOverlay` over everything: 2-px scanlines every 4 px and a vignette (Settings › Appearance › CRT scanlines).

### Focus / selection

- Selection glows breathe (`rememberGlowPulse`, GLOW_PULSE_MS): brighter and wider, then softer, in step.
- Lists and menus: pink fade with a glowing pink edge and a yellow ▶ cursor in front.
- Covers: lift (≈1.08) with a glowing pink edge; unfocused covers dim to 82%.
- Logo rail: the selected console sits in a cyan neon box with a pink ▶; others fade to 30%.
- Tabs: a pink-edged strip, the selected tab filled pink.
- Focus moves animate (spring, ~150–200 ms). Nothing ever jumps.

### Controller hint bar

- Always present at the bottom, 48 units tall, fading from transparent to night.
- Each hint = arcade button (24-unit candy-coloured circle with a glow, or a lavender key cap for L1/R1,
  L2/R2, START, SELECT) + pixel-font label.
- Left-aligned primary actions; Start/Select hints right-aligned.
- Hints reflect what the buttons actually do on the current screen. B always means Back.

### Brand

- Name: **Everest Frontend**; the app label under the icon is just "Everest" ("Everest" in running text,
  "EVEREST" in pixel type, "FRONTEND" under it on the loading screen).
- Logo: the round synthwave mountain badge (sun, snowy peak, lake reflection, neon ring), `brand_logo.webp`.
- App icon: the badge centred on a night background with a violet glow (adaptive icon: background + badge
  foreground, sized to sit inside every launcher mask); legacy launchers get the round badge. Android's splash
  shows the badge on night `#0B0420`.
- Loading screen: the badge glowing above the horizon, EVEREST in sunset pixel letters on the grid, "FRONTEND",
  a 20-block arcade loading bar, status in VT323, blinking INSERT COIN, 1UP / CREDIT corners.

## Screens

1. **Home / Systems** — left: vertical rail of console logos (Up/Down); a "P1 · Everest Frontend" tag and a pink neon
   edge; next to it a neon panel with maker · year (cyan), the name in sunset letters, a scoreboard (GAMES / FAVS /
   PLAYED in VT323), a dashed fact sheet, a pink "▶ CONTINUE?" card (X) for the last-played game, and the console (or controller) illustration filling the panel's lower part; the rail highlights the
   selected logo and dims the rest. Right, centred in the space beside the panel: by default the console's logo
   as a spinning 3D neon sign (`LogoSign`: sunset face, cyan rim, pink-to-violet extrusion that slides as it
   turns, breathing pink glow; it turns like a coin so it never reads mirrored). Settings › Appearance can switch
   to "Your pictures" (your own picture from the `_art` folder full screen, else fanart, else the sign), the
   best-selling game's cover, or the console illustration. The panel's console picture shows just the console or
   controller: game media beside it (discs, cartridges, cards) is cut off when the picture is rendered. Full-screen art gets a shade on the left (tinted with the system color unless tinting is off) and
   along the top. After MONTAGE_DELAY on a console, muted gameplay clips of its games play full screen
   over the backdrop (same shades) with a glass "Now showing" caption. A console's startup sound (if the
   player added one) plays once the selection settles. With no art at all, the synthwave backdrop shows.
   Top right: tabs (Systems / Favorites / Recent / Collections, L1/R1), a cyan VT323 clock and a pixel battery. A opens the system; Start settings.
2. **Game browser** (a system's games, and the Favorites/Recent tabs) — the synthwave backdrop. Left: a cyan
   neon panel with gameplay video over its screenshot, system · year, the title in sunset letters, credits and
   play time, chips (achievements, progress, players, favorite) and description. Right: logo or title, count,
   filter tabs, and a 3-column cover grid with favorite stars and progress tags. **A plays immediately**; X options; Y favorite; L1/R1 filter; L2/R2 jump by letter; B back.
3. **Options** (was Game details) — blurred art under a violet-to-night radial shade; the box art turned towards
   a neon "— 1 PLAYER START —" menu with the title in sunset letters, credits, chips and pixel-font entries. A play; B back; X change emulator; Y favorite.
4. **First-run setup (3 steps)** — 1) pick ROMs folder; 2) detected systems with matched emulators, missing ones
   flagged with a fix; 3) download art. Target: install → playing in under 2 minutes.
5. **Settings** — dimmed synthwave backdrop; dark sidebar with the pages as a pixel menu (selected in pink);
   rows as arcade menu entries.
6. **Collections tab** — glass list of collections (yours first, then smart ones) beside the selected
   collection's title, count and a strip of covers; A opens it in the game browser.
7. **Search** — Y on Home; on-screen keyboard, results in the game browser (Y edits the search).
8. **Screensaver** — after the configured idle time: random game art full-screen with a slow zoom and a
   glass caption; any button wakes it without doing anything else.
9. Options (3) is an action list: Play (larger pixel type), Favorite, Progress, Emulator for this game, Emulator for
   all games of the system, Add to collection, Fix artwork match, Hide. Up/Down, A, B.

## Quality bar

- 60 fps everywhere, including fast scrolling a 5,000+ game library.
- Cold start to interactive Home < 1 s on the RP5; library scans run in the background and never block UI.
- Art loads progressively with placeholders; no layout shift.
- Holding a direction accelerates scrolling.
- Subtle navigation sounds (toggleable).
- A failed launch always shows what went wrong and how to fix it — never a silent no-op or crash.
- No clipped or overlapping text at any supported string length; long titles truncate gracefully.

## Implementation notes

- Kotlin + Jetpack Compose. Design tokens (colors, type, radii, spacing, elevation, blur) live in one theme file mirroring this document; no hard-coded values in screens.
- Emulator launch intents: reference ES-DE's Android `es_systems.xml` / `es_find_rules.xml` (MIT; keep attribution).
- Brand: the frontend is **Everest Frontend**. App icon and loading screen use the crow logo (`res/drawable-nodpi/brand_logo.webp`)
  and wordmark (`brand_wordmark.png`) on navy `#050B18` with electric-blue glow `#1E6BFF`. The loading screen shows real
  start-up progress (emulators → settings → systems → artwork) and stays at least 1.4 s, then fades into Home.

## Quick menu

Holding Select anywhere opens a glass sheet on the right (Size.QUICK_MENU_W) over a dim layer: battery
header, then rows for brightness and volume (level meters, Left/Right adjust), music (A on/off, Right next
track), Wi-Fi and Bluetooth (A opens Android's panel), and Android settings. B, Select or Start closes it.
A short Select press still does the screen's own Select action.
