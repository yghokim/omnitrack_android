package kr.ac.snu.hcil.omnitrack.services

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
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
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioRecordMetadata
import kr.ac.snu.hcil.omnitrack.utils.VectorIconHelper
import org.jetbrains.anko.notificationManager
import java.util.concurrent.TimeUnit

/**
 * Created by Young-Ho on 4/22/2017.
 * Followed code from https://www.sitepoint.com/a-step-by-step-guide-to-building-an-android-audio-player-instance/
 */
class OTAudioPlayService : Service(), MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {


    companion object {
        const val TAG = "AudioService"

        private const val AUDIO_NOTIFICATION_ID = 2442

        const val INTENT_EXTRA_SESSION_ID = "audioPlaySessionId"
        const val INTENT_EXTRA_CURRENT_POSITION_SECONDS = "audioCurrentDurationSeconds"
        const val INTENT_EXTRA_CURRENT_PROGRESS_RATIO = "audioCurrentProgressRatio"
        const val INTENT_EXTRA_AUDIO_TITLE = "audioTitle"

        const val INTENT_ACTION_PLAY = "${OTApp.PREFIX_ACTION}.AUDIO_PLAY"
        const val INTENT_ACTION_PAUSE = "${OTApp.PREFIX_ACTION}.AUDIO_PAUSE"
        const val INTENT_ACTION_RESUME = "${OTApp.PREFIX_ACTION}.AUDIO_RESUME"
        const val INTENT_ACTION_STOP = "${OTApp.PREFIX_ACTION}.AUDIO_STOP"
        const val INTENT_ACTION_STOP_ALL = "${OTApp.PREFIX_ACTION}.AUDIO_STOP_ALL"

        const val INTENT_ACTION_EVENT_AUDIO_COMPLETED = "${OTApp.PREFIX_ACTION}.AUDIO_COMPLETED"

        const val INTENT_ACTION_EVENT_AUDIO_PROGRESS = "${OTApp.PREFIX_ACTION}.AUDIO_PROGRESS"

        var currentSessionId: String? = null
            private set
        /*
            get(){
                return OTApp.instance.systemSharedPreferences.getString(INTENT_EXTRA_SESSION_ID, null)
            }
            private set(value){
                if(value!=null)
                {
                    OTApp.instance.systemSharedPreferences.edit().putString(INTENT_EXTRA_SESSION_ID, value).apply()
                }
                else{
                    OTApp.instance.systemSharedPreferences.edit().remove(INTENT_EXTRA_SESSION_ID).apply()
                }
            }
            */

        val player: MediaPlayer = MediaPlayer().apply {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setAudioAttributes(AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build())
            } else {
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
        }

        val currentPlayPositionSecond: Int
            get() = ((player.currentPosition) / 1000)

        val currentProgressRatio: Float
            get() = player.currentPosition / player.duration.toFloat()

        val isSoundPlaying: Boolean
            get() = try {
                player.isPlaying
            } catch (ex: IllegalStateException) {
                false
            }

        private val commandFilter = IntentFilter().apply {
            addAction(INTENT_ACTION_STOP)
            addAction(INTENT_ACTION_PAUSE)
            addAction(INTENT_ACTION_RESUME)
        }


        fun makePlayIntent(context: Context, file: Uri, sessionId: String, title: String = "Audio Record"): Intent {
            return Intent(context, OTAudioPlayService::class.java).setAction(INTENT_ACTION_PLAY).setData(file)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
                    .putExtra(INTENT_EXTRA_AUDIO_TITLE, title)
        }

        fun makeStopCommandIntent(sessionId: String): Intent =
                Intent(INTENT_ACTION_STOP).putExtra(INTENT_EXTRA_SESSION_ID, sessionId)

        fun makePauseCommandIntent(sessionId: String): Intent = Intent(INTENT_ACTION_PAUSE).putExtra(INTENT_EXTRA_SESSION_ID, sessionId)

        fun makeResumeCommandIntent(sessionId: String): Intent = Intent(INTENT_ACTION_RESUME).putExtra(INTENT_EXTRA_SESSION_ID, sessionId)


        private fun makeCompleteEventIntent(sessionId: String): Intent =
                Intent(INTENT_ACTION_EVENT_AUDIO_COMPLETED).putExtra(INTENT_EXTRA_SESSION_ID, sessionId)

        private fun makeOnProgressEventIntent(sessionId: String, progressDurationSecond: Int, progressRatio: Float): Intent {
            return Intent(INTENT_ACTION_EVENT_AUDIO_PROGRESS)
                    .putExtra(INTENT_EXTRA_SESSION_ID, sessionId)
                    .putExtra(INTENT_EXTRA_CURRENT_POSITION_SECONDS, progressDurationSecond)
                    .putExtra(INTENT_EXTRA_CURRENT_PROGRESS_RATIO, progressRatio)
        }

        private fun makeAudioNotificationRemoteViews(context: Context, title: String, description: String): RemoteViews {
            val remoteViews = RemoteViews(context.packageName, R.layout.remoteview_notification_audio_player)
            remoteViews.setImageViewBitmap(R.id.ui_audio_icon, VectorIconHelper.getConvertedBitmap(context, R.drawable.icon_small_audio, tint = Color.WHITE))
            remoteViews.setTextViewText(R.id.ui_title, title)
            remoteViews.setTextViewText(R.id.ui_description, description)

            remoteViews.setChronometer(R.id.ui_duration_view, SystemClock.elapsedRealtime(), null, true)

            remoteViews.setImageViewBitmap(R.id.ui_player_button, VectorIconHelper.getConvertedBitmap(context, R.drawable.ex))

            currentSessionId?.let {

                val stopIntent = Intent(context, OTAudioPlayService::class.java).setAction(INTENT_ACTION_STOP_ALL)

                remoteViews.setOnClickPendingIntent(R.id.ui_player_button, PendingIntent.getService(context, AUDIO_NOTIFICATION_ID, stopIntent, PendingIntent.FLAG_ONE_SHOT))
            }

            return remoteViews
        }

        private var lastNotificationBuilder: NotificationCompat.Builder? = null

        private fun putNotificationControl(context: Context, rv: RemoteViews) {
            val builder = lastNotificationBuilder
                    ?: NotificationCompat.Builder(context, OTNotificationManager.CHANNEL_ID_WIDGETS)
                            .setSmallIcon(R.drawable.icon_simple)
                            .setAutoCancel(false)
                            .setOngoing(true)
                            .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_SOUND).setVibrate(longArrayOf())
                            .setContentTitle("OmniTrack Audio Record Player") as NotificationCompat.Builder

            lastNotificationBuilder = builder

            val notification = builder.setContent(rv)
                    .build()

            context.notificationManager.notify(TAG, AUDIO_NOTIFICATION_ID, notification)
        }

