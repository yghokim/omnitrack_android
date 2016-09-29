package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.media.MediaRecorder
import android.text.format.DateUtils
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Created by younghokim on 2016. 9. 27..
 */
class AudioRecordingModule(var listener: RecordingListener?,
                           val filePath: String, val samplingRate: Int = 11025,
                           val maxLengthMillis: Int = DateUtils.MINUTE_IN_MILLIS.toInt(),
                           progressTerm: Int = 200) : AAudioModule(progressTerm), MediaRecorder.OnInfoListener, AudioRecorderProgressBar.AmplitudeTimelineProvider {

    interface RecordingListener {
        fun onRecordingProgress(module: AudioRecordingModule, volume: Int)
        fun onRecordingFinished(module: AudioRecordingModule, resultPath: String?)
    }

    private val recorder: MediaRecorder

    private var _isRecording: Boolean = false

    private val amplitudes = ArrayList<Pair<Float, Int>>()

    override val amplitudeTimeline: List<Pair<Float, Int>> get() = amplitudes

    init {

        recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder.setOutputFile(filePath)
        recorder.setAudioSamplingRate(samplingRate)
        recorder.setMaxDuration(maxLengthMillis)

        recorder.setOnInfoListener(this)
    }

    override fun isRunning(): Boolean {
        return _isRecording
    }

    override fun getCurrentProgressRatio(now: Long): Float {
        return getCurrentProgressDuration(now).toFloat() / maxLengthMillis
    }

    override fun getCurrentProgressDuration(now: Long): Int {
        return (now - startedAt).toInt()
    }

    override fun onTick(time: Long) {
        amplitudes.add(Pair(getCurrentProgressRatio(time), recorder.maxAmplitude))
        listener?.onRecordingProgress(this, recorder.maxAmplitude)
    }


    override fun onStart() {

        try {
            recorder.prepare()
        } catch(e: IOException) {
            e.printStackTrace()
            println("audioRecorder cannot be initialized.")
            listener?.onRecordingFinished(this, null)
            return;
        }

        _isRecording = true

        amplitudes.clear()
        recorder.start()
        println("Start recording")
    }

    override fun onStop(cancel: Boolean) {
        recorder.stop()
        recorder.release()
        _isRecording = false
        if (cancel) {
            File(filePath).delete()
            listener?.onRecordingFinished(this, null)
            println("canceled the audio recording.")
        } else {
            listener?.onRecordingFinished(this, filePath)
            println("successfully finished the audio recording.")
        }
    }


    override fun onInfo(p0: MediaRecorder?, what: Int, extra: Int) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            //max duration reached.
            stop()
        }
    }
}