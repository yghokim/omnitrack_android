package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.support.v7.widget.AppCompatImageButton
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.Ticker
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016. 9. 27
 */
class AudioRecorderView : FrameLayout, View.OnClickListener, AudioRecordingModule.RecordingListener, AudioPlayerModule.PlayerListener, ValueAnimator.AnimatorUpdateListener {
    companion object {
        fun formatTime(seconds: Int): String {
            val min = seconds / 60
            val sec = seconds % 60

            return "$min:${String.format("%02d", sec)}"
        }

        private var currentRecorderId: String? = null
        private var currentRecordingModule: AudioRecordingModule? = null

        private var currentPlayingId: String? = null
        private var currentPlayer: AudioPlayerModule? = null


        private val isSoundPlaying: Boolean get() = currentPlayer != null && currentPlayer?.isRunning() == true

        private val isRecordingSourceFree: Boolean get() = currentRecordingModule == null

        private fun registerNewPlayer(id: String, module: AudioPlayerModule) {
            currentPlayingId = id
            currentPlayer = module
        }

        private fun cancelCurrentPlayer() {
            currentPlayer?.cancel()
            clearPlayer()
        }

        private fun clearPlayer() {
            currentPlayingId = null
            currentPlayer = null
        }

        private fun registerNewRecordingModule(id: String, module: AudioRecordingModule) {
            this.currentRecorderId = id
            this.currentRecordingModule = module
        }

        private fun cancelCurrentRecording() {
            if (!isRecordingSourceFree) {
                currentRecordingModule?.cancel()
                clearModule()
            }
        }

        private fun clearModule() {
            currentRecorderId = null
            currentRecordingModule = null
        }
    }

    enum class State {
        RECORD, RECORDING, FILE_MOUNTED
    }

    var state: State by Delegates.observable(State.RECORD) {
        prop, old, new ->
        if (old != new) {
            onSetState(new)
        }
    }

    var audioFileUri: Uri = Uri.EMPTY
        set(value) {
            if (field != value) {
                field = value
                if (value != Uri.EMPTY) {
                    state = State.FILE_MOUNTED
                    mountNewFile(value)
                } else {
                    state = State.RECORD
                }
            }
        }

    var audioLengthSeconds: Int = 60
        set(value) {
            if (field != value) {
                field = value
                refreshTimeViews()
            }
        }


    var currentAudioSeconds: Int = 0
        set(value) {
            if (field != value) {
                field = value
                refreshTimeViews()
            }
        }

    var mediaSessionId: String by Delegates.observable(UUID.randomUUID().toString()) {
        prop, old, new ->
        if (old != new) {
            if (Companion.currentRecorderId == new) {
                //cancel last recording
                //Companion.cancelCurrentRecording()
                Companion.currentRecordingModule!!.listener = this
                playBar.amplitudeTimelineProvider = Companion.currentRecordingModule
                state = State.RECORDING
                secondTicker.start()
            } else if (Companion.currentRecorderId == old) {
                if (state == State.RECORDING) {
                    state = State.RECORD
                    secondTicker.stop()
                }
            }

            if (Companion.currentPlayingId == new) {
                Companion.currentPlayer!!.listener = this
                state = State.FILE_MOUNTED
                secondTicker.start()
            } else if (Companion.currentPlayer?.listener === this) {
                Companion.currentPlayer?.listener = null
                if (state == State.FILE_MOUNTED) {
                    playBar.clear()
                    currentAudioSeconds = 0
                    setBarViewState(true)
                    secondTicker.stop()
                } else {
                    setBarViewState(false)
                }
            }
        }
    }

    val isRecording: Boolean get() {
        return Companion.currentRecorderId == mediaSessionId
    }

    val isPlaying: Boolean get() {
        return Companion.currentPlayingId == mediaSessionId
    }

    val recordingComplete = Event<Long>()
    val fileRemoved = Event<Long>()

