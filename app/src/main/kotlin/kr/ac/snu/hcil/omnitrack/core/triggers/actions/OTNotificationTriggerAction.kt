package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.os.Vibrator
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.activities.ReminderPopupActivity
import kr.ac.snu.hcil.omnitrack.utils.isDeviceLockedCompat
import kr.ac.snu.hcil.omnitrack.utils.isInteractiveCompat
import java.lang.ref.WeakReference

/**
 * Created by younghokim on 2017. 4. 17..
 */
class OTNotificationTriggerAction : OTTriggerAction() {

    class NotificationTriggerActionTypeAdapter : TypeAdapter<OTNotificationTriggerAction>() {
        override fun write(out: JsonWriter, value: OTNotificationTriggerAction) {
            out.beginObject()
            out.name("level").value(value.intrinsicNotificationLevel.ordinal)
            out.endObject()
        }

        override fun read(reader: JsonReader): OTNotificationTriggerAction {
            val action = OTNotificationTriggerAction()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "level" -> try {
                        action.intrinsicNotificationLevel = NotificationLevel.values()[reader.nextInt()]
                    } catch (ex: Exception) {
                        NotificationLevel.Noti
                    }
                }
            }
            reader.endObject()
            return action
        }

    }

    override fun getSerializedString(): String? {
        return parser.toJson(this, OTNotificationTriggerAction::class.java)
    }


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

        val parser: Gson by lazy {
            GsonBuilder().registerTypeAdapter(OTNotificationTriggerAction::class.java, NotificationTriggerActionTypeAdapter()).create()
        }

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

    var intrinsicNotificationLevel: NotificationLevel = NotificationLevel.Noti

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


    override fun performAction(triggerTime: Long, context: Context): Single<OTTriggerDAO> {
        println("trigger fired - send notification")
        when (notificationLevelForSystem) {
            NotificationLevel.Noti -> {
                for (tracker in trigger.trackers) {
                    TODO("Implement")
                    //OTTrackingNotificationManager.pushReminderNotification(context, tracker, triggerTime)
                }
            }
            NotificationLevel.Popup -> {

                val TAG = "ReminderPopup"

                if (popupTriggerQueueTime != triggerTime) {
                    Log.d(TAG, "there are remaining triggers: ${popupTriggersQueue.size}. Clear.")
                    popupTriggersQueue.clear()
                }

                Log.d(TAG, "add one trigger for ${triggerTime}}")
                //TODO
                // popupTriggersQueue.add(WeakReference(trigger))
                popupTriggerQueueTime = triggerTime
            }
            NotificationLevel.Impose -> {

            }
        }

        return Single.just(trigger)
    }

}