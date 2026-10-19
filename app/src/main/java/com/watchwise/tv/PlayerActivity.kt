package com.watchwise.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.net.URI

class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    private var webView: WebView? = null
    private var playerView: PlayerView? = null
    private lateinit var message: TextView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("title") ?: "WatchWise video"
        val videoUrl = intent.getStringExtra("videoUrl").orEmpty()
        val trailerUrl = intent.getStringExtra("trailerUrl").orEmpty()
        val layout = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        message = TextView(this).apply {
            text = "Loading $title…"; textSize = 18f; setTextColor(Color.WHITE)
            setBackgroundColor(0xAA000000.toInt()); gravity = Gravity.CENTER
            setPadding(24, 16, 24, 16)
            layoutParams = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM)
        }

        if (trailerUrl.isNotBlank()) {
            val web = WebView(this)
            webView = web
            web.settings.javaScriptEnabled = true
            web.settings.domStorageEnabled = true
            web.settings.mediaPlaybackRequiresUserGesture = true
            web.settings.loadsImagesAutomatically = true
            web.settings.setSupportMultipleWindows(false)
            web.webChromeClient = WebChromeClient()
            web.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) { message.text = "$title · YouTube trailer" }
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val host = request?.url?.host.orEmpty()
                    return !(host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be" ||
                        host.endsWith(".youtube-nocookie.com") || host == "google.com" || host.endsWith(".google.com"))
                }
            }
            layout.addView(web, FrameLayout.LayoutParams(-1, -1))
            layout.addView(message)
            setContentView(layout)
            val id = youtubeId(trailerUrl)
            if (id != null) {
                val html = "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><style>html,body{margin:0;background:#000;width:100%;height:100%;overflow:hidden}iframe{position:absolute;inset:0;width:100%;height:100%;border:0}</style></head><body><iframe src=\"https://www.youtube-nocookie.com/embed/$id?playsinline=1&autoplay=0&rel=0&origin=https%3A%2F%2Fwatchwise-fire-tv.vercel.app\" title=\"YouTube trailer\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" allowfullscreen referrerpolicy=\"strict-origin-when-cross-origin\"></iframe></body></html>"
                web.loadDataWithBaseURL("https://watchwise-fire-tv.vercel.app/", html, "text/html", "UTF-8", null)
            } else {
                web.loadUrl(trailerUrl)
            }
            return
        }

        val view = PlayerView(this).apply { useController = true; layoutParams = FrameLayout.LayoutParams(-1, -1) }
        playerView = view
        layout.addView(view)
        layout.addView(message)
        setContentView(layout)
        if (videoUrl.isBlank()) { message.text = "No playable video URL is configured for this title. Press Back to return."; return }
        try {
            player = ExoPlayer.Builder(this).build().also { exo ->
                view.player = exo
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        message.text = when (playbackState) {
                            Player.STATE_BUFFERING -> "Buffering…"
                            Player.STATE_READY -> title
                            Player.STATE_ENDED -> "Playback finished — press Back to return"
                            else -> title
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) { message.text = "Playback failed: ${error.errorCodeName}. Check network and media URL, then press Back." }
                })
                exo.setMediaItem(MediaItem.fromUri(videoUrl))
                exo.prepare()
                exo.playWhenReady = true
            }
        } catch (e: Exception) { message.text = "Unable to start playback: ${e.message ?: "unknown error"}" }
    }

    private fun youtubeId(url: String): String? = try {
        val uri = URI(url)
        when {
            uri.host == "youtu.be" -> uri.path.trim('/').substringBefore('/').takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,}")) }
            uri.path == "/watch" -> uri.rawQuery.orEmpty().split("&").firstOrNull { it.startsWith("v=") }?.substringAfter("v=")?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,}")) }
            uri.path.startsWith("/embed/") -> uri.path.removePrefix("/embed/").substringBefore('/').takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,}")) }
            else -> null
        }
    } catch (_: Exception) { null }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean { if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }; return super.onKeyDown(keyCode, event) }
    override fun onStop() { playerView?.player = null; player?.release(); player = null; super.onStop() }
    override fun onDestroy() { webView?.apply { stopLoading(); loadUrl("about:blank"); destroy() }; webView = null; super.onDestroy() }
}
