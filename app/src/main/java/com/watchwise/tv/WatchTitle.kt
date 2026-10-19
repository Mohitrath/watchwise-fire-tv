package com.watchwise.tv

data class WatchTitle(
    val id: String,
    val name: String,
    val genre: String,
    val description: String,
    val durationMinutes: Int,
    val videoUrl: String
)
