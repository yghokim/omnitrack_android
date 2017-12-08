package kr.ac.snu.hcil.omnitrack.utils

import android.support.annotation.Keep

@Keep
open class ValueWithTimestamp<T>(open var value: T?, open var timestamp: Long?)

data class AnyValueWithTimestamp(override var value: Any?, override var timestamp: Long?) : ValueWithTimestamp<Any>(value, timestamp) {

    constructor(nullable: Nullable<out Any>):this(nullable.datum, if(nullable.datum==null)null else System.currentTimeMillis())

    override fun toString(): String {
        return "TimestampValueInfo{ timestamp: ${timestamp}, value: ${value}, valueType: ${value?.javaClass}}"
    }

    override fun equals(other: Any?): Boolean {
        return if (other === this) true else if (other is AnyValueWithTimestamp) {
            other.timestamp == timestamp && other.value == value
        } else false
    }
}