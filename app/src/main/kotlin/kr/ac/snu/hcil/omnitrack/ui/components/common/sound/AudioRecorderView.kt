package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import kotlin.properties.Delegates

/**
 * Created by younghokim on 2016. 9. 27..
 */
class AudioRecorderView : FrameLayout, View.OnClickListener {

    companion object {
        fun formatTime(seconds: Int): String {
            val min = seconds / 60
            val sec = seconds % 60

            return "$min:${String.format("%02d", sec)}"
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


    private val mainButton: AudioRecordingButton
    private val playBar: View
    private val elapsedTimeView: TextView
    private val remainingTimeView: TextView

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    init {
        inflateContent(R.layout.component_audio_recorder_view, true)

        mainButton = findViewById(R.id.ui_main_button) as AudioRecordingButton
        mainButton.setOnClickListener(this)

        playBar = findViewById(R.id.ui_play_bar)
        elapsedTimeView = findViewById(R.id.ui_time_elapsed) as TextView
        remainingTimeView = findViewById(R.id.ui_time_remain) as TextView

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
        state = State.RECORDING
    }

    private fun stopRecording() {
        state = State.REMOVE
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

            }
        }
    }

    private fun refreshTimeViews() {

        elapsedTimeView.text = formatTime(currentAudioSeconds)
        remainingTimeView.text = formatTime(audioLengthSeconds - currentAudioSeconds)
    }
}