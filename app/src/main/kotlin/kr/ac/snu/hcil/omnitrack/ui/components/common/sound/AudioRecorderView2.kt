package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.util.AttributeSet
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kr.ac.snu.hcil.omnitrack.services.OTAudioPlayService
import kr.ac.snu.hcil.omnitrack.services.OTAudioRecordService
import kr.ac.snu.hcil.omnitrack.views.recording.AudioRecorderViewBase
import java.io.File
import java.util.*
import kotlin.properties.Delegates

class AudioRecorderView2(context: Context, attr: AttributeSet?) : AudioRecorderViewBase(context, attr) {


    var mediaSessionId: String by Delegates.observable(UUID.randomUUID().toString()) { prop, old, new ->
        if (old != new) {
            /*
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
            }*/
        }
    }

    var audioTitle: String = ""


    private val playerEventReceiver: PlayerEventReceiver = PlayerEventReceiver()


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


    private fun tryStopRecordService() {
        println("stop recording: $mediaSessionId")
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(OTAudioRecordService.makeStopIntent(context, mediaSessionId))
    }

    private fun tryStopPlayingFileService() {
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(OTAudioPlayService.makeStopCommandIntent(mediaSessionId))
    }

    override fun onStartRecording() {
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
                ".wav", /* suffix */
                dirPath      /* directory */)

        val uri = Uri.Builder().scheme("file")
                .path(recordingOutputPath.path)
                .build()

        context.startService(OTAudioRecordService.makeStartIntent(context, mediaSessionId, audioTitle, uri))
    }



    override fun onFinishRecording() {
        tryStopRecordService()
    }

    override fun onPauseRecording() {
        println("pauseRecording")
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(OTAudioRecordService.makePauseIntent(mediaSessionId))
    }

    override fun onResumeRecording() {
        println("resumeRecording")
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(OTAudioRecordService.makeResumeIntent(mediaSessionId))
    }

    override fun onPlayRecordedAudio() {
        println("playRecordedAudio")
        if (OTAudioRecordService.isRecording) {
            tryStopRecordService()
        }

        context.startService(OTAudioPlayService.makePlayIntent(context, audioFileUri, mediaSessionId, audioTitle))
    }

    override fun onStopPlayingRecordedAudio() {
        println("stopPlayingRecordedAudio")
        tryStopPlayingFileService()
    }

    override fun onPausePlayingRecordedAudio() {
        println("pausePlayingRecordedAudio")
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(OTAudioPlayService.makePauseCommandIntent(mediaSessionId))
    }

    override fun onResumeRecordedAudio() {
        println("resumeRecordedAudio")
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(OTAudioPlayService.makeResumeCommandIntent(mediaSessionId))
    }

    override fun clearRecordedFile() {
        println("clearRecordedFile")

        if (this.audioFileUri != Uri.EMPTY) {
            tryStopPlayingFileService()
            this.audioFileUri = Uri.EMPTY
        }
    }


    inner class PlayerEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                OTAudioPlayService.INTENT_ACTION_EVENT_AUDIO_COMPLETED -> {
                    val sessionId = intent.getStringExtra(OTAudioPlayService.INTENT_EXTRA_SESSION_ID)
                    if (sessionId == mediaSessionId) {
                        println("audio stopped: $mediaSessionId")
                        setViewState(EMode.Player, EStatus.Idle)
                    }
                }

                OTAudioPlayService.INTENT_ACTION_EVENT_AUDIO_PROGRESS -> {
                    val sessionId = intent.getStringExtra(OTAudioPlayService.INTENT_EXTRA_SESSION_ID)
                    if (sessionId == mediaSessionId) {
                        val ratio = intent.getFloatExtra(OTAudioPlayService.INTENT_EXTRA_CURRENT_PROGRESS_RATIO, 0f)
                        val seconds = intent.getIntExtra(OTAudioPlayService.INTENT_EXTRA_CURRENT_POSITION_SECONDS, 0)
                    }
                }
                OTAudioRecordService.INTENT_ACTION_EVENT_RECORD_START_CALLBACK -> {
                    println("received recording callback.")
                    val sessionId = intent.getStringExtra(OTAudioRecordService.INTENT_EXTRA_SESSION_ID)
                    println("this sessionId: $mediaSessionId / intent session id: $sessionId")
                    if (sessionId == mediaSessionId) {
                        setViewState(EMode.Recorder, EStatus.Running)
                    }
                }

                OTAudioRecordService.INTENT_ACTION_EVENT_RECORD_PROGRESS -> {
                    val sessionId = intent.getStringExtra(OTAudioRecordService.INTENT_EXTRA_SESSION_ID)
                    val currentMilliSeconds = intent.getIntExtra(OTAudioRecordService.INTENT_EXTRA_CURRENT_PROGRESS_MilliSECONDS, 0)
                    val currentRatio = intent.getFloatExtra(OTAudioRecordService.INTENT_EXTRA_CURRENT_PROGRESS_RATIO, 0f)
                    if (sessionId == mediaSessionId) {
                        currentRecordedDurationMillis = currentMilliSeconds
                    }
                }

                OTAudioRecordService.INTENT_ACTION_EVENT_RECORD_COMPLETED -> {
                    println("delivered record completed event.")
                    val sessionId = intent.getStringExtra(OTAudioRecordService.INTENT_EXTRA_SESSION_ID)
                    val resultUri = Uri.parse(intent.getStringExtra(OTAudioRecordService.INTENT_EXTRA_RECORD_URI))
                    if (sessionId == mediaSessionId) {
                        println("this recorder view was completed - $sessionId")
                        audioFileUri = resultUri
                    }
                }
            }
        }
    }
}