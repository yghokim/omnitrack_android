package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.os.Vibrator
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import io.reactivex.Completable
import kr.ac.snu.hcil.android.common.isDeviceLockedCompat
import kr.ac.snu.hcil.android.common.isInteractiveCompat
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.OTReminderCommands
import kr.ac.snu.hcil.omnitrack.ui.activities.ReminderPopupActivity
import java.lang.ref.WeakReference

/**
 * Created by younghokim on 2017. 4. 17..
 */
class OTReminderAction : OTTriggerAction() {

    class ReminderActionTypeAdapter : TypeAdapter<OTReminderAction>() {
        override fun write(out: JsonWriter, value: OTReminderAction) {
            out.beginObject()
            out.name("level").value(value.intrinsicNotificationLevel.ordinal)

            if (value.message?.isNotBlank() == true)
                out.name("message").value(value.message)

            out.name("durationSeconds").value(value.expirySeconds)

            out.endObject()
        }

        override fun read(reader: JsonReader): OTReminderAction {
            val action = OTReminderAction()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "level" -> try {
                        action.intrinsicNotificationLevel = NotificationLevel.values()[reader.nextInt()]
                    } catch (ex: Exception) {
                        NotificationLevel.Noti
                    }
                    "message" -> {
                        if (reader.peek() != JsonToken.NULL) {
                            action.message = reader.nextString()
                        } else {
                            reader.skipValue()
                        }
                    }
                    "durationSeconds" -> {
                        if (reader.peek() != JsonToken.NULL) {
                            action.expirySeconds = reader.nextInt()
                        } else {
                            reader.skipValue()
                        }
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return action
        }

    }

    override fun getSerializedString(): String {
        return typeAdapter.toJson(this)
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

        const val EXPIRY_INDEFINITE = 0

        val typeAdapter: TypeAdapter<OTReminderAction> by lazy {
            ReminderActionTypeAdapter()
        }

        val popupTriggersQueue = ArrayList<WeakReference<OTTriggerDAO>>()
        var popupTriggerQueueTime: Long? = null

        fun notifyPopupQueue(context: Context) {
            val triggerTime = popupTriggerQueueTime
            if (triggerTime != null && popupTriggersQueue.isNotEmpty()) {
                val TAG = "ReminderPopup"

                Log.d(TAG, "notify popup for ${popupTriggersQueue.size} triggers at $popupTriggerQueueTime")
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

    var message: String? = null

    var expirySeconds: Int = 0

    var intrinsicNotificationLevel: NotificationLevel = NotificationLevel.Noti

    val notificationLevelForSystem: NotificationLevel
        get() {
            //return localNotificationLevel ?: intrinsicNotificationLevel
            return intrinsicNotificationLevel
        }

    val expiryMilliSeconds: Int?
        get() {
            return if (expirySeconds == EXPIRY_INDEFINITE || expirySeconds < 0 || expirySeconds == Int.MAX_VALUE) null else (expirySeconds * 1000)
        }

    /*
    var localNotificationLevel: NotificationLevel?
        get() {
            val deviceSetting = localSettingsPreferences.getString("${KEY_NOTIFICATION_LEVEL}_${trigger.objectId}", null)
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
                localSettingsPreferences.edit().putString("${KEY_NOTIFICATION_LEVEL}_${trigger.objectId}", value.name).apply()
            } else {
                localSettingsPreferences.edit().remove("${KEY_NOTIFICATION_LEVEL}_${trigger.objectId}").apply()
            }
        }*/


    override fun performAction(trigger: OTTriggerDAO, triggerTime: Long, metadata: JsonObject, context: Context): Completable {
        return Completable.defer {
            println("trigger fired - send notification")

            if (trigger.liveTrackerCount > 0) {
                val reminderCommands = OTReminderCommands(context.applicationContext)
                return@defer reminderCommands.remind(trigger.objectId!!, triggerTime, metadata)
            }
            return@defer Completable.complete()
        }
    }

    override fun clone(): OTTriggerAction {
        return OTReminderAction().let {
            it.intrinsicNotificationLevel = this.intrinsicNotificationLevel
            it.expirySeconds = this.expirySeconds
            it.message = this.message
            it
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other === this) {
            true
        } else if (other is OTReminderAction) {
            other.expirySeconds == this.expirySeconds
                    && other.intrinsicNotificationLevel == this.intrinsicNotificationLevel
                    && other.message == this.message
        } else false
    }

}