        private fun dismissNotificationControl(context: Context) {
            context.notificationManager.cancel(TAG, AUDIO_NOTIFICATION_ID)
        }
    }

    private val binder = AudioServiceBinder(this)

    private var currentFile: Uri? = null

    private var pausedPosition: Int = -1

    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private var title: String = ""
    private var description: String = ""

    private val commandReceiver = CommandReceiver()

    class AudioServiceBinder(val service: OTAudioPlayService) : Binder()

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    private var ticker: Disposable? = null

    override fun onCreate() {
        super.onCreate()

        player.setOnCompletionListener(this)

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(commandReceiver, commandFilter)
    }

    private fun disposePlayer() {

        currentSessionId?.let {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(makeCompleteEventIntent(it))
        }

        currentSessionId = null

        player.reset()
        currentFile = null

        dismissNotificationControl(this)

        this.ticker?.dispose()
        this.ticker = null
    }

    private fun stopMedia(): Boolean {
        return try {
            player.stop()
            true
        } catch (ex: IllegalStateException) {
            false
        }
    }

    private fun pauseMedia(): Boolean {
        if (isSoundPlaying == true) {
            player.pause()
            pausedPosition = player.currentPosition
            return true
        } else {
            return false
        }
    }

    private fun resumeMedia(): Boolean {
        if (isSoundPlaying == false) {
            player.seekTo(pausedPosition)
            player.start()
            return true
        } else return false
    }

    private fun playMedia() {
        if (isSoundPlaying == false) {
            println("play current media")
            player.start()
            putNotificationControl(this, makeAudioNotificationRemoteViews(this, title, description))
            this.ticker = Observable.interval(50, TimeUnit.MILLISECONDS).subscribe {
                if (isSoundPlaying && currentSessionId != null) {
                    LocalBroadcastManager.getInstance(this)
                            .sendBroadcast(makeOnProgressEventIntent(currentSessionId!!, currentPlayPositionSecond, currentProgressRatio))
                }
            }
        }
    }

    private fun startNewFile(file: Uri): Boolean {
        try {
            player.setDataSource(file.path)
                this.currentFile = file
                val metadata = AudioRecordMetadata.readMetadata(file.path, this)
                if (metadata != null) {
                    description = String.format(resources.getString(R.string.msg_audio_record_player_description_recorded_at_format), metadata.recordedAt)
                }

                Thread {
                    player.prepare()
                    println("sound player prepared.")
                    playMedia()
                }.start()
            return true
        } catch (ex: Exception) {
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
                if (player == null) {
                    this.currentFile?.let {
                        startNewFile(it)
                    }
                } else playMedia()

                player.setVolume(1f, 1f)
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
                if (isSoundPlaying == true) {
                    player.setVolume(0.1f, 0.1f)
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
                    Log.d(TAG, "Play audio file: $filePath, $sessionId")
                    if (currentSessionId != sessionId && isSoundPlaying == true) {
                        disposePlayer()
                    }

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
        player.release()
        removeAudioFocus()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(commandReceiver)
    }

    override fun onCompletion(mediaPlayer: MediaPlayer?) {
        disposePlayer()
    }

    inner class CommandReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            println("Received play service local broadcast message- ${intent.action}")
            val sessionId = intent.getStringExtra(INTENT_EXTRA_SESSION_ID)
            if (currentSessionId == sessionId) {
                when (intent.action) {
                    INTENT_ACTION_STOP -> {

                        println("Play service - stop media")
                        stopMedia()
                        disposePlayer()
                    }
                    INTENT_ACTION_PAUSE -> {
                        println("Play service - pause media")
                        pauseMedia()
                    }
                    INTENT_ACTION_RESUME -> {

                        println("Play service - resume media")
                        resumeMedia()
                    }
                }
            }
        }
    }
}