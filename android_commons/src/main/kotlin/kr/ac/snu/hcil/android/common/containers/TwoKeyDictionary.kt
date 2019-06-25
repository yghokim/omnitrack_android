package kr.ac.snu.hcil.android.common.containers

import android.util.ArrayMap

class TwoKeyDictionary<Key1Type, Key2Type, ValueType> {

    private val key1Pivots = ArrayMap<Key1Type, ArrayMap<Key2Type, ValueType>>()

    fun put(key1: Key1Type, key2: Key2Type, value: ValueType) {
        val key2Map = key1Pivots[key1]
        if (key2Map != null) {
            key2Map[key2] = value
        } else {
            val newMap = ArrayMap<Key2Type, ValueType>()
            newMap[key2] = value
            key1Pivots[key1] = newMap
        }
    }

    fun get(key1: Key1Type, key2: Key2Type): ValueType? {
        val key2Map = key1Pivots[key1]
        if (key2Map != null) {
            return key2Map[key2]
        } else {
            return null
        }
    }

    fun getWithFirstKey(key1: Key1Type): Map<Key2Type, ValueType>? {
        return key1Pivots[key1]?.toMap()
    }
}