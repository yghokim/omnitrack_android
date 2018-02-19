package kr.ac.snu.hcil.omnitrack.core

import com.github.salomonbrys.kotson.contains
import com.github.salomonbrys.kotson.set
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.utils.getBooleanCompat

/**
 * Created by younghokim on 2018-01-05.
 */
object CreationFlagsHelper : AFlagsHelperBase() {


    fun isInjected(flags: JsonObject): Boolean {
        return flags.getBooleanCompat("injected") ?: false
    }

    fun isForExperiment(flags: JsonObject): Boolean {
        return flags.contains("experiment")
    }

    class Builder : BuilderBase() {

        fun setExperiment(experimentId: String): Builder {
            this.json.set("experiment", experimentId)
            return this
        }

        fun setInjected(injected: Boolean): Builder {
            this.json.set("injected", injected)
            return this
        }
    }
}