package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.events.Event
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.services.OTAudioPlayService
import kr.ac.snu.hcil.omnitrack.services.OTAudioRecordService
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016. 9. 27
 */
class AudioRecorderView : FrameLayout, View.OnClickListener, ValueAnimator.AnimatorUpdateListener {
    companion object {

        private val TAG = "AudioRecorderView"

        fun formatTime(seconds: Int): String {
            val min = seconds / 60
            val sec = seconds % 60

            return "$min:${String.format("%02d", sec)}"
        }
    }

    enum class State {
        RECORD, RECORDING, FILE_MOUNTED
    }

    val stateObservable = BehaviorSubject.createDefault<State>(State.RECORD)

    var state: State
        get() {
            return stateObservable.value ?: State.RECORD
        }
        set(value) {
            if (stateObservable.value != value) {
                stateObservable.onNext(value)
                onSetState(value)
            }
        }

    private fun onSetState(state: State) {
        mainButton.state = state

        when (state) {
            State.RECORD -> {
                audioLengthSeconds = 60
                playBar.clear()
                setBarViewState(false)
            }

            State.RECORDING -> {
                setBarViewState(false)
            }

            State.FILE_MOUNTED -> {
                setBarViewState(true)
            }
        }
    }

    var audioFileUri: Uri = Uri.EMPTY
        set(value) {
            field = value
            if (value != Uri.EMPTY) {
                if (File(value.path).exists()) {
                    state = State.FILE_MOUNTED
                    playerModeTransitionAnimator.start()
                    mountNewFile(value)
                } else {
                    println("recorded file does not exists.")
                    state = State.RECORD
                }
            } else {
                state = State.RECORD
            }

            audioFileUriChanged.invoke(this, value)
        }

    private var currentRecordingUri: Uri? = null

    var audioLengthSeconds: Int = 60
        set(value) {
            if (field != value) {
                field = value
                refreshTimeViews(0)
            }
        }

    var audioTitle: String = ""

    var recordingOutputDirectoryPathOverride: File? = null

    val audioFileUriChanged = Event<Uri>()

    var mediaSessionId: String by Delegates.observable(UUID.randomUUID().toString()) {
        prop, old, new ->
        if (old != new) {
            if (OTAudioRecordService.currentSessionId == new) {
                playBar.amplitudeTimelineProvider = OTAudioRecordService.currentRecordingModule
                state = State.RECORDING
                return@observable

            } else if (OTAudioRecordService.currentSessionId == old) {
                if (state == State.RECORDING) {
                    tryStopRecordService()
                }
                return@observable
            }

            if (OTAudioPlayService.currentSessionId == new) {
                state = State.FILE_MOUNTED
                playBar.currentProgressRatio = OTAudioPlayService.currentProgressRatio
                refreshTimeViews(OTAudioPlayService.currentPlayPositionSecond)
                return@observable
            } else {
                println("current audio play session is different.")

                if (state == State.FILE_MOUNTED) {
                    playBar.clear()
                    refreshTimeViews(0)
                    setBarViewState(true)
                } else {
                    setBarViewState(false)
                }
            }
        }
    }

    val fileRemoved = Event<Long>()

    private val mainButton: AudioRecordingButton
    private val playBar: AudioRecorderProgressBar
    private val elapsedTimeView: TextView
    private val remainingTimeView: TextView

    private val playerButton: AppCompatImageButton

    private val playerModeTransitionAnimator: ValueAnimator

    private val playerModeBarHeight: Float
    private val recorderModeBarHeight: Float

