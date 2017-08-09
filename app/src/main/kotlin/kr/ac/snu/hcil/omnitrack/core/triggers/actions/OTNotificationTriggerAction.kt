package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.os.Vibrator
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.util.Log
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.system.OTTrackingNotificationManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.activities.ReminderPopupActivity
import kr.ac.snu.hcil.omnitrack.utils.isDeviceLockedCompat
import kr.ac.snu.hcil.omnitrack.utils.isInteractiveCompat
import rx.Observable
import java.lang.ref.WeakReference

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

        val popupTriggersQueue = ArrayList<WeakReference<OTTrigger>>()
        var popupTriggerQueueTime: Long? = null

        fun notifyPopupQueue(context: Context) {
            val triggerTime = popupTriggerQueueTime
            if (triggerTime != null && popupTriggersQueue.isNotEmpty()) {
                val TAG = "ReminderPopup"

                Log.d(TAG, "notify popup for ${popupTriggersQueue.size} triggers at ${popupTriggerQueueTime}")
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                Log.d(TAG, "interactiveMode: ${powerManager.isInteractiveCompat}, locked: ${keyguardManager.isDeviceLockedCompat}")
                //phone locked, screen off: activity
                //phone unlocked, screen off: activity
                //phone locked, screen on: activity
                //phone unlocked, screen on: window view


                if (!keyguardManager.isDeviceLockedCompat && powerManager.isInteractiveCompat) {
                    println("device is unlocked. show popup as window view.")
                } else {
                    println("device is locked. show popup as activity.")
                    context.startActivity(ReminderPopupActivity.makeIntent(context, "asdfaffsdfa", popupTriggerQueueTime!!))
                }

                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(400)

                popupTriggerQueueTime = null
                popupTriggersQueue.clear()
            }
        }
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
        when (notificationLevelForSystem) {
            NotificationLevel.Noti -> {
                for (tracker in trigger.trackers) {
                    OTTrackingNotificationManager.pushReminderNotification(context, tracker, triggerTime)
                }
            }
            NotificationLevel.Popup -> {

                val TAG = "ReminderPopup"

                if (popupTriggerQueueTime != triggerTime) {
                    Log.d(TAG, "there are remaining triggers: ${popupTriggersQueue.size}. Clear.")
                    popupTriggersQueue.clear()
                }

                Log.d(TAG, "add one trigger for ${triggerTime}}")
                popupTriggersQueue.add(WeakReference(trigger))
                popupTriggerQueueTime = triggerTime
            }
            NotificationLevel.Impose -> {

            }
        }

        return Observable.just(trigger)
    }

}