package com.minimal.launcher

enum class Screen {
    HOME,
    APPS,
    STATS,
    SETTINGS_SEARCH
}

data class AppInfo(
    val label: String,
    val packageName: String,
    val labelLower: String = label.lowercase()
)
