package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import android.content.Context
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.reactivex.Completable
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService

/**
 * Created by younghokim on 2017. 4. 17..
 */
class OTBackgroundLoggingTriggerAction : OTTriggerAction() {
    class BackgroundLoggingActionTypeAdapter : TypeAdapter<OTBackgroundLoggingTriggerAction>() {
        override fun write(out: JsonWriter, value: OTBackgroundLoggingTriggerAction) {
            out.beginObject()
            out.name("notify").value(value.notify)
            out.endObject()
        }

        override fun read(reader: JsonReader): OTBackgroundLoggingTriggerAction {
            val result = OTBackgroundLoggingTriggerAction()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "notify" -> result.notify = reader.nextBoolean()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            return result
        }

    }

    companion object {
        val typeAdapter: BackgroundLoggingActionTypeAdapter by lazy {
            BackgroundLoggingActionTypeAdapter()
        }
    }

    override fun getSerializedString(): String {
        return typeAdapter.toJson(this)
    }

    var notify: Boolean = true

    override fun performAction(triggerTime: Long, context: Context): Completable {
        return Completable.defer {
            if (trigger.liveTrackerCount > 0) {
                context.startService(
                        OTItemLoggingService
                                .makeLoggingIntent(context,
                                        ItemLoggingSource.Trigger,
                                        notify,
                                        *(trigger.liveTrackersQuery.findAll().map { it.objectId!! }.toTypedArray()))
                )
            }
            Completable.complete()
        }
    }


}