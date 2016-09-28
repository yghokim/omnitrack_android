package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.AudioRecordingModule
import kr.ac.snu.hcil.omnitrack.utils.Ticker
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by younghokim on 2016. 9. 27..
 */
class AudioRecorderView : FrameLayout, View.OnClickListener, AudioRecordingModule.RecordingListener {

    companion object {
        fun formatTime(seconds: Int): String {
            val min = seconds / 60
            val sec = seconds % 60

            return "$min:${String.format("%02d", sec)}"
        }

        private var currentRecorderId: String? = null
        private var currentRecordingModule: AudioRecordingModule? = null

        private val isRecordingSourceFree: Boolean get() = currentRecordingModule == null

        private fun assignNewRecordingModule(id: String, module: AudioRecordingModule) {
            this.currentRecorderId = id
            this.currentRecordingModule = module
        }

        private fun cancelCurrentModule() {
            if (!isRecordingSourceFree) {
                currentRecordingModule?.stop(true)
                clearModule()
            }
        }

        private fun clearModule() {
            currentRecorderId = null
            currentRecordingModule = null
        }
    }

    enum class State {
        RECORD, RECORDING, REMOVE
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
                    state = State.REMOVE
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

    var recordingId: String by Delegates.observable(UUID.randomUUID().toString()) {
        prop, old, new ->
        if (old != new) {
            if (Companion.currentRecorderId == old) {
                //cancel last recording
                Companion.cancelCurrentModule()
            }
        }
    }

    private val mainButton: AudioRecordingButton
    private val playBar: AudioRecorderProgressBar
    private val elapsedTimeView: TextView
    private val remainingTimeView: TextView

    private val secondTicker: Ticker = Ticker(1000)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    init {
        inflateContent(R.layout.component_audio_recorder_view, true)

        mainButton = findViewById(R.id.ui_main_button) as AudioRecordingButton
        mainButton.setOnClickListener(this)

        playBar = findViewById(R.id.ui_play_bar) as AudioRecorderProgressBar
        elapsedTimeView = findViewById(R.id.ui_time_elapsed) as TextView
        remainingTimeView = findViewById(R.id.ui_time_remain) as TextView

        secondTicker.tick += {
            sender, time ->
            if (currentRecorderId == recordingId) {
                currentAudioSeconds = ((time - Companion.currentRecordingModule!!.recordingStartedAt) / 1000).toInt()
            } else {
                secondTicker.stop()
            }
        }

        refreshTimeViews()
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

                State.REMOVE -> {
                    clearMountedFile()
                }
            }
        }
    }

    private fun startRecording() {

        if (!Companion.isRecordingSourceFree) {
            Companion.cancelCurrentModule()
        }

        Companion.assignNewRecordingModule(recordingId, AudioRecordingModule(this, context.filesDir.path + "/haha.3gp"))
        Companion.currentRecordingModule?.startAsync()

        playBar.currentProgressRatio = 0f
        secondTicker.start()

        state = State.RECORDING
    }

    private fun stopRecording() {
        Companion.currentRecordingModule?.stop(false)
    }

    private fun clearMountedFile() {
        this.audioFileUri = Uri.EMPTY
        state = State.RECORD
    }

    private fun onSetState(state: State) {
        mainButton.state = state

        when (state) {
            State.RECORD -> {
                currentAudioSeconds = 0
                audioLengthSeconds = 60
            }

            State.RECORDING -> {

            }

            State.REMOVE -> {
                currentAudioSeconds = 0
                audioLengthSeconds = 60
            }
        }
    }

    private fun refreshTimeViews() {
        elapsedTimeView.text = formatTime(currentAudioSeconds)
        remainingTimeView.text = formatTime(audioLengthSeconds - currentAudioSeconds)
    }


    override fun onRecordingProgress(module: AudioRecordingModule, volume: Int) {
        if (module === currentRecordingModule) {
            playBar.currentProgressRatio = (System.currentTimeMillis() - currentRecordingModule!!.recordingStartedAt).toFloat() / (audioLengthSeconds * 1000)
            playBar.putVolumeBar(volume = volume)
        }
    }

    override fun onRecordingFinished(module: AudioRecordingModule, resultPath: String?) {
        if (module === currentRecordingModule) {
            playBar.clear()
            secondTicker.stop()
            Companion.clearModule()

            if (resultPath != null) {
                this.audioFileUri = Uri.parse(resultPath)
            } else {
                state = State.RECORD
            }
        }
    }

}