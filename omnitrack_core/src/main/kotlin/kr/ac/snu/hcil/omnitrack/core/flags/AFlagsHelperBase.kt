package kr.ac.snu.hcil.omnitrack.core.flags

import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * Created by younghokim on 2018. 2. 19..
 */
abstract class AFlagsHelperBase {
    companion object {

        val parser: Gson by lazy {
            Gson()
        }

        fun parseFlags(json: String): JsonObject {
            return parser.fromJson(json, JsonObject::class.java)
        }
    }

    fun parseFlags(json: String): JsonObject {
        return Companion.parseFlags(json)
    }

    open class BuilderBase {

        protected val json: JsonObject

        constructor() {
            this.json = JsonObject()
        }

        constructor(base: String) {
            this.json = parseFlags(base)
        }

        constructor(baseObj: JsonObject) {
            this.json = baseObj
        }

        fun build(): String {
            return this.json.toString()
        }
    }
}