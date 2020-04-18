package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.views.recording.AudioRecorderViewBase
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


    override fun onStartRecording() {
        println("startRecording")
    }

    override fun onFinishRecording() {
        println("finishRecording")
    }

    override fun onPauseRecording() {
        println("pauseRecording")
    }

    override fun onResumeRecording() {
        println("resumeRecording")
    }

    override fun onPlayRecordedAudio() {
        println("playRecordedAudio")
    }

    override fun onStopPlayingRecordedAudio() {
        println("stopPlayingRecordedAudio")
    }

    override fun onPausePlayingRecordedAudio() {
        println("pausePlayingRecordedAudio")
    }

    override fun onResumeRecordedAudio() {
        println("resumeRecordedAudio")
    }

    override fun clearRecordedFile() {
        println("clearRecordedFile")
    }

}