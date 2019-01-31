package kr.ac.snu.hcil.omnitrack.core.system

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kr.ac.snu.hcil.omnitrack.R
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.powerManager
import org.jetbrains.anko.runOnUiThread

/**
 * This class manages configurations that have to be set via external intent.
 */
class OTExternalSettingsPrompter(private val context: Context) {

    companion object {

        const val KEY_IGNORE_BATTERY_OPTIMIZATION = "ignore_battery_optimization"

        const val REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATION = 23123
        const val TAG = "OTExternalSettingsPrompter"

        const val NOTIFICATION_ID_IGNORE_BATTERY_OPTIMIZATION = 0


        fun isBatteryOptimizationWhiteListed(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= 23)
                context.powerManager.isIgnoringBatteryOptimizations(context.packageName)
            else true
        }
    }

    fun isBatteryOptimizationWhiteListed(): Boolean {
        return isBatteryOptimizationWhiteListed(context)
    }

    @SuppressLint("BatteryLife")
    fun askUserBatterOptimizationWhitelist() {
        if (Build.VERSION.SDK_INT >= 23) {

            val settingIntent = if (isBatteryOptimizationWhiteListed()) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            } else {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:" + context.packageName))
            }

            if (context is Activity) {
                //dialog
                context.startActivityForResult(settingIntent, REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATION)
            } else {
                //notification
                println("$TAG show notification")
                val notificationBuilder = NotificationCompat.Builder(context, OTNotificationManager.CHANNEL_ID_IMPORTANT)
                        .setColor(ContextCompat.getColor(context, R.color.colorRed))
                        .setSmallIcon(R.drawable.icon_simple)
                        .setStyle(NotificationCompat.BigTextStyle()
                                .setBigContentTitle("Turn off battery optimization")
                                .bigText("Don't optimize battery to improve accuracy of triggers and reminders."))
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .setContentTitle("Turn off battery optimization")
                        .setContentText("Don't optimize battery to improve accuracy of triggers and reminders.")
                        .setContentIntent(PendingIntent.getActivity(context, REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATION, settingIntent, PendingIntent.FLAG_CANCEL_CURRENT))

                context.runOnUiThread {
                    context.notificationManager.notify(TAG, NOTIFICATION_ID_IGNORE_BATTERY_OPTIMIZATION, notificationBuilder.build())
                }
            }
        }
    }

    fun handleBatteryOptimizationWhitelistResult(onWhitelist: Boolean) {
        context.notificationManager.cancel(TAG, NOTIFICATION_ID_IGNORE_BATTERY_OPTIMIZATION)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Pair<String, Boolean>? {
        when (requestCode) {
            REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATION -> {
                if (isBatteryOptimizationWhiteListed()) {
                    handleBatteryOptimizationWhitelistResult(true)
                }
                return Pair(KEY_IGNORE_BATTERY_OPTIMIZATION, isBatteryOptimizationWhiteListed())
            }
        }
        return null
    }
}