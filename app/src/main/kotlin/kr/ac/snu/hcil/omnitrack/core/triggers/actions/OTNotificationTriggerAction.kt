package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import android.content.Context
import android.content.Intent
import android.os.Vibrator
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.system.OTTrackingNotificationManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.activities.ReminderPopupActivity
import rx.Observable

/**
 * Created by younghokim on 2017. 4. 17..
 */
class OTNotificationTriggerAction(trigger: OTTrigger) : OTTriggerAction(trigger) {

    enum class NotificationLevel(@StringRes val nameRes: Int, @StringRes val descRes: Int, @DrawableRes val thumbnailRes: Int) {
        Noti(
                R.string.msg_notification_trigger_level_noti_name,
                R.string.msg_notification_trigger_level_noti_desc,
                R.drawable.thumb_notifications_noti
        ),
        Popup(
                R.string.msg_notification_trigger_level_popup_name,
                R.string.msg_notification_trigger_level_popup_desc,
                R.drawable.thumb_notifications_popup
        ),
        Impose(
                R.string.msg_notification_trigger_level_impose_name,
                R.string.msg_notification_trigger_level_impose_desc,
                R.drawable.thumb_notifications_impose
        )
    }

    companion object {
        const val KEY_NOTIFICATION_LEVEL = "notificationLevel"
    }

    var intrinsicNotificationLevel: NotificationLevel
        get() {
            return NotificationLevel.valueOf((trigger.properties.get(KEY_NOTIFICATION_LEVEL) as? String ?: NotificationLevel.Noti.name))
        }
        set(value) {
            trigger.properties.set(KEY_NOTIFICATION_LEVEL, value.name)
            trigger.syncPropertyToDatabase(KEY_NOTIFICATION_LEVEL, value.name)
            trigger.notifyPropertyChanged(KEY_NOTIFICATION_LEVEL, value.name)
        }

    val notificationLevelForSystem: NotificationLevel
        get() {
            return localNotificationLevel ?: intrinsicNotificationLevel
        }

    var localNotificationLevel: NotificationLevel?
        get() {
            val deviceSetting = OTTrigger.localSettingsPreferences.getString("${KEY_NOTIFICATION_LEVEL}_${trigger.objectId}", null)
            if (deviceSetting != null) {
                try {
                    return NotificationLevel.valueOf(deviceSetting)
                } catch(ex: Exception) {
                    ex.printStackTrace()
                    return null
                }
            } else return null
        }
        set(value) {
            if (value != null) {
                OTTrigger.localSettingsPreferences.edit().putString("${KEY_NOTIFICATION_LEVEL}_${trigger.objectId}", value.name).apply()
            } else {
                OTTrigger.localSettingsPreferences.edit().remove("${KEY_NOTIFICATION_LEVEL}_${trigger.objectId}").apply()
            }
        }


    override fun performAction(triggerTime: Long, context: Context): Observable<OTTrigger> {
        println("trigger fired - send notification")
        for (tracker in trigger.trackers) {
            when (notificationLevelForSystem) {
                NotificationLevel.Noti -> {
                    OTTrackingNotificationManager.pushReminderNotification(context, tracker, triggerTime)
                }
                NotificationLevel.Popup -> {
                    context.startActivity(Intent(context, ReminderPopupActivity::class.java))
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(longArrayOf(0, 400, 200, 400, 200, 400), -1)
                }
                NotificationLevel.Impose -> {

                }
            }
        }

        return Observable.just(trigger)
    }

}