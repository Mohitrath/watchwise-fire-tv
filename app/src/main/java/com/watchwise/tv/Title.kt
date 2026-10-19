package com.watchwise.tv

import org.json.JSONObject

data class WatchTitle(
    val id: String,
    val name: String,
    val genre: String,
    val description: String,
    val durationMinutes: Int,
    val videoUrl: String
) {
    companion object {
        fun fromJson(json: JSONObject) = WatchTitle(
            id = json.optString("id"),
            name = json.optString("name"),
            genre = json.optString("genre"),
            description = json.optString("description"),
            durationMinutes = json.optInt("duration", 0),
            videoUrl = json.optString("videoUrl")
        )
    }
}
