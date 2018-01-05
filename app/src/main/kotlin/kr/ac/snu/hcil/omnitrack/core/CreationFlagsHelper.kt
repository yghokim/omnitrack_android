package kr.ac.snu.hcil.omnitrack.core

import com.github.salomonbrys.kotson.contains
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.utils.getBooleanCompat

/**
 * Created by younghokim on 2018-01-05.
 */
object CreationFlagsHelper {
    fun parseCreationFlags(json: String): JsonObject {
        return OTApp.instance.serializationComponent.genericGson().fromJson(json, JsonObject::class.java)
    }

    fun isInjected(flags: JsonObject): Boolean {
        return flags.getBooleanCompat("injected") ?: false
    }

    fun isForExperiment(flags: JsonObject): Boolean {
        return flags.contains("experiment")
    }
}