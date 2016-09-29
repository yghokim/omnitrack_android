package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.media.MediaPlayer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Young-Ho Kim on 2016-09-29.
 */
class AudioPlayerModule(var listener: PlayerListener?, val filePath: String, progressTerm: Int = 200) : AAudioModule(progressTerm), MediaPlayer.OnCompletionListener {

    interface PlayerListener {
        fun onAudioPlayerProgress(module: AudioPlayerModule, volume: Int)
        fun onAudioPlayerFinished(module: AudioPlayerModule, resultPath: String?)
    }

    private val player: MediaPlayer

    private val isReady: AtomicBoolean = AtomicBoolean(false)

    private var isPlaying: Boolean = false

    init {
        player = MediaPlayer()
        player.setDataSource(filePath)
        player.setOnCompletionListener(this)
    }


    override fun onCompletion(p0: MediaPlayer?) {
        stop()
    }

    override fun getCurrentProgressRatio(now: Long): Float {
        return (if (isRunning()) {
            player.currentPosition
        } else 0) / player.duration.toFloat()
    }

    override fun getCurrentProgressDuration(now: Long): Int {
        if (isRunning()) {
            return player.currentPosition
        } else return 0
    }

    override fun onTick(time: Long) {
        if (isReady.get() == true) {
            listener?.onAudioPlayerProgress(this, 0)
        }
    }

    override fun onStart() {
        Thread {
            player.prepare()
            println("sound player prepared.")
            isReady.set(true)
            player.start()
            isPlaying = true
        }.start()
    }

    override fun onStop(cancel: Boolean) {
        player.stop()
        player.reset()
        player.release()
        isPlaying = false
        if (cancel != true) {
            listener?.onAudioPlayerFinished(this, filePath)
        } else {
            listener?.onAudioPlayerFinished(this, null)
        }
    }

    override fun isRunning(): Boolean {
        return isPlaying
    }
}