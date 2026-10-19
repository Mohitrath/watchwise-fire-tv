package com.watchwise.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
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
        val genres = listOf("All", "Family", "Adventure", "Education", "Nature", "Comedy", "Favorites")
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
            .setPositiveButton("Play") { _, _ -> play(title) }
            .setNeutralButton(if (saved) "Remove favorite" else "Add favorite") { _, _ -> toggleFavorite(title.id) }
            .setNegativeButton("Close", null).show()
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
        val endpoint = prefs.getString("backend_url", "").orEmpty().trim().trimEnd('/')
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

    private fun backendSettings() {
        val input = EditText(this).apply { setSingleLine(true); hint = "https://your-backend.example.com"; setText(prefs.getString("backend_url", "")); setTextColor(Color.BLACK) }
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
        WatchTitle("demo-001", "The Little Inventor", "Education", "A curious young inventor learns through experiments and teamwork.", 8, "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
        WatchTitle("demo-002", "Mountain Quest", "Adventure", "Explore breathtaking landscapes on a light-hearted journey.", 10, "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
        WatchTitle("demo-003", "Ocean Neighbors", "Nature", "Meet fascinating creatures living beneath the waves.", 12, "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"),
        WatchTitle("demo-004", "Family Game Night", "Family", "A cheerful short about friends solving a puzzle together.", 6, "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"),
        WatchTitle("demo-005", "Tiny Trailblazers", "Adventure", "Young explorers solve clues in a colourful outdoor adventure.", 14, "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"),
        WatchTitle("demo-006", "Laugh Lab", "Comedy", "Everyday science turns into a series of funny surprises.", 7, "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4")
    )

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
