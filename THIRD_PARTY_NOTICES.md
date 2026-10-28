# Third-party notices

## ES-DE (EmulationStation Desktop Edition)

`app/src/main/assets/esde/es_systems.xml` and `app/src/main/assets/esde/es_find_rules.xml` are copied
unmodified from ES-DE's `resources/systems/android/` directory
(https://gitlab.com/es-de/emulationstation-de). The launcher reads them to find emulator packages,
activities and intent extras.

```
Copyright (c) 2024-2026 Northwestern Software AB
Copyright (c) 2020-2024 Leon Styhre
Copyright (c) 2014 Alec Lofquist
```

Licensed under the MIT License; the full text is in `app/src/main/assets/esde/LICENSE-ES-DE.txt`.

## Press Start 2P, Chakra Petch, VT323

`app/src/main/res/font/pressstart2p_regular.ttf`, `chakrapetch_*.ttf` (weights 400, 600, 700) and
`vt323_regular.ttf`, from https://github.com/google/fonts/tree/main/ofl.

```
Copyright 2012 The Press Start 2P Project Authors (cody@zone38.net), with Reserved Font Name "Press Start 2P".
Copyright 2018 The Chakra Petch Project Authors (https://github.com/m4rc1e/Chakra-Petch.git)
Copyright 2011, The VT323 Project Authors (peter.hull@oikoi.com)
```

Licensed under the SIL Open Font License 1.1; the full texts are in `app/src/main/assets/OFL-PressStart2P.txt`,
`OFL-ChakraPetch.txt` and `OFL-VT323.txt`.

## Downloaded at runtime (not included in the APK)

- **Box art and screenshots**: [libretro-thumbnails](https://github.com/libretro-thumbnails).
  Artwork is copyright of the respective publishers.
- **Year, publisher, developer, genre**: [libretro-database](https://github.com/libretro/libretro-database)
  `metadat` files.
- **Console logos**: [RetroPie es-theme-carbon](https://github.com/RetroPie/es-theme-carbon), with
  [ES-DE Slate](https://gitlab.com/es-de/themes/slate-es-de) (CC BY-NC-SA) for systems Carbon lacks.
  Logos are trademarks of their respective owners.
- **Console illustrations and fact sheets**: ES-DE Slate theme (`images/consolegame.svg`,
  `images/controller.svg`, `systeminfo.xml`), CC BY-NC-SA.
- **Game descriptions**: English [Wikipedia](https://en.wikipedia.org) article intros, CC BY-SA 4.0.
- **Fanart, gameplay videos, descriptions, ratings** (optional, with the user's own account):
  [ScreenScraper.fr](https://www.screenscraper.fr).

## Sounds

`app/src/main/res/raw/ui_*.wav` were synthesized for this project and carry no third-party rights.

## Music

`app/src/main/res/raw/music_*.ogg` (Neon Drift, Pixel Garden, After Hours) are original compositions
synthesized for Everest Frontend by `tools/music/compose.py`, with no samples or third-party material.

## Libraries

- [Coil](https://github.com/coil-kt/coil), Apache License 2.0
- [AndroidSVG](https://github.com/BigBadaboom/androidsvg), Apache License 2.0
- [AndroidX Media3 / ExoPlayer](https://github.com/androidx/media), Apache License 2.0
