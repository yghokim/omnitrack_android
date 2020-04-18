package kr.ac.snu.hcil.omnitrack.views.recording

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.component_audio_recorder.view.*
import kr.ac.snu.hcil.android.common.events.Event
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.views.R
import java.io.File


abstract class AudioRecorderViewBase(context: Context, attr: AttributeSet?) : ConstraintLayout(context, attr), IAudioRecorderView {


    override var audioFileUri: Uri = Uri.EMPTY
        set(value) {
            if (field != value) {
                field = value
                if (value != Uri.EMPTY) {
                    if (File(value.path).exists()) {
                        this.setViewState(EMode.Player, EStatus.Idle)

                        val mmr = MediaMetadataRetriever()
                        mmr.setDataSource(context, value)
                        val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toInt()
                        audioLengthSeconds = duration / 1000
                    } else {
                        println("recorded file does not exists.")
                        this.setViewState(EMode.Recorder, this.status)
                    }
                } else {
                    this.setViewState(EMode.Recorder, EStatus.Idle)
                }

                audioFileUriChanged.invoke(this, value)
            }
        }

    override var recordingOutputDirectoryPathOverride: File? = null

    override val audioFileUriChanged = Event<Uri>()

    protected enum class EMode {
        Recorder,
        Player
    }

    protected enum class EStatus {
        Idle,
        Running,
        Paused
    }

    protected var mode: EMode = EMode.Recorder
        private set

    protected var status: EStatus = EStatus.Idle
        private set


    protected var audioLengthSeconds: Int = 60
        private set(value) {
            if (field != value) {
                field = value
                //TODO refresh to chronometer
            }
        }


    init {
        inflateContent(R.layout.component_audio_recorder, true)

        this.ui_button_stop.setOnClickListener { this.onStopButtonClicked() }
        this.ui_button_main.setOnClickListener { this.onMainButtonClicked() }
        this.ui_button_delete.setOnClickListener { this.onDeleteButtonClicked() }

        refreshUIStyles()

    }

    private fun refreshUIStyles() {

        //handle chronometer style
        this.ui_chronometer.setTextColor(
                if (this.status == EStatus.Running) {
                    when (this.mode) {
                        EMode.Recorder -> ContextCompat.getColor(context, R.color.colorRed)
                        EMode.Player -> ContextCompat.getColor(context, R.color.colorPointed)
                    }
                } else {
                    ContextCompat.getColor(context, R.color.textColorLight)
                }
        )

        //handle stop button
        this.ui_button_stop.visibility = if (this.status == EStatus.Idle) View.INVISIBLE else View.VISIBLE

        //handle main button
        this.ui_button_main.apply {
            if (status == EStatus.Running) {
                this.tintColor = ContextCompat.getColor(context, R.color.buttonIconColorDark)
                this.setIconResource(R.drawable.pause)
                this.setBackgroundColor(ContextCompat.getColor(context, R.color.editTextFormBackground))
            } else {
                when (mode) {
                    EMode.Recorder -> {
                        this.setIconResource(null as Drawable?)
                        this.setBackgroundColor(ContextCompat.getColor(context, R.color.colorRed))
                    }
                    EMode.Player -> {
                        this.tintColor = Color.parseColor("#ffffff")
                        this.setIconResource(R.drawable.play_dark)
                        this.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPointed_Light))
                    }
                }
            }
        }

        this.ui_button_delete.visibility = if (status == EStatus.Idle && mode == EMode.Player) View.VISIBLE else View.GONE
    }

    protected fun setViewState(mode: EMode, status: EStatus, animate: Boolean = true) {
        this.mode = mode
        this.status = status
        if (animate) {
            TransitionManager.beginDelayedTransition(this)
        }
        refreshUIStyles()
    }

    protected fun onMainButtonClicked() {
        when (this.status) {
            EStatus.Idle -> {
                //recording or playing
                if (this.mode == EMode.Recorder) {
                    this.onStartRecording()
                } else this.onPlayRecordedAudio()
                this.setViewState(this.mode, EStatus.Running)
            }

            EStatus.Running -> {
                //pause
                if (this.mode == EMode.Recorder) {
                    this.onPauseRecording()
                } else this.onPausePlayingRecordedAudio()

                this.setViewState(this.mode, EStatus.Paused)
            }

            EStatus.Paused -> {
                //resume
                if (this.mode == EMode.Recorder) {
                    this.onResumeRecording()
                } else this.onResumeRecordedAudio()

                this.setViewState(this.mode, EStatus.Running)
            }
        }
    }

    protected fun onDeleteButtonClicked() {
        this.setViewState(EMode.Recorder, EStatus.Idle)
        this.clearRecordedFile()
    }

    protected fun onStopButtonClicked() {
        this.setViewState(if (this.mode == EMode.Recorder) EMode.Player else EMode.Recorder, EStatus.Idle)
        if (this.mode == EMode.Recorder) {
            this.onFinishRecording()
        } else this.onStopPlayingRecordedAudio()
    }

    fun finishRecording() {
        this.setViewState(if (this.mode == EMode.Recorder) EMode.Player else EMode.Recorder, EStatus.Idle)
        if (this.mode == EMode.Recorder) {
            this.onFinishRecording()
        } else this.onStopPlayingRecordedAudio()
    }

    protected abstract fun onStartRecording()
    protected abstract fun onFinishRecording()
    protected abstract fun onPauseRecording()
    protected abstract fun onResumeRecording()


    protected abstract fun onPlayRecordedAudio()
    protected abstract fun onStopPlayingRecordedAudio()
    protected abstract fun onPausePlayingRecordedAudio()
    protected abstract fun onResumeRecordedAudio()

    protected abstract fun clearRecordedFile()

}