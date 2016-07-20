package kr.ac.snu.hcil.omnitrack.utils.serialization

import com.google.gson.Gson
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-20.
 */
class SerializableGenericList(data: String?) {

    data class Entry(val key: Int, val type: String, val value: String)

    companion object {
        val TYPE_INT = Int.javaClass.simpleName
        val TYPE_FLOAT = Float.javaClass.simpleName
        val TYPE_LONG = Long.javaClass.simpleName
        val TYPE_STRING = String.javaClass.simpleName
        val TYPE_BOOLEAN = "Boolean"
        val TYPE_DOUBLE = Double.javaClass.simpleName
    }

    // Key/Type/Value
    private val values = Stack<Entry>()

    init {
        if (data != null) {
            values.addAll(Gson().fromJson<Array<Entry>>(data, Array<Entry>::class.java))
        }
    }

    fun addInt(key: Int, value: Int) {
        values.push(Entry(key, TYPE_INT, value.toString()))
    }

    fun addValue(key: Int, value: Any) {
        values.push(Entry(key, value.javaClass.simpleName, value.toString()))
    }

    //Key/Value
    fun get(): Pair<Int, Any> {
        val entry = values.pop()
        val value: Any = when (entry.type) {
            TYPE_INT -> entry.value.toInt()
            TYPE_BOOLEAN -> entry.value.toBoolean()
            TYPE_FLOAT -> entry.value.toDouble()
            TYPE_DOUBLE -> entry.value.toDouble()
            TYPE_LONG -> entry.value.toLong()
            else -> entry.value
        }

        println("parsed - ${value}")
        return Pair(entry.key, value)
    }

    fun getSerializedString(): String {

        val res = Gson().toJson(values.toTypedArray())
        println("serialized : ${res}")
        return res
    }
}