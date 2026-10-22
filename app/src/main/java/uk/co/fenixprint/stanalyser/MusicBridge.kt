package uk.co.fenixprint.stanalyser

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings

object MusicBridge {
    @Volatile var title: String = "ABOVE & BEYOND - BLUE MONDAY"
    @Volatile var playing: Boolean = true
    @Volatile private var controller: MediaController? = null

    fun enabled(context: Context): Boolean =
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.contains(context.packageName) == true

    fun refresh(context: Context) {
        if (!enabled(context)) return
        try {
            val mgr = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val component = ComponentName(context, MusicListener::class.java)
            val sessions = mgr.getActiveSessions(component)
            val candidate = sessions.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: sessions.firstOrNull { it.metadata != null }
            controller = candidate
            candidate?.metadata?.let { md ->
                val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
                val song = md.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
                if (artist.isNotBlank() || song.isNotBlank())
                    title = listOf(artist, song).filter { it.isNotBlank() }.joinToString(" - ").uppercase()
            }
            playing = candidate?.playbackState?.state == PlaybackState.STATE_PLAYING
        } catch (_: Exception) { }
    }

    fun previous() { controller?.transportControls?.skipToPrevious() }
    fun next() { controller?.transportControls?.skipToNext() }
    fun toggle() {
        if (playing) controller?.transportControls?.pause()
        else controller?.transportControls?.play()
    }
}
