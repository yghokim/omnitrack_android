package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.media.AudioFormat
import android.media.MediaRecorder
import android.net.Uri
import android.text.format.DateUtils
import androidx.core.net.toFile
import kr.ac.snu.hcil.android.common.containers.WritablePair
import omrecorder.*
import java.io.File
import java.util.*

/**
 * Created by younghokim on 2016. 9. 27..
 */
class AudioRecordingModule(var listener: RecordingListener?,
                           val fileUri: Uri,
                           val maxLengthMillis: Int = 2 * DateUtils.MINUTE_IN_MILLIS.toInt()) : PullTransport.OnAudioChunkPulledListener, AudioRecorderProgressBar.AmplitudeTimelineProvider {

    interface RecordingListener {
        fun onRecordingFinished(module: AudioRecordingModule, resultUri: Uri?)
    }


    private val sessionTimestamps = ArrayList<WritablePair<Long, Long?>>()

    private val recorder: Recorder

    private var _isRecording: Boolean = false

    private val amplitudes = ArrayList<Pair<Float, Int>>()

    override val amplitudeTimeline: List<Pair<Float, Int>> get() = amplitudes

    private val audioSource: PullableSource by lazy {
        PullableSource.AutomaticGainControl(
                PullableSource.Default(
                        AudioRecordConfig.Default(
                                MediaRecorder.AudioSource.MIC,
                                AudioFormat.ENCODING_PCM_16BIT,
                                AudioFormat.CHANNEL_IN_MONO, 11000
                        )
                )
        )
    }

    val recordedDurationMillis: Int
        get() {
            return if (sessionTimestamps.size > 1) {
                sessionTimestamps.sumBy {
                    ((it.second ?: System.currentTimeMillis()) - it.first).toInt()
                }
            } else ((sessionTimestamps.last().second
                    ?: System.currentTimeMillis()) - sessionTimestamps.last().first).toInt()
        }

    init {

        recorder = OmRecorder.wav(
                PullTransport.Default(
                        audioSource, this
                ), fileUri.toFile())

    }


    override fun onAudioChunkPulled(audioChunk: AudioChunk?) {
        println("audioChunk: ${audioChunk?.maxAmplitude()}")
    }

    fun isRunning(): Boolean {
        return _isRecording
    }


    fun startAsync() {
        _isRecording = true

        amplitudes.clear()
        recorder.startRecording()
        sessionTimestamps.clear()
        sessionTimestamps.add(WritablePair(System.currentTimeMillis(), null))
        println("Start recording")
    }

    fun getCurrentProgressRatio(now: Long): Float {
        return recordedDurationMillis.toFloat() / maxLengthMillis
    }

    fun stop() {
        sessionTimestamps.lastOrNull()?.second = System.currentTimeMillis()
        onStop(false)
    }

    fun cancel() {
        sessionTimestamps.lastOrNull()?.second = System.currentTimeMillis()
        onStop(true)
    }

    fun pause() {
        sessionTimestamps.lastOrNull()?.second = System.currentTimeMillis()
        recorder.pauseRecording()
    }

    fun resume() {
        sessionTimestamps.add(WritablePair(System.currentTimeMillis(), null))
        recorder.resumeRecording()
    }

    fun onStop(cancel: Boolean) {
        recorder.stopRecording()
        _isRecording = false
        if (cancel) {
            File(fileUri.path).delete()
            listener?.onRecordingFinished(this, null)
            println("canceled the audio recording.")
        } else {
            listener?.onRecordingFinished(this, fileUri)
            println("successfully finished the audio recording.")
        }
    }
}