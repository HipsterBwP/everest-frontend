package io.github.hipsterbwp.rp5launcher.ui.screens

fun formatPlaytime(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 1 -> "<1m"
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

fun timesPlayed(count: Int): String = if (count == 1) "Played once" else "Played $count×"

fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.0f MB".format(bytes / (1L shl 20).toDouble())
    bytes > 0 -> "%.0f KB".format(bytes / 1024.0)
    else -> ""
}
