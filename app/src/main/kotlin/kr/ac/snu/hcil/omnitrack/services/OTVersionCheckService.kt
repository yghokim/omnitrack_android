package kr.ac.snu.hcil.omnitrack.services

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import br.com.goncalves.pugnotification.notification.PugNotification
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.RemoteConfigManager
import rx.subscriptions.SerialSubscription
import rx.subscriptions.Subscriptions

/**
 * Created by younghokim on 2017. 4. 15..
 */
class OTVersionCheckService : Service() {

    companion object {
        const val TAG = "VersionCheckService"

        const val REQUEST_CODE = 20

        const val PREF_LAST_NOTIFIED_VERSION = "last_notified_version"

        private val checkSubscription = SerialSubscription()

        fun setupServiceAlarm(context: Context) {
            val serviceIntent = Intent(context, OTVersionCheckService::class.java)
            val pendingIntent = PendingIntent.getService(context, REQUEST_CODE, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_check_updates", false)) {
                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 1000, 7200 * 1000, pendingIntent)
            } else {
                alarmManager.cancel(pendingIntent)
                context.stopService(serviceIntent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        synchronized(checkSubscription)
        {
            Log.d(TAG, "check latest version of OmniTrack...: Service")
            RemoteConfigManager.getServerLatestVersionName().subscribe({
                versionName ->
                Log.d(TAG, "received version name.: ${versionName}")
                if (BuildConfig.DEBUG || RemoteConfigManager.isNewVersionGreater(BuildConfig.VERSION_NAME, versionName)) {
                    val lastNotifiedVersion = OTApplication.app.systemSharedPreferences.getString(PREF_LAST_NOTIFIED_VERSION, "")
                    if (lastNotifiedVersion != versionName) {

                        if (!OTApplication.app.isAppInForeground) {
                            Log.d(TAG, "app is in background. send notification.")
                            PugNotification.with(this).load()
                                    .color(R.color.colorPointed)
                                    .identifier(REQUEST_CODE)
                                    .tag(TAG)
                                    .title(R.string.msg_notification_new_version_detected_title)
                                    .message(R.string.msg_notification_new_version_detected_text)
                                    .largeIcon(R.drawable.icon_simple)
                                    .smallIcon(R.drawable.icon_simple)
                                    .flags(Notification.DEFAULT_ALL)
                                    .autoCancel(true)
                                    .click {
                                        PendingIntent.getActivity(this, REQUEST_CODE,
                                                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=kr.ac.snu.hcil.omnitrack")),
                                                PendingIntent.FLAG_UPDATE_CURRENT)
                                    }
                                    .simple()
                                    .build()


                            OTApplication.app.systemSharedPreferences.edit()
                                    .putString(PREF_LAST_NOTIFIED_VERSION, versionName)
                                    .apply()
                        } else {

                            Log.d(TAG, "app is in foreground. send broadcast.")
                            PugNotification.with(this).cancel(TAG, REQUEST_CODE)
                            val intent = Intent(OTApplication.BROADCAST_ACTION_NEW_VERSION_DETECTED)
                            intent.putExtra(OTApplication.INTENT_EXTRA_LATEST_VERSION_NAME, versionName)

                            sendBroadcast(intent)
                        }
                    } else {
                        Log.d(TAG, "this version was already notified. ignore notification.")
                    }
                }
            }, { e -> e.printStackTrace() })
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        checkSubscription.set(Subscriptions.empty())
    }
}