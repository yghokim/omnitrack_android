package kr.ac.snu.hcil.omnitrack.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.RemoteViews
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioRecordMetadata
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioRecorderView
import kr.ac.snu.hcil.omnitrack.utils.Ticker
import kr.ac.snu.hcil.omnitrack.utils.VectorIconHelper

/**
 * Created by Young-Ho on 4/22/2017.
 * Followed code from https://www.sitepoint.com/a-step-by-step-guide-to-building-an-android-audio-player-app/
 */
class OTAudioPlayService : Service(), MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {


    companion object {
        const val TAG = "AudioService"

        const val AUDIO_NOTIFICATION_ID = 2442

        const val INTENT_EXTRA_SESSION_ID = "audioPlaySessionId"
        const val INTENT_EXTRA_CURRENT_POSITION_SECONDS = "audioCurrentDurationSeconds"
        const val INTENT_EXTRA_CURRENT_PROGRESS_RATIO = "audioCurrentProgressRatio"
        const val INTENT_EXTRA_AUDIO_TITLE = "audioTitle"

        const val INTENT_ACTION_PLAY = "kr.ac.snu.hcil.omnitrack.action.ACTION_AUDIO_PLAY"
        const val INTENT_ACTION_STOP = "kr.ac.snu.hcil.omnitrack.action.ACTION_AUDIO_STOP"
        const val INTENT_ACTION_STOP_ALL = "kr.ac.snu.hcil.omnitrack.action.ACTION_AUDIO_STOP_ALL"

        const val INTENT_ACTION_EVENT_AUDIO_COMPLETED = "kr.ac.snu.hcil.omnitrack.action.ACTION_AUDIO_COMPLETED"
        const val INTENT_ACTION_EVENT_AUDIO_PROGRESS = "kr.ac.snu.hcil.omnitrack.action.ACTION_AUDIO_PROGRESS"

        var currentSessionId: String? = null
            private set
        /*
            get(){
                return OTApplication.app.systemSharedPreferences.getString(INTENT_EXTRA_SESSION_ID, null)
            }
            private set(value){
                if(value!=null)
                {
                    OTApplication.app.systemSharedPreferences.edit().putString(INTENT_EXTRA_SESSION_ID, value).apply()
                }
                else{
                    OTApplication.app.systemSharedPreferences.edit().remove(INTENT_EXTRA_SESSION_ID).apply()
                }
            }
            */

        var currentPlayer: MediaPlayer? = null
            private set

        val currentPlayPositionSecond: Int get() {
            return ((currentPlayer?.currentPosition ?: 0) / 1000)
        }

        val currentProgressRatio: Float get() {
            val currentPlayer = currentPlayer
            if (currentPlayer != null) {
                return currentPlayer.currentPosition / currentPlayer.duration.toFloat()
            } else return 0f
        }

        val isSoundPlaying: Boolean
            get() {
                return currentPlayer?.isPlaying ?: false
            }

        private val commandFilter = IntentFilter().apply {
            addAction(INTENT_ACTION_STOP)
        }


        fun makePlayIntent(context: Context, file: Uri, sessionId: String, title: String = "Audio Record"): Intent {
            return Intent(context, OTAudioPlayService::class.java).setAction(INTENT_ACTION_PLAY).setData(file)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
                    .putExtra(INTENT_EXTRA_AUDIO_TITLE, title)
        }

        fun makeStopCommandIntent(sessionId: String): Intent {
            return Intent(INTENT_ACTION_STOP).putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
        }

        private fun makeCompleteEventIntent(sessionId: String): Intent {
            return Intent(INTENT_ACTION_EVENT_AUDIO_COMPLETED).putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
        }

        private fun makeOnProgressEventIntent(sessionId: String, progressDurationSecond: Int, progressRatio: Float): Intent {
            return Intent(INTENT_ACTION_EVENT_AUDIO_PROGRESS)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
                    .putExtra(INTENT_EXTRA_CURRENT_POSITION_SECONDS, progressDurationSecond)
                    .putExtra(INTENT_EXTRA_CURRENT_PROGRESS_RATIO, progressRatio)
        }

        private fun makeAudioNotificationRemoteViews(context: Context, title: String, description: String, currentProgressSeconds: Int): RemoteViews {
            val remoteViews = RemoteViews(context.packageName, R.layout.remoteview_notification_audio_player)
            remoteViews.setTextViewText(R.id.ui_title, title)
            remoteViews.setTextViewText(R.id.ui_description, description)

            remoteViews.setTextViewText(R.id.ui_duration_view, AudioRecorderView.formatTime(currentProgressSeconds))

            remoteViews.setImageViewBitmap(R.id.ui_player_button, VectorIconHelper.getConvertedBitmap(context, R.drawable.ex))

            currentSessionId?.let {

                val stopIntent = Intent(context, OTAudioPlayService::class.java).setAction(INTENT_ACTION_STOP_ALL)

                remoteViews.setOnClickPendingIntent(R.id.ui_player_button, PendingIntent.getService(context, AUDIO_NOTIFICATION_ID, stopIntent, PendingIntent.FLAG_ONE_SHOT))
            }

            return remoteViews
        }

        private var lastNotificationBuilder: NotificationCompat.Builder? = null

        private fun putNotificationControl(context: Context, rv: RemoteViews) {
            val builder = lastNotificationBuilder ?: NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.icon_simple)
                    .setAutoCancel(false)
                    .setContentTitle("OmniTrack Audio Record Player") as NotificationCompat.Builder

            lastNotificationBuilder = builder

            val notification = builder.setContent(rv)
                    .build()

            notificationManager.notify(TAG, AUDIO_NOTIFICATION_ID, notification)
        }