    private val mainButton: AudioRecordingButton
    private val playBar: AudioRecorderProgressBar
    private val elapsedTimeView: TextView
    private val remainingTimeView: TextView

    private val playerButton: AppCompatImageButton

    private val playerModeTransitionAnimator: ValueAnimator

    private val secondTicker: Ticker = Ticker(1000)

    private val playerModeBarHeight: Float
    private val recorderModeBarHeight: Float

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    init {
        inflateContent(R.layout.component_audio_recorder_view, true)

        mainButton = findViewById(R.id.ui_main_button) as AudioRecordingButton
        mainButton.setOnClickListener(this)

        playBar = findViewById(R.id.ui_play_bar) as AudioRecorderProgressBar
        elapsedTimeView = findViewById(R.id.ui_time_elapsed) as TextView
        remainingTimeView = findViewById(R.id.ui_time_remain) as TextView

        playerButton = findViewById(R.id.ui_player_button) as AppCompatImageButton
        playerButton.setOnClickListener(this)

        playerModeTransitionAnimator = ValueAnimator.ofFloat(0f, 1f)
        playerModeTransitionAnimator.addUpdateListener(this)

        playerModeBarHeight = resources.getDimension(R.dimen.audio_recorder_progressbar_height_player)
        recorderModeBarHeight = resources.getDimension(R.dimen.audio_recorder_progressbar_height_recorder)

        secondTicker.tick += {
            sender, time ->
            when (state) {
                State.RECORD -> {
                }
                State.RECORDING -> {
                    if (currentRecorderId == mediaSessionId) {
                        currentAudioSeconds = ((time - Companion.currentRecordingModule!!.startedAt) / 1000).toInt()
                    } else {
                        secondTicker.stop()
                    }
                }

                State.FILE_MOUNTED -> {
                    if (currentPlayingId == mediaSessionId) {
                        currentAudioSeconds = currentPlayer!!.getCurrentProgressDuration(time) / 1000
                    } else {
                        secondTicker.stop()
                    }
                }
            }
        }

        refreshTimeViews()
    }

    private fun setBarViewState(playerMode: Boolean) {
        val lp = playBar.layoutParams
        lp.height = (.5f + if (playerMode) {
            playerModeBarHeight
        } else {
            recorderModeBarHeight
        }).toInt()
        playBar.layoutParams = lp
        playBar.requestLayout()

        playerButton.visibility = if (playerMode) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    override fun onClick(view: View) {
        if (view === mainButton) {
            when (state) {
                State.RECORD -> {
                    startRecording()
                }

                State.RECORDING -> {
                    stopRecording()
                }

                State.FILE_MOUNTED -> {
                    clearMountedFile()
                }
            }
        } else if (view === playerButton) {

            if (Companion.isSoundPlaying && Companion.currentPlayingId == mediaSessionId) {
                stopFile()
            } else {
                playFile()
                println("play")
            }
        }
    }

    override fun onAnimationUpdate(animator: ValueAnimator) {
        if (animator === playerModeTransitionAnimator) {
            val lp = playBar.layoutParams
            lp.height = (.5f + recorderModeBarHeight + ((playerModeBarHeight - recorderModeBarHeight) * animator.animatedValue as Float)).toInt()
            playBar.layoutParams = lp
            playBar.requestLayout()

            playerButton.alpha = animator.animatedValue as Float
            if (playerButton.alpha > 0f) {
                playerButton.visibility = View.VISIBLE
            } else {
                playerButton.visibility = View.INVISIBLE
            }
        }
    }


    private fun startRecording() {

        if (!Companion.isRecordingSourceFree) {
            Companion.cancelCurrentRecording()
        }

        if (Companion.isSoundPlaying) {
            Companion.cancelCurrentPlayer()
        }

        Companion.registerNewRecordingModule(mediaSessionId, AudioRecordingModule(this, context.filesDir.path + "/audio_record_${System.currentTimeMillis()}.3gp"))
        Companion.currentRecordingModule?.startAsync()

        playBar.currentProgressRatio = 0f
        playBar.amplitudeTimelineProvider = Companion.currentRecordingModule
        secondTicker.start()

        state = State.RECORDING
    }

    private fun stopRecording() {
        Companion.currentRecordingModule?.stop()
        playerModeTransitionAnimator.start()
    }

    private fun playFile() {
        if (state == State.FILE_MOUNTED) {
            if (Companion.isSoundPlaying && Companion.currentPlayingId != mediaSessionId) {
                Companion.cancelCurrentPlayer()
            }

            val newModule = AudioPlayerModule(this, audioFileUri.toString())
            registerNewPlayer(mediaSessionId, newModule)

            newModule.startAsync()

            playerButton.setImageResource(R.drawable.stop_dark)
            secondTicker.start()
/*
            if (player != null) {
                println("play")
                registerNewPlayer(mediaSessionId, player!!)
                player?.start()
            }*/
        }
    }

    private fun stopFile() {
        if (Companion.currentPlayingId == mediaSessionId) {
            Companion.currentPlayer?.stop()
        }
    }


    private fun mountNewFile(fileUri: Uri) {

        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, fileUri)
        val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toInt()
        audioLengthSeconds = duration / 1000
    }

