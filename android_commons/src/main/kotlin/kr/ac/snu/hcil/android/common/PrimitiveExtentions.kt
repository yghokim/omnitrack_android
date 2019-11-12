package kr.ac.snu.hcil.android.common

import com.github.salomonbrys.kotson.set
import com.google.gson.JsonObject
import java.math.BigDecimal
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun Float.nearestInt(): Int {
    return (Math.abs(this) + .5f).toInt() * (if (this < 0) -1 else 1)
}

fun Boolean.toInt(): Int {
    return if (this) 1 else 0
}

fun Int.toBoolean(): Boolean {
    return this != 0
}

fun <T> arrayEquals(a: Array<T>?, b: Array<T>?): Boolean {
    return if (a != null && b != null) {
        a.contentEquals(b)
    } else a == null && b == null
}

fun arrayEquals(a: IntArray?, b: IntArray?): Boolean {
    return if (a != null && b != null) {
        a.contentEquals(b)
    } else a == null && b == null
}

fun List<*>.isSame(other: List<*>): Boolean {
    if (this.size == other.size) {
        for (i in 0 until this.size) {
            if (this[i] != other[i]) {
                return false
            }
        }
        return true
    } else return false
}

fun isNumericPrimitive(value: Any?): Boolean {
    return value is Number || value is BigDecimal
}

fun convertNumericToDouble(value: Any): Double {
    if (value is Number) {
        return value.toDouble()
    } else return value.toString().toDouble()
}

fun toBigDecimal(value: Any): BigDecimal {
    if (value is Int) {
        return BigDecimal(value)
    } else if (value is Long) {
        return BigDecimal(value)
    } else if (value is Double) {
        return BigDecimal(value)
    } else if (value is Float) {
        return BigDecimal(value.toDouble())
    } else if (value is BigDecimal) {
        return value
    } else throw Exception("value is not number primitive.")
}

fun List<*>.move(fromPosition: Int, toPosition: Int): Boolean {
    if (fromPosition != toPosition) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(this, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(this, i, i - 1)
            }
        }

        return true
    }
    return false
}