package kr.ac.snu.hcil.omnitrack.core.system

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.v4.app.NotificationCompat
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.R
import org.jetbrains.anko.notificationManager

/**
 * Created by younghokim on 2017-08-09.
 */
object OTNotificationManager {
    const val CHANNEL_ID_WIDGETS = "${BuildConfig.APPLICATION_ID}.notification.channel.widgets"
    const val CHANNEL_ID_IMPORTANT = "${BuildConfig.APPLICATION_ID}.notification.channel.important"
    const val CHANNEL_ID_SYSTEM = "${BuildConfig.APPLICATION_ID}.notification.channel.system"
    const val CHANNEL_ID_NOTICE = "${BuildConfig.APPLICATION_ID}.notification.channel.notice"

    @TargetApi(Build.VERSION_CODES.O)
    fun refreshChannels(context: Context) {
        val widgetsChannel = NotificationChannel(CHANNEL_ID_WIDGETS, context.getString(R.string.msg_notification_channel_name_widgets), NotificationManager.IMPORTANCE_HIGH)
        val alertChannel = NotificationChannel(CHANNEL_ID_IMPORTANT, context.getString(R.string.msg_notification_channel_name_alerts), NotificationManager.IMPORTANCE_HIGH)
        val systemChannel = NotificationChannel(CHANNEL_ID_SYSTEM, context.getString(R.string.msg_notification_channel_name_system_messages), NotificationManager.IMPORTANCE_DEFAULT)
        val noticeChannel = NotificationChannel(CHANNEL_ID_NOTICE, context.getString(R.string.msg_notification_channel_name_notice), NotificationManager.IMPORTANCE_LOW)


        widgetsChannel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        widgetsChannel.enableVibration(false)
        widgetsChannel.enableLights(false)

        noticeChannel.enableVibration(false)
        noticeChannel.enableLights(true)

        val channels = listOf(widgetsChannel, alertChannel, systemChannel, noticeChannel)
        context.notificationManager.createNotificationChannels(channels)
    }
}