    private val playerEventReceiver: PlayerEventReceiver = PlayerEventReceiver()

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

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
        refreshTimeViews(0)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        println("attach broadcastreceiver for recorder view")
        LocalBroadcastManager.getInstance(context)
                .registerReceiver(playerEventReceiver, IntentFilter().apply {
                    addAction(OTAudioPlayService.INTENT_ACTION_EVENT_AUDIO_COMPLETED)
                    addAction(OTAudioPlayService.INTENT_ACTION_EVENT_AUDIO_PROGRESS)
                    addAction(OTAudioRecordService.INTENT_ACTION_EVENT_RECORD_START_CALLBACK)
                    addAction(OTAudioRecordService.INTENT_ACTION_EVENT_RECORD_PROGRESS)
                    addAction(OTAudioRecordService.INTENT_ACTION_EVENT_RECORD_COMPLETED)
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
                    tryStartRecordService()
                }

                State.RECORDING -> {
                    LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(OTAudioRecordService.makeStopIntent(context, mediaSessionId))
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


    private fun tryStartRecordService() {

        println("start recording: $mediaSessionId")

        if (OTAudioRecordService.isRecording) {
            tryStopRecordService()
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

        context.startService(OTAudioRecordService.makeStartIntent(context, mediaSessionId, audioTitle, uri))
    }

    private fun tryStopRecordService() {
        println("stop recording: $mediaSessionId")
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(OTAudioRecordService.makeStopIntent(context, mediaSessionId))
    }

    private fun playFile() {
        if (state == State.FILE_MOUNTED) {

            if (OTAudioRecordService.isRecording) {
                tryStopRecordService()
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

        println("fileUri: $fileUri")
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

    private fun refreshTimeViews(currentAudioSeconds: Int) {
        elapsedTimeView.text = formatTime(currentAudioSeconds)
        remainingTimeView.text = formatTime(audioLengthSeconds - currentAudioSeconds)
    }

    fun stopRecordingAndApplyUri(): Single<Nullable<Uri>> {
        if (state == State.RECORDING) {
            tryStopRecordService()
            return stateObservable.filter {
                println("state: $it")
                it == State.FILE_MOUNTED
            }.firstOrError().subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).timeout(2, TimeUnit.SECONDS).onErrorReturn { State.FILE_MOUNTED }.map { state -> Nullable(audioFileUri) }
        } else {
            return Single.just(Nullable(audioFileUri))
        }
    }

    fun dispose() {

    }

    inner class PlayerEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                OTAudioPlayService.INTENT_ACTION_EVENT_AUDIO_COMPLETED -> {
                    val sessionId = intent.getStringExtra(OTAudioPlayService.INTENT_EXTRA_SESSION_ID)
                    if (sessionId == mediaSessionId) {
                        println("audio stopped: $mediaSessionId")
                        playBar.clear()
                        refreshTimeViews(0)
                        playerButton.setImageResource(R.drawable.play_dark)
                    }
                }

                OTAudioPlayService.INTENT_ACTION_EVENT_AUDIO_PROGRESS -> {
                    val sessionId = intent.getStringExtra(OTAudioPlayService.INTENT_EXTRA_SESSION_ID)
                    if (sessionId == mediaSessionId) {
                        val ratio = intent.getFloatExtra(OTAudioPlayService.INTENT_EXTRA_CURRENT_PROGRESS_RATIO, 0f)
                        playBar.currentProgressRatio = ratio
                        val seconds = intent.getIntExtra(OTAudioPlayService.INTENT_EXTRA_CURRENT_POSITION_SECONDS, 0)
                        refreshTimeViews(seconds)
                    }
                }
                OTAudioRecordService.INTENT_ACTION_EVENT_RECORD_START_CALLBACK -> {
                    println("received recording callback.")
                    val sessionId = intent.getStringExtra(OTAudioRecordService.INTENT_EXTRA_SESSION_ID)
                    println("this sessionId: $mediaSessionId / intent session id: $sessionId")
                    if (sessionId == mediaSessionId) {
                        playBar.currentProgressRatio = 0f
                        playBar.amplitudeTimelineProvider = OTAudioRecordService.currentRecordingModule
                        state = State.RECORDING
                        currentRecordingUri = intent.getStringExtra(OTAudioRecordService.INTENT_EXTRA_RECORD_URI)?.let { Uri.parse(it) }
                    } else {

                    }
                }

                OTAudioRecordService.INTENT_ACTION_EVENT_RECORD_PROGRESS -> {
                    val sessionId = intent.getStringExtra(OTAudioRecordService.INTENT_EXTRA_SESSION_ID)
                    val currentSeconds = intent.getIntExtra(OTAudioRecordService.INTENT_EXTRA_CURRENT_PROGRESS_SECONDS, 0)
                    val currentRatio = intent.getFloatExtra(OTAudioRecordService.INTENT_EXTRA_CURRENT_PROGRESS_RATIO, 0f)
                    if (sessionId == mediaSessionId) {
                        refreshTimeViews(currentSeconds)
                        playBar.currentProgressRatio = currentRatio
                    }
                }

                OTAudioRecordService.INTENT_ACTION_EVENT_RECORD_COMPLETED -> {
                    println("delivered record completed event.")
                    val sessionId = intent.getStringExtra(OTAudioRecordService.INTENT_EXTRA_SESSION_ID)
                    val resultUri = Uri.parse(intent.getStringExtra(OTAudioRecordService.INTENT_EXTRA_RECORD_URI))
                    if (sessionId == mediaSessionId) {
                        println("this recorder view was completed - $sessionId")
                        refreshTimeViews(0)
                        playBar.clear()
                        audioFileUri = resultUri
                    }
                }
            }
        }
    }
}