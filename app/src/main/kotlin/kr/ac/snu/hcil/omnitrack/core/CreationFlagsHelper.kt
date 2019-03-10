package kr.ac.snu.hcil.omnitrack.core

import com.github.salomonbrys.kotson.set
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.core.serialization.getBooleanCompat
import kr.ac.snu.hcil.omnitrack.core.serialization.getStringCompat

/**
 * Created by younghokim on 2018-01-05.
 */
object CreationFlagsHelper : AFlagsHelperBase() {


    fun isInjected(flags: JsonObject): Boolean {
        return flags.getBooleanCompat("injected") ?: false
    }

    fun isForExperiment(flags: JsonObject): Boolean {
        return !flags.getStringCompat("experiment").isNullOrBlank()
    }

    fun getExperimentId(flags: JsonObject): String? {
        return flags.getStringCompat("experiment")
    }

    class Builder : BuilderBase {
        constructor() : super()
        constructor(base: String) : super(base)

        fun setExperiment(experimentId: String?): Builder {
            this.json.set("experiment", experimentId)
            return this
        }

        fun setInjected(injected: Boolean): Builder {
            this.json.set("injected", injected)
            return this
        }
    }
}