        private fun dismissNotificationControl(context: Context) {
            notificationManager.cancel(TAG, AUDIO_NOTIFICATION_ID)
        }


        private val notificationManager: NotificationManager by lazy {
            (OTApplication.app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        }
    }

    private val binder = AudioServiceBinder(this)

    var currentFile: Uri? = null
        private set

    private var pausedPosition: Int = -1

    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private var title: String = ""
    private var description: String = ""

    private val commandReceiver = CommandReceiver()

    private val secondTicker = Ticker(1000)

    class AudioServiceBinder(val service: OTAudioPlayService) : Binder()

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    init {

        secondTicker.tick += {
            _, _ ->
            if (isSoundPlaying && currentSessionId != null) {
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(makeOnProgressEventIntent(currentSessionId!!, currentPlayPositionSecond, currentProgressRatio))

                putNotificationControl(this, makeAudioNotificationRemoteViews(this, title, description, currentPlayPositionSecond))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(commandReceiver, commandFilter)
    }

    private fun disposePlayer() {

        currentSessionId?.let {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(makeCompleteEventIntent(it))
        }

        currentSessionId = null

        reset()

        currentPlayer = null
        currentFile = null

        dismissNotificationControl(this)
    }

    private fun stopMedia(): Boolean {
        if (currentPlayer?.isPlaying == true) {
            currentPlayer?.stop()
            secondTicker.stop()
            return true
        } else return false
    }

    private fun pauseMedia(): Boolean {
        if (currentPlayer?.isPlaying == true) {
            currentPlayer?.pause()
            pausedPosition = currentPlayer?.currentPosition ?: -1

            secondTicker.stop()
            return true
        } else {
            return false
        }
    }

    private fun resumeMedia(): Boolean {
        if (currentPlayer?.isPlaying == false) {
            currentPlayer?.seekTo(pausedPosition)
            currentPlayer?.start()
            secondTicker.start()
            return true
        } else return false
    }

    private fun reset() {
        try {
            stopMedia()
            currentPlayer?.reset()
        } catch(e: Exception) {
            e.printStackTrace()
        } finally {
            currentPlayer?.release()
        }
    }

    private fun initPlayer() {
        currentPlayer = MediaPlayer()
        currentPlayer?.setOnCompletionListener(this)
        currentPlayer?.reset()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            currentPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        } else {
            currentPlayer?.setAudioAttributes(AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build())
        }
    }

    private fun playMedia() {
        if (currentPlayer?.isPlaying == false) {
            println("play current media")
            currentPlayer?.start()
            secondTicker.start()
            putNotificationControl(this, makeAudioNotificationRemoteViews(this, title, description, 0))
        }
    }

    private fun startNewFile(file: Uri): Boolean {
        try {
            currentPlayer?.let {
                it.setDataSource(file.path)
                this.currentFile = file
                val metadata = AudioRecordMetadata.readMetadata(file.path)
                if (metadata != null) {
                    description = String.format(resources.getString(R.string.msg_audio_record_player_description_recorded_at_format), metadata.recordedAt)
                }

                Thread {
                    it.prepare()
                    println("sound player prepared.")
                    playMedia()
                }.start()
            } ?: return false
            return true
        } catch(ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

    private fun requestAudioFocus(): Boolean {
        val focusResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).setOnAudioFocusChangeListener(this).build())
        } else {
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        return focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun removeAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocusRequest(AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).setOnAudioFocusChangeListener(this).build())
        } else {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusState: Int) {
        when (focusState) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                println("audioFocus gained")
                if (currentPlayer == null) {
                    initPlayer()
                    this.currentFile?.let {
                        startNewFile(it)
                    }
                } else playMedia()

                currentPlayer?.setVolume(1f, 1f)
            }

            AudioManager.AUDIOFOCUS_LOSS -> {

                println("lost audioFocus")
                disposePlayer()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                println("lost audioFocus transient")
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                println("lost audioFocus transient but can ignore")
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (currentPlayer?.isPlaying == true) {
                    currentPlayer?.setVolume(0.1f, 0.1f)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            INTENT_ACTION_PLAY -> {
                val filePath = intent.data
                val sessionId = intent.getStringExtra(INTENT_EXTRA_SESSION_ID)
                title = intent.getStringExtra(INTENT_EXTRA_AUDIO_TITLE)
                if (filePath != null && sessionId != null) {
                    Log.d(TAG, "Play audio file: ${filePath}, ${sessionId}")
                    if (currentSessionId != sessionId && currentPlayer?.isPlaying == true) {
                        disposePlayer()
                    }

                    initPlayer()
                    currentSessionId = sessionId
                    currentFile = filePath
                    if (!requestAudioFocus()) {
                        Log.d(TAG, "AudioFocus not granted. stop the service.")
                        stopSelf()
                    } else {
                        Log.d(TAG, "AudioFocus granted")
                        startNewFile(filePath)
                    }
                }
            }
            INTENT_ACTION_STOP_ALL -> {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMedia()
        disposePlayer()
        removeAudioFocus()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(commandReceiver)
    }

    override fun onCompletion(mediaPlayer: MediaPlayer?) {
        disposePlayer()
    }

    inner class CommandReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                INTENT_ACTION_STOP -> {
                    val sessionId = intent.getStringExtra(INTENT_EXTRA_SESSION_ID)
                    if (currentSessionId == sessionId) {
                        stopMedia()
                        disposePlayer()
                    }
                }
            }
        }

    }
}