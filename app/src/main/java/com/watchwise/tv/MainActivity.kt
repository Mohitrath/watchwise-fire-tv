package com.watchwise.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val dark = Color.rgb(13, 17, 28)
    private val panel = Color.rgb(26, 33, 49)
    private val accent = Color.rgb(91, 161, 255)
    private lateinit var prefs: SharedPreferences
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private var selectedGenre = "All"
    private var searchTerm = ""
    private var titles = sampleTitles()
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = dark
        window.navigationBarColor = dark
        prefs = getSharedPreferences("watchwise", MODE_PRIVATE)
        buildShell()
        render()
        loadBackendCatalogue()
    }

    private fun buildShell() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(34, 24, 34, 24); setBackgroundColor(dark) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = TextView(this).apply { text = "WATCHWISE  TV"; setTextColor(Color.WHITE); textSize = 28f; typeface = Typeface.DEFAULT_BOLD }
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        val search = EditText(this).apply {
            hint = "Search titles"; setSingleLine(true); textSize = 16f; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
            background = rounded(panel, 14); setPadding(18, 8, 18, 8); minWidth = 250
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { searchTerm = s?.toString()?.trim().orEmpty(); renderContent() }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        header.addView(search, LinearLayout.LayoutParams(300, 56))
        root.addView(header)
        status = TextView(this).apply { text = "Discover something worth watching"; setTextColor(Color.LTGRAY); textSize = 15f; setPadding(0, 12, 0, 16) }
        root.addView(status)
        val scroller = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroller.addView(content)
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun render() {
        val genres = listOf("All", "Family", "Adventure", "Education", "Nature", "Comedy", "Action", "Animation", "Drama", "Favorites")
        val genreRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        genres.forEach { genre ->
            val b = makeButton(if (selectedGenre == genre) "✓ $genre" else genre) {
                selectedGenre = genre; render()
            }
            genreRow.addView(b, LinearLayout.LayoutParams(-2, 48).apply { rightMargin = 10; bottomMargin = 12 })
        }
        content.removeAllViews()
        content.addView(genreRow)
        val hero = TextView(this).apply {
            text = "Your next favorite starts here\n\nBrowse curated videos, search by topic, or ask for a recommendation."; textSize = 24f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); setPadding(24, 20, 24, 20); background = rounded(Color.rgb(33, 49, 76), 18)
        }
        content.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 18 })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(makeButton("Recommend for me") { askRecommendations() }, LinearLayout.LayoutParams(-2, 50).apply { rightMargin = 12 })
        actions.addView(makeButton("Backend settings") { backendSettings() }, LinearLayout.LayoutParams(-2, 50))
        content.addView(actions, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 18 })
        renderContent()
    }

    private fun renderContent() {
        if (!::content.isInitialized) return
        // Preserve the first three dashboard rows and rebuild only the catalogue portion.
        while (content.childCount > 3) content.removeViewAt(content.childCount - 1)
        val favorites = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
        val filtered = titles.filter { t ->
            (selectedGenre == "All" || (selectedGenre == "Favorites" && favorites.contains(t.id)) || t.genre.equals(selectedGenre, true)) &&
                (searchTerm.isBlank() || "${t.name} ${t.genre} ${t.description}".contains(searchTerm, true))
        }
        val heading = TextView(this).apply { text = if (selectedGenre == "Favorites") "Your favorites" else "$selectedGenre picks"; textSize = 22f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0, 4, 0, 12) }
        content.addView(heading)
        if (filtered.isEmpty()) {
            content.addView(TextView(this).apply { text = "No titles found. Try another search or genre."; textSize = 17f; setTextColor(Color.LTGRAY); setPadding(0, 16, 0, 16) })
            return
        }
        filtered.forEach { title ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = rounded(panel, 14); setPadding(16, 12, 16, 12) }
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            info.addView(TextView(this).apply { text = title.name; textSize = 19f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
            info.addView(TextView(this).apply { text = "${title.genre} • ${title.durationMinutes} min\n${title.description}"; textSize = 14f; setTextColor(Color.LTGRAY); setPadding(0, 5, 12, 0) })
            row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(makeButton("Details") { showDetails(title) }, LinearLayout.LayoutParams(-2, 46).apply { leftMargin = 8 })
            row.addView(makeButton(if (favorites.contains(title.id)) "★ Saved" else "☆ Save") { toggleFavorite(title.id) }, LinearLayout.LayoutParams(-2, 46).apply { leftMargin = 8 })
            content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        }
    }

    private fun showDetails(title: WatchTitle) {
        val saved = (prefs.getStringSet("favorites", emptySet()) ?: emptySet()).contains(title.id)
        AlertDialog.Builder(this).setTitle(title.name)
            .setMessage("${title.genre} • ${title.durationMinutes} minutes\n\n${title.description}")
            .setPositiveButton(if (title.trailerUrl.isNotBlank()) "Watch Trailer" else "Play") { _, _ -> if (title.trailerUrl.isNotBlank()) openTrailer(title) else play(title) }
            .setNeutralButton(if (saved) "Remove favorite" else "Add favorite") { _, _ -> toggleFavorite(title.id) }
            .setNegativeButton("Close", null).show()
    }

    private fun openTrailer(title: WatchTitle) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(title.trailerUrl)))
        } catch (e: Exception) {
            AlertDialog.Builder(this).setTitle("Trailer unavailable").setMessage("No app can open this trailer link on this device.").setPositiveButton("OK", null).show()
        }
    }

    private fun play(title: WatchTitle) {
        prefs.edit().putString("last_title", title.id).apply()
        startActivity(Intent(this, PlayerActivity::class.java).putExtra("title", title.name).putExtra("videoUrl", title.videoUrl))
    }

    private fun toggleFavorite(id: String) {
        val favorites = (prefs.getStringSet("favorites", emptySet()) ?: emptySet()).toMutableSet()
        if (!favorites.add(id)) favorites.remove(id)
        prefs.edit().putStringSet("favorites", favorites).apply()
        render()
    }

    private fun askRecommendations() {
        val genres = if (selectedGenre == "All" || selectedGenre == "Favorites") listOf("Family", "Adventure") else listOf(selectedGenre)
        val endpoint = prefs.getString("backend_url", "https://watchwise-fire-tv.vercel.app").orEmpty().trim().trimEnd('/')
        if (endpoint.isBlank()) {
            val picks = titles.filter { it.genre in genres }.take(3).ifEmpty { titles.take(3) }
            AlertDialog.Builder(this).setTitle("Recommendations (local fallback)")
                .setMessage(picks.joinToString("\n\n") { "${it.name}\n${it.description}" })
                .setPositiveButton("Explore") { _, _ -> selectedGenre = genres.first(); render() }.setNegativeButton("Close", null).show()
            return
        }
        status.text = "Finding recommendations…"
        executor.execute {
            try {
                val connection = (URL("$endpoint/api/recommendations").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 5000; readTimeout = 9000; doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val body = JSONObject().put("genres", JSONArray(genres)).put("maxDurationMinutes", 30).put("language", "English").toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val text = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
                if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
                val result = JSONObject(text).optJSONArray("recommendations") ?: JSONArray()
                val message = (0 until result.length()).mapNotNull { i ->
                    val item = result.optJSONObject(i) ?: return@mapNotNull null
                    val t = titles.find { it.id == item.optString("titleId") } ?: return@mapNotNull null
                    "${t.name}\n${item.optString("reason", t.description)}"
                }.joinToString("\n\n").ifBlank { "No recommendations returned. Try local fallback." }
                runOnUiThread { status.text = "Recommendations ready"; AlertDialog.Builder(this).setTitle("Recommended for you").setMessage(message).setPositiveButton("OK", null).show() }
                connection.disconnect()
            } catch (e: Exception) {
                runOnUiThread { status.text = "Using local recommendations"; AlertDialog.Builder(this).setTitle("Recommendations unavailable").setMessage("The backend could not be reached. Set a reachable HTTPS URL in Backend settings or try the local catalogue.\n\n${e.message ?: "Network error"}").setPositiveButton("OK", null).show() }
            }
        }
    }


    private fun loadBackendCatalogue() {
        val endpoint = prefs.getString("backend_url", "https://watchwise-fire-tv.vercel.app").orEmpty().trim().trimEnd('/')
        if (endpoint.isBlank()) return
        status.text = "Connecting to WatchWise catalogue…"
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$endpoint/api/titles").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"; connectTimeout = 5000; readTimeout = 7000
                    setRequestProperty("Accept", "application/json")
                }
                if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val array = JSONObject(response).optJSONArray("titles") ?: JSONArray()
                val loaded = (0 until array.length()).mapNotNull { i ->
                    val item = array.optJSONObject(i) ?: return@mapNotNull null
                    val id = item.optString("id")
                    val name = item.optString("name")
                    if (id.isBlank() || name.isBlank()) return@mapNotNull null
                    WatchTitle(
                        id, name, item.optString("genre", "Featured"),
                        item.optString("description", "A WatchWise pick."),
                        item.optInt("duration", 10),
                        item.optString("videoUrl", ""),
                        item.optString("trailerUrl", "")
                    )
                }
                if (loaded.isEmpty()) throw IllegalStateException("The catalogue was empty")
                runOnUiThread {
                    titles = loaded
                    status.text = "Connected · ${loaded.size} titles from WatchWise API"
                    renderContent()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Offline catalogue · using built-in titles"
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun backendSettings() {
        val input = EditText(this).apply { setSingleLine(true); hint = "https://watchwise-fire-tv.vercel.app"; setText(prefs.getString("backend_url", "https://watchwise-fire-tv.vercel.app")); setTextColor(Color.BLACK) }
        AlertDialog.Builder(this).setTitle("Recommendation backend URL").setMessage("Enter the backend base URL without a trailing slash. For TV devices, localhost refers to the TV itself.").setView(input)
            .setPositiveButton("Save") { _, _ -> prefs.edit().putString("backend_url", input.text.toString().trim().trimEnd('/')).apply(); status.text = "Backend setting saved" }
            .setNeutralButton("Clear") { _, _ -> prefs.edit().remove("backend_url").apply(); status.text = "Using local recommendations" }
            .setNegativeButton("Cancel", null).show()
    }

    private fun makeButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label; isAllCaps = false; textSize = 14f; setTextColor(Color.WHITE); background = rounded(Color.rgb(42, 66, 101), 12)
        isFocusable = true; isFocusableInTouchMode = true; setOnClickListener { action() }
        setOnFocusChangeListener { v, hasFocus -> v.background = rounded(if (hasFocus) accent else Color.rgb(42, 66, 101), 12); setTextColor(if (hasFocus) Color.BLACK else Color.WHITE) }
        setPadding(12, 4, 12, 4)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }

    private fun sampleTitles() = listOf(
        WatchTitle("demo-001", "Big Buck Bunny", "Animation", "A playful animated short from the Blender open-movie community.", 10, "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4"),
        WatchTitle("demo-002", "Sintel · Quick Trailer", "Fantasy", "A short fantasy-animation trailer sample for testing playback.", 1, "https://test-videos.co.uk/vids/sintel/mp4/h264/720/Sintel_720_10s_2MB.mp4"),
        WatchTitle("demo-003", "Flower Stories", "Nature", "A calm close-up nature clip—ideal for testing video playback.", 1, "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"),
        WatchTitle("demo-004", "Sintel · Full Trailer", "Adventure", "The longer Sintel trailer sample for checking seeking and playback.", 1, "https://media.w3.org/2010/05/sintel/trailer.mp4"),
        WatchTitle("demo-005", "Big Buck Bunny · Compact", "Comedy", "A lightweight 360p sample clip for slower connections and quick demos.", 1, "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4"),
        WatchTitle("demo-006", "Quick Sample · 1 MB", "Demo", "A compact H.264 MP4 test clip for quick playback checks.", 1, "https://cdn.truefilesize.com/mp4/sample-1mb.mp4"),
        WatchTitle("demo-007", "HD Sample · 5 MB", "Demo", "A larger H.264 MP4 test clip for checking buffering and playback.", 1, "https://cdn.truefilesize.com/mp4/sample-5mb.mp4"),
        WatchTitle("demo-008", "Weekend Wonders", "Family", "A quick, family-friendly demo pick to test the full play flow.", 1, "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4"),
        WatchTitle("demo-009", "Nature Close-Up", "Nature", "A short CC0 flower video sample hosted by MDN.", 1, "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"),
        WatchTitle("demo-010", "HD Sample · 5 MB Plus", "Technology", "An H.264 sample source for catalogue and player testing.", 1, "https://cdn.truefilesize.com/mp4/sample-5mb.mp4"),
        WatchTitle("trailer-spider-man-bnd", "Spider-Man: Brand New Day — Trailer", "Action", "Official-trailer search for the upcoming Spider-Man film.", 3, "", "https://www.youtube.com/results?search_query=Spider-Man+Brand+New+Day+official+trailer+Sony+Pictures"),
        WatchTitle("trailer-avengers-doomsday", "Avengers: Doomsday — Trailer", "Action", "Official-trailer search for Marvel Studios' Avengers: Doomsday.", 3, "", "https://www.youtube.com/results?search_query=Avengers+Doomsday+official+trailer+Marvel+Studios"),
        WatchTitle("trailer-toy-story-5", "Toy Story 5 — Trailer", "Animation", "Official-trailer search for Pixar's Toy Story 5.", 3, "", "https://www.youtube.com/results?search_query=Toy+Story+5+official+trailer+Pixar"),
        WatchTitle("trailer-moana-2026", "Moana — Trailer", "Adventure", "Official-trailer search for the upcoming Moana movie.", 3, "", "https://www.youtube.com/results?search_query=Moana+2026+official+trailer+Disney"),
        WatchTitle("trailer-street-fighter", "Street Fighter (2026) — Trailer", "Action", "Official-trailer search for the 2026 Street Fighter movie.", 3, "", "https://www.youtube.com/results?search_query=Street+Fighter+2026+official+trailer+Paramount"),
        WatchTitle("trailer-hunger-games-sunrise", "The Hunger Games: Sunrise on the Reaping — Trailer", "Drama", "Official-trailer search for the next Hunger Games film.", 3, "", "https://www.youtube.com/results?search_query=Hunger+Games+Sunrise+on+the+Reaping+official+trailer+Lionsgate"),
        WatchTitle("trailer-zootopia-2", "Zootopia 2 — Trailer", "Animation", "Official-trailer search for Disney Animation's Zootopia 2.", 3, "", "https://www.youtube.com/results?search_query=Zootopia+2+official+trailer+Disney"),
        WatchTitle("trailer-avengers-endgame-encore", "Avengers: Endgame — Encore Trailer", "Action", "Trailer search for the Endgame Encore release.", 3, "", "https://www.youtube.com/results?search_query=Avengers+Endgame+Encore+official+trailer+Marvel")
    )

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
