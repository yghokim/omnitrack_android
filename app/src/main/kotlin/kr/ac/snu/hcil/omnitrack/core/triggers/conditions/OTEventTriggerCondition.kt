package kr.ac.snu.hcil.omnitrack.core.triggers.conditions

import android.content.Context
import com.github.salomonbrys.kotson.toJson
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.OTAttachableFactory
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.event.OTEventFactory
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.system.OTEventFactoryManager

class OTEventTriggerCondition : ATriggerCondition(OTTriggerDAO.CONDITION_TYPE_EVENT){

    class ConditionTypeAdapter(val eventFactoryManager: OTEventFactoryManager, val gson: Lazy<Gson>): TypeAdapter<OTEventTriggerCondition>(){

        val eventTypeAdapter = OTAttachableFactory.OTAttachableTypeAdapter(gson){
            factoryCode, arguments ->
            eventFactoryManager.getFactoryByCode(factoryCode)?.makeAttachable(arguments)
        }

        override fun write(out: JsonWriter, value: OTEventTriggerCondition?) {
            if(value!=null){
                out.beginObject()
                out.name("event")
                eventTypeAdapter.write(out, value.attachedEvent)
                out.endObject()
            }else{
                out.nullValue()
            }
        }

        override fun read(reader: JsonReader): OTEventTriggerCondition? {
            if(reader.peek() == JsonToken.NULL){
                reader.skipValue()
                return null
            }else{
                val condition = OTEventTriggerCondition()
                reader.beginObject()

                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "event"->{
                            condition.attachedEvent = eventTypeAdapter.read(reader)
                        }
                        else -> reader.skipValue()
                    }
                }

                reader.endObject()
                return condition
            }
        }

    }


    var attachedEvent: OTEventFactory.OTAttachableEvent? = null

    override fun getSerializedString(): String {
        return OTApp.applicationComponent.eventConditionTypeAdapter().toJson(this)
    }

    override fun isConfigurationValid(context: Context): Single<Pair<Boolean, List<CharSequence>?>> {
        return Single.defer {
            val event = this.attachedEvent
            return@defer if (event == null) Single.just(Pair(false, listOf("No event is attached.")))
            else {
                val factory = event.getFactory<OTMeasureFactory>()
                if (factory is OTEventFactory) {
                    return@defer Single.just(Pair(true, null))
                } else {
                    return@defer Single.just(Pair(false, listOf("The attachable is not an event.")))
                }
            }
        }
    }

    override fun writeEventLogContent(table: JsonObject) {
        table.add("event_factory", attachedEvent?.factoryCode?.toJson())
    }

    override fun makeInformationText(): CharSequence {
        return "Actuated when the [" + attachedEvent?.getFormattedName() + "] event happens."
    }


    override fun equals(other: Any?): Boolean {
        return if (other === this) {
            true
        } else if (other is OTEventTriggerCondition) {
            attachedEvent == other.attachedEvent
        } else false
    }
}