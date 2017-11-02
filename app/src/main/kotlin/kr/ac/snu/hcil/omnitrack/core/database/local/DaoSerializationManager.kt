package kr.ac.snu.hcil.omnitrack.core.database.local

import com.google.gson.Gson
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.di.ApplicationScope
import kr.ac.snu.hcil.omnitrack.core.di.ForAttribute
import kr.ac.snu.hcil.omnitrack.core.di.ForTrigger
import javax.inject.Inject

/**
 * Created by younghokim on 2017-11-02.
 */
@ApplicationScope
class DaoSerializationManager @Inject constructor(@ForTrigger val triggerGson: Lazy<Gson>, @ForAttribute val attributeGson: Lazy<Gson>) {

    fun parseTrigger(triggerJson: String): OTTriggerDAO {
        return triggerGson.get().fromJson(triggerJson, OTTriggerDAO::class.java)
    }

    fun serializeTrigger(trigger: OTTriggerDAO): String {
        return triggerGson.get().toJson(trigger, OTTriggerDAO::class.java)
    }

    fun parseAttribute(attrJson: String): OTAttributeDAO {
        return attributeGson.get().fromJson(attrJson, OTAttributeDAO::class.java)
    }

    fun serializeAttribute(attribute: OTAttributeDAO): String{
        return attributeGson.get().toJson(attribute, OTAttributeDAO::class.java)
    }
}