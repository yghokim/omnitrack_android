package kr.ac.snu.hcil.omnitrack.services

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioRecordingModule
import kr.ac.snu.hcil.omnitrack.utils.VectorIconHelper
import org.jetbrains.anko.notificationManager
import java.util.concurrent.TimeUnit

/**
 * Created by junhoe on 2017. 9. 11..
 */
class OTAudioRecordService : Service(), AudioRecordingModule.RecordingListener {

    companion object {

        private const val TAG = "AudioRecordService"

        const val DEBUG_USE_NOTIFICATION = false

        const val RECORD_NOTIFICATION_ID = 2443
        const val INTENT_EXTRA_SESSION_ID = "audioRecordSessionId"
        const val INTENT_EXTRA_CURRENT_PROGRESS_MilliSECONDS = "audioCurrentRecordProgressMilliSeconds"
        const val INTENT_EXTRA_CURRENT_PROGRESS_RATIO = "audioCurrentRecordProgressRatio"
        const val INTENT_EXTRA_RECORD_URI = "recordURI"
        const val INTENT_EXTRA_RECORD_TITLE = "recordTitle"

        var currentSessionId: String? = null
            private set

        var currentRecordingModule: AudioRecordingModule? = null
            private set

        var isRecording: Boolean = false

        private val commandFilter = IntentFilter().apply {
            addAction(INTENT_ACTION_RECORD_STOP)
            addAction(INTENT_ACTION_RECORD_PAUSE)
            addAction(INTENT_ACTION_RECORD_RESUME)
        }

        const val INTENT_ACTION_RECORD_START = "${OTApp.PREFIX_ACTION}.RECORD_START"
        const val INTENT_ACTION_RECORD_PAUSE = "${OTApp.PREFIX_ACTION}.RECORD_PAUSE"
        const val INTENT_ACTION_RECORD_RESUME = "${OTApp.PREFIX_ACTION}.RECORD_RESUME"
        const val INTENT_ACTION_RECORD_STOP = "${OTApp.PREFIX_ACTION}.RECORD_STOP"

        const val INTENT_ACTION_RECORD_DISCARD = "${OTApp.PREFIX_ACTION}.RECORD_DISCARD"

        const val INTENT_ACTION_EVENT_RECORD_START_CALLBACK = "${OTApp.PREFIX_ACTION}.RECORD_START_CALLBACK"
        const val INTENT_ACTION_EVENT_RECORD_COMPLETED = "${OTApp.PREFIX_ACTION}.RECORD_COMPLETED"
        const val INTENT_ACTION_EVENT_RECORD_PROGRESS = "${OTApp.PREFIX_ACTION}.RECORD_PROGRESS"

        fun makeStartIntent(context: Context, sessionId: String, title: String = "Audio Record", file: Uri): Intent {
            return Intent(context, OTAudioRecordService::class.java).setAction(INTENT_ACTION_RECORD_START)
                    .setData(file)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
                    .putExtra(INTENT_EXTRA_RECORD_TITLE, title)
        }

        fun makeStartCallbackIntent(sessionId: String, recordedFileUri: Uri): Intent {
            return Intent(INTENT_ACTION_EVENT_RECORD_START_CALLBACK)
                    .putExtra(INTENT_EXTRA_RECORD_URI, recordedFileUri.toString())
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
        }

        private fun makeProgressIntent(sessionId: String, progressMilliSeconds: Int = 0, progressRatio: Float): Intent {
            return Intent(INTENT_ACTION_EVENT_RECORD_PROGRESS)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
                    .putExtra(INTENT_EXTRA_CURRENT_PROGRESS_MilliSECONDS, progressMilliSeconds)
                    .putExtra(INTENT_EXTRA_CURRENT_PROGRESS_RATIO, progressRatio)
        }

        fun makeStopIntent(context: Context, sessionId: String): Intent {
            return Intent(context, OTAudioRecordService::class.java).setAction(INTENT_ACTION_RECORD_STOP)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
        }

        fun makeDiscardIntent(context: Context, sessionId: String): Intent {
            return Intent(context, OTAudioRecordService::class.java).setAction(INTENT_ACTION_RECORD_DISCARD)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
        }

        fun makeCompleteIntent(sessionId: String, uri: Uri?): Intent {
            return Intent(INTENT_ACTION_EVENT_RECORD_COMPLETED)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
                    .putExtra(INTENT_EXTRA_RECORD_URI, uri.toString())
        }

        fun makePauseIntent(sessionId: String): Intent {
            return Intent(INTENT_ACTION_RECORD_PAUSE)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
        }

        fun makeResumeIntent(sessionId: String): Intent {
            return Intent(INTENT_ACTION_RECORD_RESUME)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
        }
    }

    private var title = ""
    private val commandReceiver = CommandReceiver()
    private var remoteViews: RemoteViews? = null

    private var ticker: Disposable? = null

