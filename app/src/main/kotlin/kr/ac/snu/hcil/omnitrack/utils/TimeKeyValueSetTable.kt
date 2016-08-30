package kr.ac.snu.hcil.omnitrack.utils

import java.util.*

/**
 * Created by younghokim on 16. 8. 29..
 */
class TimeKeyValueSetTable<T>(var thresholdMillis: Int, data: SortedMap<Long, MutableSet<T>>? = null) {

    /*
    enum class Range{
        EarlierThanKeyExclusive, EarlierThanKeyInclusive, LaterThanKeyExclusive, LaterThanKeyInclusive, BidirectionalExclusive, BidirectionalInclusive
    }*/

    private val map: TreeMap<Long, MutableSet<T>>

    val timeKeys: Set<Long>
        get() = map.keys

    init {
        if (data != null)
            map = TreeMap<Long, MutableSet<T>>(data)
        else map = TreeMap<Long, MutableSet<T>>()
    }


    fun findNearestTimeKey(timestamp: Long): Long? {
        if (map.size > 0) {
            //TODO more efficient search like binary search
            var minDiff = Long.MAX_VALUE
            var currentBest: Long? = null
            for (key in map.keys) {
                if (key == timestamp) return key
                else {
                    val diff = Math.abs(key - timestamp)

                    if (diff < minDiff && diff <= thresholdMillis) {
                        minDiff = diff
                        currentBest = key
                    }
                }
            }

            return currentBest
        } else return null
    }

    /**
     * return: if true, new alarm time.
     */
    fun appendAndCheckIsNewKey(timestamp: Long, data: T, resultPass: WritablePair<Long, Boolean>?): WritablePair<Long, Boolean> {
        val r = resultPass ?: WritablePair<Long, Boolean>(0, false)

        val nearestKey = findNearestTimeKey(timestamp)
        if (nearestKey == null) {
            val newSet = HashSet<T>()
            newSet.add(data)
            map[timestamp] = newSet
            r.first = timestamp
            r.second = true
            return r
        } else {
            map[nearestKey]?.add(data)
            r.first = nearestKey
            r.second = false
            return r
        }
    }

    operator fun get(timestamp: Long): Set<T>? {
        return map[timestamp] as? Set<T>
    }

    fun removeValueAndCheckIsTimestampEmpty(timestamp: Long, value: T): Boolean {
        val set = map[timestamp]
        if (set != null) {
            if (set.remove(value)) {
                if (set.isEmpty()) {
                    map.remove(timestamp)
                    return true
                } else return false
            } else return false
        } else return false
    }

    fun containsKey(timestamp: Long): Boolean {
        return (map[timestamp]?.size ?: 0) > 0
    }

    /**
     * returns number of triggers reserved
     */
    fun clearTimestamp(timestamp: Long): Int {
        val set = map[timestamp]
        if (set != null) {
            map.remove(timestamp)
            return set.size
        } else return 0
    }

    operator fun iterator(): Iterator<MutableMap.MutableEntry<Long, MutableSet<T>>> {
        return map.iterator()
    }
}