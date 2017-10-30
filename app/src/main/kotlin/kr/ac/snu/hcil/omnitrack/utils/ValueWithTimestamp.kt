package kr.ac.snu.hcil.omnitrack.utils

import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.SingleTransformer

data class ValueWithTimestamp(var value: Any?, var timestamp: Long?) {

    constructor(nullable: Nullable<out Any>):this(nullable.datum, if(nullable.datum==null)null else System.currentTimeMillis())

    override fun toString(): String {
        return "TimestampValueInfo{ timestamp: ${timestamp}, value: ${value}, valueType: ${value?.javaClass}}"
    }

    override fun equals(other: Any?): Boolean {
        return if (other === this) true else if (other is ValueWithTimestamp) {
            other.timestamp == timestamp && other.value == value
        } else false
    }
}