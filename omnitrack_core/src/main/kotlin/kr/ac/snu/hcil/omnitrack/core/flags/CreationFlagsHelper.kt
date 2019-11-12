package kr.ac.snu.hcil.omnitrack.core.flags

import com.google.gson.JsonObject
import kr.ac.snu.hcil.android.common.getBooleanCompat
import kr.ac.snu.hcil.android.common.getStringCompat

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
            this.json.addProperty("experiment", experimentId)
            return this
        }

        fun setInjected(injected: Boolean): Builder {
            this.json.addProperty("injected", injected)
            return this
        }
    }
}