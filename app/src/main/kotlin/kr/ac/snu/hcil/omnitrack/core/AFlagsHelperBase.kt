package kr.ac.snu.hcil.omnitrack.core

import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * Created by younghokim on 2018. 2. 19..
 */
abstract class AFlagsHelperBase {
    companion object {
        private val parser: Gson by lazy {
            Gson()
        }

        fun parseFlags(json: String): JsonObject {
            return parser.fromJson(json, JsonObject::class.java)
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