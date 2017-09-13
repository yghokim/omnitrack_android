package kr.ac.snu.hcil.omnitrack.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.RemoteViews
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioRecorderView
import kr.ac.snu.hcil.omnitrack.utils.VectorIconHelper

/**
 * Created by junhoe on 2017. 9. 11..
 */
class OTAudioRecordService : Service() {

    companion object {
        const val TAG = "AudioRecordService"

        const val RECORD_NOTIFICATION_ID = 2443
        const val INTENT_EXTRA_SESSION_ID = "audioRecordSessionId"
        const val INTENT_EXTRA_CURRENT_POSITION_SECONDS = "audioCurrentDurationSeconds"
        const val INTENT_EXTRA_RECORD_TITLE = "recordTitle"

        var currentSessionId: String? = null
            private set

        private val commandFilter = IntentFilter().apply {
            addAction(INTENT_ACTION_RECORD_START)
            addAction(INTENT_ACTION_RECORD_STOP)
            addAction(INTENT_ACTION_RECORD_PROGRESS)
        }

        const val INTENT_ACTION_RECORD_START = "kr.ac.snu.hcil.omnitrack.action.ACTION_RECORD_START"
        const val INTENT_ACTION_RECORD_STOP = "kr.ac.snu.hcil.omnitrack.action.ACTION_RECORD_STOP"
        const val INTENT_ACTION_RECORD_PROGRESS = "kr.ac.snu.hcil.omnitrack.action.ACTION_RECORD_PROGRESS"

        const val INTENT_ACTION_EVENT_RECORD_COMPLETED = "kr.ac.snu.hcil.omnitrack.action.ACTION_RECORD_COMPLETED"

        fun makeStartIntent(context: Context, sessionId: String, title: String = "Audio Record"): Intent {
            return Intent(context, OTAudioRecordService::class.java).setAction(INTENT_ACTION_RECORD_START)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
                    .putExtra(INTENT_EXTRA_RECORD_TITLE, title)
        }

        fun makeProgressIntent(context: Context, sessionId: String, progressSeconds: Int = 0): Intent {
            return Intent(context, OTAudioRecordService::class.java).setAction(INTENT_ACTION_RECORD_PROGRESS)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
                    .putExtra(INTENT_EXTRA_CURRENT_POSITION_SECONDS, progressSeconds)
        }

        fun makeStopIntent(sessionId: String): Intent {
            return Intent(INTENT_ACTION_RECORD_STOP)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
        }

        fun makeCompleteIntent(sessionId: String): Intent {
            return Intent(INTENT_ACTION_EVENT_RECORD_COMPLETED)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
        }
    }

    private val commandReceiver = CommandReceiver()
    private var title = ""
    private var description = ""
    private var currentPlayPositionSecond = 0
    private var remoteViews: RemoteViews? = null

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
                if (sessionId != null) {
                    Log.d(TAG, "Play record view: $sessionId")
                    putNotificationControl(this, remoteViews)
                    currentSessionId = sessionId
                    remoteViews = initRemoteViews(this, title, description, currentPlayPositionSecond)
                }
            }
            INTENT_ACTION_RECORD_STOP -> {
                val sessionId = intent.getStringExtra(INTENT_EXTRA_SESSION_ID)
                if (currentSessionId == sessionId) {
                    dispose()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder = Binder()

    private fun initRemoteViews(context: Context,
                                title: String,
                                description: String,
                                currentProgressSeconds: Int): RemoteViews {
        return RemoteViews(context.packageName,
                R.layout.remoteview_notification_record_player).apply {
            setTextViewText(R.id.ui_title, title)
            setTextViewText(R.id.ui_description, description)
            setTextViewText(R.id.ui_duration_view, AudioRecorderView.formatTime(currentProgressSeconds))
            setImageViewBitmap(R.id.ui_player_button, VectorIconHelper.getConvertedBitmap(context, R.drawable.ex))
            currentSessionId?.let {
                val stopIntent = Intent(context, OTAudioRecordService::class.java).setAction(INTENT_ACTION_RECORD_STOP)
                        .putExtra(INTENT_EXTRA_SESSION_ID, currentSessionId)
                setOnClickPendingIntent(R.id.ui_player_button, PendingIntent.getService(context, RECORD_NOTIFICATION_ID, stopIntent, PendingIntent.FLAG_ONE_SHOT))
            }
        }
    }

    private fun updateRemoteView(currentProgressSeconds: Int) {
        remoteViews?.setTextViewText(R.id.ui_duration_view, AudioRecorderView.formatTime(currentProgressSeconds))
        putNotificationControl(this, remoteViews)
    }

    private var notificationBuilder: NotificationCompat.Builder? = null
    private fun putNotificationControl(context: Context, rv: RemoteViews?) {
        val builder = notificationBuilder ?: NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.icon_simple)
                .setAutoCancel(false)
                .setContentTitle("OmniTrack Audio Record Player") as NotificationCompat.Builder

        notificationBuilder = builder

        val notification = builder.setContent(rv)
                .build()

        notificationManager.notify(TAG, RECORD_NOTIFICATION_ID, notification)
    }

    private fun dismissNotificationControl()
        = notificationManager.cancel(TAG, RECORD_NOTIFICATION_ID)

    private val notificationManager: NotificationManager by lazy {
        (OTApplication.app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    }

    private fun dispose() {
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(makeCompleteIntent(currentSessionId?: ""))
        title = ""
        description = ""
        currentPlayPositionSecond = 0
        remoteViews = null
        dismissNotificationControl()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        dispose()
    }

    inner class CommandReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                INTENT_ACTION_RECORD_STOP -> {
                    val sessionId = intent.getStringExtra(INTENT_EXTRA_SESSION_ID)
                    if (currentSessionId == sessionId) {
                        dispose()
                    }
                }

                INTENT_ACTION_RECORD_PROGRESS -> {
                    val sessionId = intent.getStringExtra(INTENT_EXTRA_SESSION_ID)
                    if (currentSessionId == sessionId) {
                        val progressSeconds = intent.getIntExtra(INTENT_EXTRA_CURRENT_POSITION_SECONDS, 0)
                        updateRemoteView(progressSeconds)
                    }
                }
            }
        }
    }
}