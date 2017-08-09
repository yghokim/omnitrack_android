package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.AppCompatImageButton
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.services.OTAudioPlayService
import kr.ac.snu.hcil.omnitrack.utils.Ticker
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import java.io.File
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016. 9. 27
 */
class AudioRecorderView : FrameLayout, View.OnClickListener, AudioRecordingModule.RecordingListener, ValueAnimator.AnimatorUpdateListener {
    companion object {
        fun formatTime(seconds: Int): String {
            val min = seconds / 60
            val sec = seconds % 60

            return "$min:${String.format("%02d", sec)}"
        }

        private var currentRecorderId: String? = null
        private var currentRecordingModule: AudioRecordingModule? = null

        /*
        private var currentPlayingId: String? = null
        private var currentPlayer: AudioPlayerModule? = null


        private val isSoundPlaying: Boolean get() = currentPlayer != null && currentPlayer?.isRunning() == true*/

        private val isRecordingSourceFree: Boolean get() = currentRecordingModule == null
/*
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
        }*/

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
                    if (File(value.path).exists()) {
                        state = State.FILE_MOUNTED
                        mountNewFile(value)
                    } else
                    {
                        println("recorded file does not exists.")
                        state = State.RECORD
                    }
                } else {
                    state = State.RECORD
                }

