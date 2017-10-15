package kr.ac.snu.hcil.omnitrack.utils

data class ValueWithTimestamp(var value: Any?, var timestamp: Long?) {
    override fun toString(): String {
        return "TimestampValueInfo{ timestamp: ${timestamp}, value: ${value}, valueType: ${value?.javaClass}}"
    }

    override fun equals(other: Any?): Boolean {
        return if (other === this) true else if (other is ValueWithTimestamp) {
            other.timestamp == timestamp && other.value == value
        } else false
    }
}