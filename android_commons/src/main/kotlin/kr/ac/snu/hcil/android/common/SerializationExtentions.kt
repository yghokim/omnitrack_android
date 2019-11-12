package kr.ac.snu.hcil.android.common

import com.github.salomonbrys.kotson.set
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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

fun JsonObject.getDoubleCompat(key: String): Double? {
    return getElementCompat(key)?.asDouble
}


abstract class JsonDictBase<T>(val json: JsonObject?, val key: String): ReadWriteProperty<Any, T?> {

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        if(value!=null) {
            json?.set(key, value)
        }else{
            json?.remove(key)
        }
    }

}

class JsonDictString(json: JsonObject?, key: String): JsonDictBase<String>(json, key){
    override fun getValue(thisRef: Any, property: KProperty<*>): String? {
        return json?.getStringCompat(key)
    }
}

class JsonDictDouble(json: JsonObject?, key: String): JsonDictBase<Double>(json, key){
    override fun getValue(thisRef: Any, property: KProperty<*>): Double? {
        return json?.getDoubleCompat(key)
    }
}