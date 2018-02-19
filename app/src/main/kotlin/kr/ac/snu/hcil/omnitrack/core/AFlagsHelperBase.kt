package kr.ac.snu.hcil.omnitrack.core

import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApp

/**
 * Created by younghokim on 2018. 2. 19..
 */
abstract class AFlagsHelperBase {
    companion object {
        fun parseFlags(json: String): JsonObject {
            return OTApp.instance.serializationComponent.genericGson().fromJson(json, JsonObject::class.java)
        }
    }

    fun parseFlags(json: String): JsonObject {
        return AFlagsHelperBase.parseFlags(json)
    }

    open class BuilderBase {

        protected val json: JsonObject

        constructor() {
            this.json = JsonObject()
        }

        constructor(base: String) {
            this.json = parseFlags(base)
        }

        fun build(): String {
            return this.json.toString()
        }
    }
}