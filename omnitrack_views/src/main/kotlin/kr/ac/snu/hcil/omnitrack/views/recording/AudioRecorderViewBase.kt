package kr.ac.snu.hcil.omnitrack.views.recording

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.component_audio_recorder.view.*
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.events.Event
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.views.R
import java.io.File
import java.util.concurrent.TimeUnit


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
                        audioLengthMillis = duration
                    } else {
                        println("recorded file does not exists.")
                        this.setViewState(EMode.Recorder, this.status)
                    }
                } else {
                    audioLengthMillis = 0
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


    protected var audioLengthMillis: Int = 0
        private set(value) {
            if (field != value) {
                field = value
                refreshUIStyles()
            }
        }

    protected var currentRecordedDurationMillis: Int = 0
        set(value) {
            if (field != value) {
                field = value
                refreshUIStyles()
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
                this.setBackgroundColor(ContextCompat.getColor(context, R.color.editTextFormBackground))
            } else {
                when (mode) {
                    EMode.Recorder -> {
                        this.setBackgroundColor(ContextCompat.getColor(context, R.color.colorRed))
                    }
                    EMode.Player -> {
                        this.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPointed_Light))
                    }
                }
            }
        }

        this.ui_icon_main.apply {
            if (status == EStatus.Running) {
                this.setImageResource(R.drawable.pause)
                this.setColorFilter(ContextCompat.getColor(context, R.color.buttonIconColorDark))
                this.visibility = View.VISIBLE
            } else {
                when (mode) {
                    EMode.Recorder -> {
                        this.visibility = View.GONE
                    }
                    EMode.Player -> {
                        this.setImageResource(R.drawable.play_dark)
                        this.setColorFilter(Color.parseColor("#FFFFFFFF"))
                        this.visibility = View.VISIBLE
                    }
                }
            }
        }

        this.ui_button_delete.visibility = if (status == EStatus.Idle && mode == EMode.Player) View.VISIBLE else View.GONE

        //update values

        val duration = when (mode) {
            EMode.Recorder -> currentRecordedDurationMillis
            EMode.Player -> audioLengthMillis
        }


        val minute = duration / 60000
        val second = (duration % 60000) / 1000
        val millis = duration % 1000

        @SuppressLint("SetTextI18n")
        this.ui_chronometer.text = "$minute:${second.toString().padStart(2, '0')}.${(millis / 100)}"
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
        this.currentRecordedDurationMillis = 0
        this.clearRecordedFile()
    }

    protected fun onStopButtonClicked() {
        val originalMode = this.mode
        this.setViewState(EMode.Player, EStatus.Idle)
        if (originalMode == EMode.Recorder) {
            this.onFinishRecording()
        } else this.onStopPlayingRecordedAudio()
    }

    fun finishRecordingAndGetUri(): Single<Nullable<Uri>> {
        return Single.defer {
            this.setViewState(EMode.Player, EStatus.Idle)
            this.onFinishRecording()
            this.audioFileUriChanged.observable.map { (sender, args) -> Nullable(args) }.firstOrError().subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                    .timeout(2, TimeUnit.SECONDS).onErrorReturn { Nullable(null) }
        }
    }

    fun stopPlayingRecordedAudio() {
        this.setViewState(EMode.Player, EStatus.Idle)
        this.onStopPlayingRecordedAudio()
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