package io.github.hipsterbwp.everest.data

/** Friendly names for the home hero and tiles. Systems not listed fall back to ES-DE's full name. */
data class SystemInfo(
    val displayName: String,
    val shortName: String,
    val maker: String? = null,
    val year: Int? = null,
)

object SystemMeta {
    private val known = mapOf(
        "snes" to SystemInfo("Super Nintendo", "SNES", "Nintendo", 1990),
        "nes" to SystemInfo("Nintendo Entertainment System", "NES", "Nintendo", 1983),
        "fds" to SystemInfo("Famicom Disk System", "FDS", "Nintendo", 1986),
        "gb" to SystemInfo("Game Boy", "GB", "Nintendo", 1989),
        "gbc" to SystemInfo("Game Boy Color", "GBC", "Nintendo", 1998),
        "gba" to SystemInfo("Game Boy Advance", "GBA", "Nintendo", 2001),
        "n64" to SystemInfo("Nintendo 64", "N64", "Nintendo", 1996),
        "nds" to SystemInfo("Nintendo DS", "NDS", "Nintendo", 2004),
        "n3ds" to SystemInfo("Nintendo 3DS", "3DS", "Nintendo", 2011),
        "gc" to SystemInfo("GameCube", "GC", "Nintendo", 2001),
        "wii" to SystemInfo("Wii", "WII", "Nintendo", 2006),
        "virtualboy" to SystemInfo("Virtual Boy", "VB", "Nintendo", 1995),
        "psx" to SystemInfo("PlayStation", "PS1", "Sony", 1994),
        "ps2" to SystemInfo("PlayStation 2", "PS2", "Sony", 2000),
        "psp" to SystemInfo("PlayStation Portable", "PSP", "Sony", 2004),
        "psvita" to SystemInfo("PlayStation Vita", "VITA", "Sony", 2011),
        "mastersystem" to SystemInfo("Master System", "SMS", "Sega", 1985),
        "megadrive" to SystemInfo("Mega Drive", "MD", "Sega", 1988),
        "genesis" to SystemInfo("Genesis", "GEN", "Sega", 1989),
        "segacd" to SystemInfo("Sega CD", "SCD", "Sega", 1991),
        "sega32x" to SystemInfo("Sega 32X", "32X", "Sega", 1994),
        "saturn" to SystemInfo("Saturn", "SAT", "Sega", 1994),
        "dreamcast" to SystemInfo("Dreamcast", "DC", "Sega", 1998),
        "gamegear" to SystemInfo("Game Gear", "GG", "Sega", 1990),
        "pcengine" to SystemInfo("PC Engine", "PCE", "NEC", 1987),
        "tg16" to SystemInfo("TurboGrafx-16", "TG16", "NEC", 1989),
        "ngp" to SystemInfo("Neo Geo Pocket", "NGP", "SNK", 1998),
        "ngpc" to SystemInfo("Neo Geo Pocket Color", "NGPC", "SNK", 1999),
        "neogeo" to SystemInfo("Neo Geo", "NEO", "SNK", 1990),
        "arcade" to SystemInfo("Arcade", "ARC"),
        "androidapps" to SystemInfo("Android Apps", "APPS"),
        "androidgames" to SystemInfo("Android Games", "GAMES"),
        "emulators" to SystemInfo("Emulators", "EMU"),
        "atari2600" to SystemInfo("Atari 2600", "2600", "Atari", 1977),
        "atarilynx" to SystemInfo("Atari Lynx", "LYNX", "Atari", 1989),
        "wonderswan" to SystemInfo("WonderSwan", "WS", "Bandai", 1999),
        "wonderswancolor" to SystemInfo("WonderSwan Color", "WSC", "Bandai", 2000),
        "xbox" to SystemInfo("Xbox", "XBOX", "Microsoft", 2001),
        "xbox360" to SystemInfo("Xbox 360", "X360", "Microsoft", 2005),
    )

    /** Common folder names people use that differ from ES-DE's. */
    val folderAliases = mapOf(
        "ps1" to "psx",
        "playstation" to "psx",
        "sfc" to "snes",
        "supernintendo" to "snes",
        "famicom" to "nes",
        "gameboy" to "gb",
        "gameboycolor" to "gbc",
        "gameboyadvance" to "gba",
        "md" to "megadrive",
        "sms" to "mastersystem",
        "gg" to "gamegear",
        "ds" to "nds",
        "3ds" to "n3ds",
        "gamecube" to "gc",
        "microsoftxbox" to "xbox",
        "originalxbox" to "xbox",
        "ogxbox" to "xbox",
        "xboxog" to "xbox",
        "xbox1" to "xbox",
        "xboxclassic" to "xbox",
    )

    fun info(def: SystemDef): SystemInfo =
        known[def.name] ?: SystemInfo(
            displayName = def.fullName.ifBlank { def.name },
            shortName = def.name.uppercase().take(5),
        )
}
