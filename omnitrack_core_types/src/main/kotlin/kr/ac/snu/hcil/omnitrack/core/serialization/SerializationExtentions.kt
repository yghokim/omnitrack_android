package kr.ac.snu.hcil.omnitrack.core.serialization

import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun JsonObject.getElementCompat(key: String): JsonElement? {
    val value = this[key]
    if (value?.isJsonNull != false) {
        return null
    } else return value
}

fun JsonObject.getStringCompat(key: String): String? {
    val element = getElementCompat(key)
    return try {
        element?.asString
    } catch (ex: UnsupportedOperationException) {
        element?.toString()
    }
}

fun JsonObject.getBooleanCompat(key: String): Boolean? {
    return getElementCompat(key)?.asBoolean
}

fun JsonObject.getIntCompat(key: String): Int? {
    return getElementCompat(key)?.asInt
}

fun JsonObject.getLongCompat(key: String): Long? {
    return getElementCompat(key)?.asLong
}