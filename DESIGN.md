# DESIGN.md — RP5 Launcher

Design source of truth for the launcher. Every screen and feature must follow this file.
Visual reference (clickable mockups): https://claude.ai/artifact/TZq3HWB8GAsEpvCGSR4asb — use the **B · Cartridge** row only.

## Target device

- Retroid Pocket 5: Android 13, 5.5" 1920×1080 AMOLED, landscape, 16:9.
- Mockups are drawn at 960×540 (half resolution). Treat 1 mockup px ≈ 1 dp-like unit and scale ×2 to device pixels.
- Controller-first. Every action must be reachable with D-pad, A/B/X/Y, L1/R1, L2/R2, Start, Select. Touch is a bonus, never required.

## Visual style: "Cartridge"

Color-blocked per system, chunky rounded shapes, bold friendly type.

### Color

- **System color drives the theme.** The selected system's color fills the background on Home and Game grid, and tints accents (pills, Play button, side panels) everywhere else. Transitions between system colors animate (cross-fade ~250 ms).
- Default system colors (editable per system in Appearance settings):

| System | Color |
|---|---|
| SNES | `#2E2370` |
| NES | `#B3261E` |
| Game Boy | `#3E6B1F` |
| GBA | `#3949AB` |
| N64 | `#1E7353` |
| PS1 | `#4D4D57` |
| PS2 | `#1F4BA5` |
| PSP | `#1A1A1A` |

- Neutrals:
  - Light page (details, setup, settings): `#F4F1EA`
  - Light surface / chip: `#E4DFD3`; cards `#FFFFFF`
  - Ink: `#1C1A24`; secondary ink `#5A5666`; body ink `#3A3646`
  - Hint bar on light screens: `#1C1A24`; on system-color screens: `rgba(0,0,0,0.28)`
  - Translucent surfaces on system color: `rgba(255,255,255,0.10–0.14)`; dark chips `rgba(0,0,0,0.25)`
- Status: OK `#1E6B48`, problem `#A3361A`.
- All text must meet 4.5:1 contrast (3:1 for text ≥24 px). Check every new system color against white text.

### Type

- Font: **Bricolage Grotesque** (Google Fonts, OFL). Bundle it in the APK; no network fonts.
- Weights: 400 body, 600 labels, 800 headings.
- Scale (mockup px): hero 76 / page title 54 / section 34 / screen title 22–30 / body 14–15 / label 12–13 / micro 10–11.
- Headings: tight tracking (−0.02 to −0.03 em), line-height ~0.95.

### Shape and spacing

- Radii: pills 999; system tiles 24 (selected 30); cards 14–16; big panels 22–28.
- Screen padding: 40 horizontal, ~26–36 top.
- Gaps: 6–8 within lists, 14 between tiles/cards, 18–28 between sections.
- No drop shadows, no gradients. Depth comes from color blocks and translucency.

### Focus / selection

- The focused item is always unmistakable:
  - On system color: item turns white with system-color text, plus a 3–4 px translucent white outline offset 4 px, and is slightly larger (≈1.15–1.25×).
  - On light screens: 3 px system-color outline.
- Focus moves animate (spring, ~150–200 ms). Nothing ever jumps.

### Controller hint bar

- Always present at the bottom, 48 px tall, full width.
- Each hint = button glyph (24 px white circle, or rounded rect for L1/R1, L2/R2, START, SELECT) + label.
- Left-aligned primary actions; Start/Select hints right-aligned.
- Hints reflect what the buttons actually do on the current screen. B always means Back.

## Screens

1. **Home** — tabs (Systems / Favorites / Recent / Collections, switch with L1/R1); hero with system maker·year, big system name, game count and last-played chips; system artwork; bottom carousel of system tiles. A opens the system; Y search; Start settings.
2. **Game grid** — system name + count; filter pills (L1/R1); 6-column cover grid, title + year on each card, playtime on focused card. A details; B back; Y favorite; L2/R2 jump by letter.
3. **Game details** — left panel in system color with box art; right light page: system/genre/players pills, title, publisher·year, description, Play + Resume save, stats (playtime, save states, emulator). A play; B back; X change emulator; Y favorite.
4. **First-run setup (3 steps)** — 1) pick ROMs folder; 2) "We found your games": detected systems, game counts and auto-matched emulators, missing emulators flagged with a fix; 3) optional scraping. Target: install → playing in under 2 minutes.
5. **Settings** — system-color sidebar (Library, Emulators, Appearance, Controls, Scraper, About); Emulators page lists per-system emulator, re-detect, X to test-launch.
6. To design next: Search, Favorites/Recent views, remaining setup steps and settings pages, launch-error dialog.

## Quality bar

- 60 fps everywhere, including fast scrolling a 5,000+ game library.
- Cold start to interactive Home < 1 s on the RP5; library scans run in the background and never block UI.
- Art loads progressively with placeholders; no layout shift.
- Holding a direction accelerates scrolling.
- Subtle navigation sounds (toggleable).
- A failed launch always shows what went wrong and how to fix it — never a silent no-op or crash.
- No clipped or overlapping text at any supported string length; long titles truncate gracefully.

## Implementation notes

- Kotlin + Jetpack Compose. Design tokens (colors, type, radii, spacing) live in one theme file mirroring this document; no hard-coded values in screens.
- Emulator launch intents: reference ES-DE's Android `es_systems.xml` / `es_find_rules.xml` (MIT; keep attribution).
- Placeholders in mockups (box art, system artwork, launcher name) are to be filled by the scraper and a final brand pass.
