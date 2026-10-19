package com.watchwise.tv

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var message: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("title") ?: "WatchWise video"
        val url = intent.getStringExtra("videoUrl").orEmpty()
        val layout = FrameLayout(this)
        playerView = PlayerView(this).apply { useController = true; layoutParams = FrameLayout.LayoutParams(-1, -1) }
        message = TextView(this).apply {
            text = "Loading $title…"; textSize = 18f; setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(0xAA000000.toInt()); gravity = Gravity.CENTER; setPadding(24, 16, 24, 16)
            layoutParams = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM)
        }
        layout.addView(playerView)
        layout.addView(message)
        setContentView(layout)
        if (url.isBlank()) {
            message.text = "No video URL is configured for this title. Press Back to return."
            return
        }
        try {
            player = ExoPlayer.Builder(this).build().also { exo ->
                playerView.player = exo
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        message.text = when (playbackState) {
                            Player.STATE_BUFFERING -> "Buffering…"
                            Player.STATE_READY -> title
                            Player.STATE_ENDED -> "Playback finished — press Back to return"
                            else -> title
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        message.text = "Playback failed: ${error.errorCodeName}. Check network and media URL, then press Back."
                    }
                })
                exo.setMediaItem(MediaItem.fromUri(url))
                exo.prepare()
                exo.playWhenReady = true
            }
        } catch (e: Exception) {
            message.text = "Unable to start playback: ${e.message ?: "unknown error"}"
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onStop() {
        playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }
}