                audioFileUriChanged.invoke(this, value)
            }
        }

    var audioLengthSeconds: Int = 60
        set(value) {
            if (field != value) {
                field = value
                refreshTimeViews()
            }
        }

    var audioTitle: String = ""

    var recordingOutputDirectoryPathOverride: File? = null

    val audioFileUriChanged = Event<Uri>()

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
                return@observable
            } else if (Companion.currentRecorderId == old) {
                if (state == State.RECORDING) {
                    state = State.RECORD
                    secondTicker.stop()
                }
                return@observable
            }

            if (OTAudioPlayService.currentSessionId == new) {
                //Companion.currentPlayer!!.listener = this
                state = State.FILE_MOUNTED
                playBar.currentProgressRatio = OTAudioPlayService.currentProgressRatio
                currentAudioSeconds = OTAudioPlayService.currentPlayPositionSecond
                return@observable
            } else {
                println("current audio play session is different.")
                ///Companion.currentPlayer?.listener = null

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
        return OTAudioPlayService.currentSessionId == mediaSessionId
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

    private val playerEventReceiver: PlayerEventReceiver = PlayerEventReceiver()

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    init {
        inflateContent(R.layout.component_audio_recorder_view, true)

        mainButton = findViewById(R.id.ui_main_button)
        mainButton.setOnClickListener(this)

        playBar = findViewById(R.id.ui_play_bar)
        elapsedTimeView = findViewById(R.id.ui_time_elapsed)
        remainingTimeView = findViewById(R.id.ui_time_remain)

        playerButton = findViewById(R.id.ui_player_button)
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
            /*
            State.FILE_MOUNTED -> {
                if (currentPlayingId == mediaSessionId) {
                    currentAudioSeconds = currentPlayer!!.getCurrentProgressDuration(time) / 1000
                } else {
                    secondTicker.stop()
                }
            }*/
            }
        }

        refreshTimeViews()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LocalBroadcastManager.getInstance(context)
                .registerReceiver(playerEventReceiver, IntentFilter().apply {
                    this.addAction(OTAudioPlayService.INTENT_ACTION_EVENT_AUDIO_COMPLETED)
                    this.addAction(OTAudioPlayService.INTENT_ACTION_EVENT_AUDIO_PROGRESS)
                })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(playerEventReceiver)
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
            if (OTAudioPlayService.isSoundPlaying && OTAudioPlayService.currentSessionId == mediaSessionId) {
                stopFile()
            } else playFile()
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


        println("start recording: ${mediaSessionId}")

        if (!Companion.isRecordingSourceFree) {
            Companion.cancelCurrentRecording()
        }

        if (OTAudioPlayService.isSoundPlaying) {
            context.stopService(Intent(context, OTAudioPlayService::class.java))
        }

        val dirPath: File = recordingOutputDirectoryPathOverride ?: context.filesDir
        val recordingOutputPath = File.createTempFile(
                "audio_record_${System.currentTimeMillis()}", /* prefix */
                ".3gp", /* suffix */
                dirPath      /* directory */)

        val uri = Uri.Builder().scheme("file")
                .path(recordingOutputPath.path)
                .build()

        Companion.registerNewRecordingModule(mediaSessionId, AudioRecordingModule(this, uri))
        Companion.currentRecordingModule?.startAsync()

        playBar.currentProgressRatio = 0f
        playBar.amplitudeTimelineProvider = Companion.currentRecordingModule
        secondTicker.start()

        state = State.RECORDING
    }

    private fun stopRecording() {
        println("stop recording: ${mediaSessionId}")
        Companion.currentRecordingModule?.stop()
        playerModeTransitionAnimator.start()
    }

    private fun playFile() {
        if (state == State.FILE_MOUNTED) {

            if (!Companion.isRecordingSourceFree) {
                Companion.cancelCurrentRecording()
            }

            context.startService(OTAudioPlayService.makePlayIntent(context, audioFileUri, mediaSessionId, audioTitle))

            playerButton.setImageResource(R.drawable.stop_dark)
/*
            if (player != null) {
                println("play")
                registerNewPlayer(mediaSessionId, player!!)
                player?.start()
            }*/
        }
    }

    private fun stopFile() {
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(OTAudioPlayService.makeStopCommandIntent(mediaSessionId))
    }


    private fun mountNewFile(fileUri: Uri) {

        println("fileUri: ${fileUri.toString()}")
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, fileUri)
        val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toInt()
        audioLengthSeconds = duration / 1000
    }

    private fun clearMountedFile() {
        if (this.audioFileUri != Uri.EMPTY) {
            stopFile()

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

    override fun onRecordingProgress(module: AudioRecordingModule, volume: Int) {
        if (module === currentRecordingModule && currentRecorderId == mediaSessionId) {
            playBar.currentProgressRatio = currentRecordingModule!!.getCurrentProgressRatio(System.currentTimeMillis())
        }
    }

    override fun onRecordingFinished(module: AudioRecordingModule, resultUri: Uri?) {
        if (module === currentRecordingModule && currentRecorderId == mediaSessionId) {
            playBar.clear()
            secondTicker.stop()
            val soundLength = System.currentTimeMillis() - currentRecordingModule!!.startedAt
            Companion.clearModule()

            if (resultUri != null) {
                this.audioFileUri = resultUri
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
            // println("cancel recording")
            // cancelCurrentPlayer()
            //stopFile()
        }
    }

    inner class PlayerEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                OTAudioPlayService.INTENT_ACTION_EVENT_AUDIO_COMPLETED -> {
                    val sessionId = intent.getStringExtra(OTAudioPlayService.INTENT_EXTRA_SESSION_ID)
                    if (sessionId == mediaSessionId) {
                        println("audio stopped: ${mediaSessionId}")
                        playBar.clear()
                        currentAudioSeconds = 0
                        playerButton.setImageResource(R.drawable.play_dark)
                    }
                }

                OTAudioPlayService.INTENT_ACTION_EVENT_AUDIO_PROGRESS -> {
                    val sessionId = intent.getStringExtra(OTAudioPlayService.INTENT_EXTRA_SESSION_ID)
                    if (sessionId == mediaSessionId) {
                        val ratio = intent.getFloatExtra(OTAudioPlayService.INTENT_EXTRA_CURRENT_PROGRESS_RATIO, 0f)
                        playBar.currentProgressRatio = ratio
                        val seconds = intent.getIntExtra(OTAudioPlayService.INTENT_EXTRA_CURRENT_POSITION_SECONDS, 0)
                        currentAudioSeconds = seconds
                    }
                }
            }
        }

    }
}