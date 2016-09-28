package kr.ac.snu.hcil.omnitrack.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Looper
import android.text.format.DateUtils
import com.badoo.mobile.util.WeakHandler
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.FileOutputStream

/**
 * Created by younghokim on 2016. 9. 27..
 */
class AudioRecordingModule(val listener: RecordingListener, val filePath: String, val samplingRate: Int = 11025, val maxLengthMillis: Long = DateUtils.MINUTE_IN_MILLIS) {

    interface RecordingListener {
        fun onRecordingProgress(module: AudioRecordingModule, volume: Int)
        fun onRecordingFinished(module: AudioRecordingModule, success: Boolean)
        fun onRecordingCanceled(module: AudioRecordingModule)
    }

    private val audioRecord: AudioRecord

    private val bufferSize: Int

    private var _isRecording: Boolean = false

    val isRecording: Boolean get() = _isRecording

    private var _stopReserved: Boolean = false

    private var _recordingStartedAt: Long = 0

    init {

        bufferSize = AudioRecord.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

        audioRecord = AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                samplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)

    }

    fun startAsync() {


        val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(filePath, false)))

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            println("audioRecorder cannot be initialized.")
            listener.onRecordingFinished(this, false)
            return;
        }

        _stopReserved = false
        _isRecording = true

        Thread(RecordRunnable(outputStream)).start()

    }

    fun stopAsync() {
        _stopReserved = true
    }

    internal inner class RecordRunnable(val outputStream: DataOutputStream) : Runnable {

        private val mainHandler: WeakHandler

        val progressRunner = ProgressPublishRunnable()

        init {
            mainHandler = WeakHandler(Looper.getMainLooper())
        }

        override fun run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

            val audioBuffer = ShortArray(bufferSize / 2)
            audioRecord.startRecording()

            println("Start recording")
            _recordingStartedAt = System.currentTimeMillis()

            var shortsRead: Long = 0
            while (!_stopReserved) {
                val numberOfShort = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                shortsRead += numberOfShort.toLong()

                var volume = 0
                //store the audioBuffer
                for (i in 0..numberOfShort - 1) {
                    outputStream.writeShort(audioBuffer[i].toInt())
                    volume = Math.max(volume, audioBuffer[i].toInt())
                }

                if (System.currentTimeMillis() - _recordingStartedAt >= maxLengthMillis) {
                    _stopReserved = true
                }

                this.progressRunner.volume = volume
                mainHandler.post(this.progressRunner)
            }

            audioRecord.stop()
            audioRecord.release()
            _stopReserved = false
            _isRecording = false
            outputStream.close()

            mainHandler.post { listener.onRecordingFinished(this@AudioRecordingModule, true) }
        }

        internal inner class ProgressPublishRunnable : Runnable {
            var volume: Int = 0

            override fun run() {
                listener.onRecordingProgress(this@AudioRecordingModule, volume)
            }

        }

    }
}