    private fun clearMountedFile() {
        if (this.audioFileUri != Uri.EMPTY) {
            if (isSoundPlaying && this.mediaSessionId == currentPlayingId) {
                cancelCurrentPlayer()
            }

            this.audioFileUri = Uri.EMPTY
            state = State.RECORD
            playerModeTransitionAnimator.reverse()
            fileRemoved.invoke(this, 0)
        }
    }

    private fun onSetState(state: State) {
        mainButton.state = state

        when (state) {
            State.RECORD -> {
                currentAudioSeconds = 0
                audioLengthSeconds = 60
                playBar.clear()
                secondTicker.stop()
                setBarViewState(false)
            }

            State.RECORDING -> {
                setBarViewState(false)
            }

            State.FILE_MOUNTED -> {
                currentAudioSeconds = 0
                setBarViewState(true)
            }
        }
    }

    private fun refreshTimeViews() {
        elapsedTimeView.text = formatTime(currentAudioSeconds)
        remainingTimeView.text = formatTime(audioLengthSeconds - currentAudioSeconds)
    }

    override fun onAudioPlayerProgress(module: AudioPlayerModule, volume: Int) {
        if (module === currentPlayer && currentPlayingId == mediaSessionId) {
            playBar.currentProgressRatio = currentPlayer!!.getCurrentProgressRatio(System.currentTimeMillis())
        }
    }

    override fun onAudioPlayerFinished(module: AudioPlayerModule, resultPath: String?) {
        if (module === currentPlayer && currentPlayingId == mediaSessionId) {
            playBar.clear()
            secondTicker.stop()
            currentAudioSeconds = 0
            Companion.clearModule()
            playerButton.setImageResource(R.drawable.play_dark)
        }
    }


    override fun onRecordingProgress(module: AudioRecordingModule, volume: Int) {
        if (module === currentRecordingModule && currentRecorderId == mediaSessionId) {
            playBar.currentProgressRatio = currentRecordingModule!!.getCurrentProgressRatio(System.currentTimeMillis())
        }
    }

    override fun onRecordingFinished(module: AudioRecordingModule, resultPath: String?) {
        if (module === currentRecordingModule && currentRecorderId == mediaSessionId) {
            playBar.clear()
            secondTicker.stop()
            val soundLength = System.currentTimeMillis() - currentRecordingModule!!.startedAt
            Companion.clearModule()

            if (resultPath != null) {
                this.audioFileUri = Uri.parse(resultPath)
                recordingComplete.invoke(this, soundLength)
            } else {
                state = State.RECORD
            }
        }
    }

    fun dispose() {
        if (isRecording) {
            println("cancel recording.")
            cancelCurrentRecording()
        }

        if (isPlaying) {
            println("cancel recording")
            cancelCurrentPlayer()
        }
    }

}