package kr.ac.snu.hcil.omnitrack.utils

import java.util.*

/**
 * Created by younghokim on 2017. 9. 25..
 *
 * This class manages a list of value and timestamp pairs.
 * This class is mainly used for maintaining attributes' history list.
 * When new value-timestamp pair or new list is input,
 * The class merges the input with current value, maintaining the list with latest timestamp and unique values.
 */
class NaiveRecentUniqueValueList<T>(val maxSize: Int) {
    data class Node<T>(val value: T, var timestamp: Long)

    private val list: PriorityQueue<Node<T>>

    init {
        list = PriorityQueue(maxSize, object : Comparator<Node<T>> {
            override fun compare(p0: Node<T>, p1: Node<T>): Int {
                if (p0 === p1) {
                    return 0
                } else return if (p0.timestamp - p1.timestamp > 0) 1 else -1
            }
        })
    }

    val finalList: List<Node<T>> get() = list.toList()

    fun insert(value: T, timestamp: Long) {
        list.find { node -> node.value == value }?.let { duplicate ->
            if (duplicate.timestamp < timestamp) {
                list.remove(duplicate)
                list.add(Node(value, timestamp))
                return
            } else return
        } ?: let {
            if (list.size < maxSize) {
                list.add(Node(value, timestamp))
                return
            } else { // when list is full
                if (list.last().timestamp < timestamp)//reject if timestamp is the lowest
                {
                    //remove current earliest, and add this one
                    list.poll()
                    list.add(Node(value, timestamp))
                }
            }

        }
    }


}