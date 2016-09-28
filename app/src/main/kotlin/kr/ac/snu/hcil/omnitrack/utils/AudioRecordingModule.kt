package kr.ac.snu.hcil.omnitrack.utils

import android.media.MediaRecorder
import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioRecorderProgressBar
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Created by younghokim on 2016. 9. 27..
 */
class AudioRecordingModule(var listener: RecordingListener,
                           val filePath: String, val samplingRate: Int = 11025,
                           val maxLengthMillis: Int = DateUtils.MINUTE_IN_MILLIS.toInt(),
                           val progressTerm: Int = 100) : MediaRecorder.OnInfoListener, AudioRecorderProgressBar.AmplitudeTimelineProvider {


    interface RecordingListener {
        fun onRecordingProgress(module: AudioRecordingModule, volume: Int)
        fun onRecordingFinished(module: AudioRecordingModule, resultPath: String?)
    }

    private val recorder: MediaRecorder

    private var _isRecording: Boolean = false

    val isRecording: Boolean get() = _isRecording

    private val amplitudes = ArrayList<Pair<Float, Int>>()

    override val amplitudeTimeline: List<Pair<Float, Int>> get() = amplitudes

    var recordingStartedAt: Long = 0
        private set

    private val ticker: Ticker

    init {

        recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder.setOutputFile(filePath)
        recorder.setAudioSamplingRate(samplingRate)
        recorder.setMaxDuration(maxLengthMillis)

        recorder.setOnInfoListener(this)

        ticker = Ticker(progressTerm)

        ticker.tick += {
            sender, time ->
            val progress = (time - recordingStartedAt).toFloat() / maxLengthMillis
            println(progress)
            amplitudes.add(Pair(progress, recorder.maxAmplitude))
            listener.onRecordingProgress(this, recorder.maxAmplitude)

        }
    }

    fun startAsync() {

        try {
            recorder.prepare()
        } catch(e: IOException) {
            e.printStackTrace()
            println("audioRecorder cannot be initialized.")
            listener.onRecordingFinished(this, null)
            return;
        }

        _isRecording = true

        amplitudes.clear()

        recordingStartedAt = System.currentTimeMillis()
        recorder.start()
        println("Start recording")
        ticker.start()

    }

    fun stop(cancel: Boolean) {
        ticker.stop()
        recorder.stop()
        recorder.release()
        _isRecording = false
        if (cancel) {
            File(filePath).delete()
            listener.onRecordingFinished(this, null)
            println("canceled the audio recording.")
        } else {
            listener.onRecordingFinished(this, filePath)
            println("successfully finished the audio recording.")
        }
    }

    override fun onInfo(p0: MediaRecorder?, what: Int, extra: Int) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            //max duration reached.
            stop(false)
        }
    }
}