# DESIGN.md — ARCROW

Design source of truth for the launcher. Every screen and feature must follow this file.
Visual reference: the original Cartridge mockups (https://claude.ai/artifact/TZq3HWB8GAsEpvCGSR4asb, **B** row) for proportions;
the visual style below supersedes their colors, type and focus treatment.

## Target device

- Retroid Pocket 5: Android 13, 5.5" 1920×1080 AMOLED, landscape, 16:9.
- Mockups are drawn at 960×540 (half resolution). Treat 1 mockup px ≈ 1 dp-like unit and scale ×2 to device pixels.
- Controller-first. Every action must be reachable with D-pad, A/B/X/Y, L1/R1, L2/R2, Start, Select. Touch is a bonus, never required.

## Visual style: Apple-TV-inspired dark ("Cartridge" retired Oct 2026)

Dark, calm, content-first, in the spirit of tvOS: game art does the talking, chrome is frosted glass.
The original Cartridge mockups still define layout proportions where a screen isn't described below.

### Color

- Pages are near-black (`#000000`); grouped cards `#1C1C1E`, chips/secondary rows `#2C2C2E`, sidebars `#111113`.
- Text: primary `#FFFFFF`, secondary `#A1A1A8` (≥4.5:1 on cards), body `#D9D9DE`; text on white `#1C1C1E`.
- Accent: Apple system blue `#0A84FF` on settings-style screens. Status: OK `#30D158`, problem `#FF6961`, stars `#FFD60A`.
- **System color is now an accent**, not a page fill: console screen background when there's no hero art,
  the details panel, badges. Defaults (SNES `#2E2370`, NES `#B3261E`, GB `#3E6B1F`, GBA `#3949AB`,
  N64 `#1E7353`, PS1 `#4D4D57`, PS2 `#1F4BA5`, PSP `#1A1A1A`) stay editable per system in the config file.
- Glass: `rgba(22,22,24,0.62)` (strong: 0.80) with a 1 px `rgba(255,255,255,0.12)` edge, always over blurred
  or dimmed art. Soft shade gradients are allowed only to keep text readable over art.
- All text must meet 4.5:1 contrast (3:1 for text ≥24 px).

### Type

- Font: **Inter** (SIL OFL, bundled), the closest freely licensed match to Apple's San Francisco.
- Weights: 400 body, 600 labels, 700 headings.
- Scale (mockup px): hero 60 / page title 44 / section 30 / screen title 22–30 / body 14–15 / label 12–13 / micro 10–11.
- Headings: tracking −0.015 to −0.025 em, line-height ~1.02. Eyebrow labels: 12 px caps, +0.08 em.

### Shape, depth and spacing

- Radii: pills 999; cards 14–16; panels 22–28.
- Screen padding: 40 horizontal, ~26–36 top. Gaps: 6–8 in lists, 14 between cards, 18–28 between sections.
- Depth: soft shadows under media and lifted covers (10 → 24 units), blur (26 units) on backdrop art.

### Focus / selection

- Covers: tvOS lift — scale ≈1.08, deeper shadow, 3 px white edge, drawn above neighbours.
- Logo rail: the selected logo is centered, larger and fully opaque; others fade to 45%.
- Lists on dark screens: 3 px accent outline offset 4 px.
- Focus moves animate (spring, ~150–200 ms). Nothing ever jumps.

### Controller hint bar

- Always present at the bottom, 48 px tall, full width, frosted glass with a hairline top edge.
- Each hint = button glyph (24 px white circle, or rounded rect for L1/R1, L2/R2, START, SELECT) + label.
- Left-aligned primary actions; Start/Select hints right-aligned.
- Hints reflect what the buttons actually do on the current screen. B always means Back.

## Screens

1. **Home / Systems** — left: vertical rail of console logos (Up/Down); next to it a glass panel with the console
   illustration, maker · year, name, game and favorite counts, a short fact sheet and last played; right: hero
   fanart from one of that system's games (or the console illustration as a product shot). Top right: tabs
   (Systems / Favorites / Recent / Collections, L1/R1) and a clock/battery capsule. A opens the system; Start settings.
2. **Game browser** (a system's games, and the Favorites/Recent tabs) — blurred, dimmed art of the focused game
   fills the screen. Left: gameplay video over its screenshot, then a glass card with system · year, title,
   rating stars, genre/players pills and description. Right: logo or title, count, filter pills, and a 3-column
   cover grid. **A plays immediately**; X options; Y favorite; L1/R1 filter; L2/R2 jump by letter; B back.
3. **Options** (was Game details) — system-color panel with box art over the screenshot; title, publisher · year,
   Play, stats, emulator. A play; B back; X change emulator; Y favorite.
4. **First-run setup (3 steps)** — 1) pick ROMs folder; 2) detected systems with matched emulators, missing ones
   flagged with a fix; 3) download art. Target: install → playing in under 2 minutes.
5. **Settings** — dark sidebar (Library, Emulators, Scraper, ScreenScraper, Sound, About), grouped rows.
6. To design next: Search, Collections, remaining Appearance/Controls pages.

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
- Brand: the frontend is **ARCROW**. App icon and loading screen use the crow logo (`res/drawable-nodpi/brand_logo.webp`)
  and wordmark (`brand_wordmark.png`) on navy `#050B18` with electric-blue glow `#1E6BFF`. The loading screen shows real
  start-up progress (emulators → settings → systems → artwork) and stays at least 1.4 s, then fades into Home.