    override fun onCreate() {
        super.onCreate()
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(commandReceiver, commandFilter)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            INTENT_ACTION_RECORD_START -> {
                val sessionId = intent.getStringExtra(INTENT_EXTRA_SESSION_ID)
                title = intent.getStringExtra(INTENT_EXTRA_RECORD_TITLE)
                val filePath = intent.data

                if (filePath != null && sessionId != null) {
                    Log.d(TAG, "Start recording of record view: $filePath $sessionId")
                    if (currentSessionId != sessionId && currentRecordingModule?.isRunning() == true) {
                        stopRecording()
                    }

                    currentSessionId = sessionId
                    currentRecordingModule = AudioRecordingModule(this, filePath)
                    remoteViews = initRemoteViews(this, currentSessionId!!, title)
                    putNotificationControl(remoteViews)
                    startRecording()
                }
            }
            INTENT_ACTION_RECORD_STOP -> {
                val sessionId = intent.getStringExtra(INTENT_EXTRA_SESSION_ID)
                if (currentSessionId == sessionId) {
                    stopRecording()
                }
            }

            INTENT_ACTION_RECORD_DISCARD -> {
                val sessionId = intent.getStringExtra(INTENT_EXTRA_SESSION_ID)
                if (currentSessionId == sessionId) {
                    discardRecording()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        currentRecordingModule!!.startAsync()
        isRecording = true
        println("send recording start callback intent")
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(makeStartCallbackIntent(currentSessionId!!, currentRecordingModule!!.fileUri))
        ticker?.dispose()
        ticker = Observable.interval(50, TimeUnit.MILLISECONDS).subscribe {
            val now = System.currentTimeMillis()
            if (currentSessionId != null && currentRecordingModule != null) {
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(makeProgressIntent(currentSessionId!!, currentRecordingModule!!.recordedDurationMillis, currentRecordingModule!!.getCurrentProgressRatio(now)))
            }
        }
    }

    private fun stopRecording() {
        if (currentRecordingModule != null) {
            currentRecordingModule!!.stop()
            isRecording = false
        }
    }

    private fun discardRecording() {
        if (currentRecordingModule != null) {
            currentRecordingModule!!.cancel()
            isRecording = false
        }
    }

    override fun onRecordingFinished(module: AudioRecordingModule, resultUri: Uri?) {
        if (currentRecordingModule != null) {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(makeCompleteIntent(currentSessionId!!, resultUri))
            disposeRecorder()
        }
    }

    override fun onBind(p0: Intent?): IBinder = Binder()

    private fun initRemoteViews(context: Context,
                                sessionId: String,
                                title: String): RemoteViews {
        return RemoteViews(context.packageName,
                R.layout.remoteview_notification_record_player).apply {

            setImageViewBitmap(R.id.ui_audio_icon, VectorIconHelper.getConvertedBitmap(context, R.drawable.icon_small_audio, tint = Color.WHITE))
            setTextViewText(R.id.ui_title, title)
            setChronometer(R.id.ui_duration_view, SystemClock.elapsedRealtime(), null, true)
            setImageViewBitmap(R.id.ui_player_button, VectorIconHelper.getConvertedBitmap(context, R.drawable.stop_dark, 24, Color.WHITE))
            setImageViewBitmap(R.id.ui_discard_button, VectorIconHelper.getConvertedBitmap(context, R.drawable.trashcan, 24, Color.WHITE))
            setOnClickPendingIntent(R.id.ui_player_button, PendingIntent.getService(context,
                    RECORD_NOTIFICATION_ID,
                    makeStopIntent(context, sessionId),
                    PendingIntent.FLAG_ONE_SHOT))
            setOnClickPendingIntent(R.id.ui_discard_button, PendingIntent.getService(context,
                    RECORD_NOTIFICATION_ID,
                    makeDiscardIntent(context, sessionId),
                    PendingIntent.FLAG_ONE_SHOT))
        }
    }

    private val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(this, OTNotificationManager.CHANNEL_ID_WIDGETS)
                    .setSmallIcon(R.drawable.icon_simple)
                    .setAutoCancel(false)
                    .setOngoing(true)
                .setContentTitle("OmniTrack Audio Record Player")
    }

    private fun putNotificationControl(rv: RemoteViews?) {
        if (DEBUG_USE_NOTIFICATION) {
            val notification = notificationBuilder.setContent(rv).build()
            notificationManager.notify(TAG, RECORD_NOTIFICATION_ID, notification)
        }
    }

    private fun dismissNotificationControl() {
        if (DEBUG_USE_NOTIFICATION) {
            notificationManager.cancel(TAG, RECORD_NOTIFICATION_ID)
        }
    }

    private fun disposeRecorder() {
        title = ""
        currentRecordingModule = null
        currentSessionId = null
        remoteViews = null
        isRecording = false
        ticker?.dispose()
        ticker = null
        dismissNotificationControl()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(commandReceiver)
        disposeRecorder()
    }

    inner class CommandReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val sessionId = intent.getStringExtra(INTENT_EXTRA_SESSION_ID)
            if (currentSessionId == sessionId) {
                when (intent.action) {
                    INTENT_ACTION_RECORD_STOP -> {
                        stopRecording()
                    }
                    INTENT_ACTION_RECORD_PAUSE -> {
                        println("recorder pause!")
                        currentRecordingModule?.pause()
                    }

                    INTENT_ACTION_RECORD_RESUME -> {
                        println("recorder resume!")
                        currentRecordingModule?.resume()
                    }
                }
            }
        }
    }
}