package kr.ac.snu.hcil.omnitrack.utils

import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class ObservableList<T>(){

    private val list = ArrayList<T>()

    val elementAdded = Event<ReadOnlyPair<T, Int>>()
    val elementRemoved = Event<ReadOnlyPair<T, Int>>()
    val elementReordered = Event<IntRange>()

    val size: Int
        get() = list.size

    fun add(element: T): Boolean {
        if(list.add(element)) {
            elementAdded.invoke(this, ReadOnlyPair(element, list.size - 1))
            return true
        }
        else{
            return false
        }
    }

    fun addAt(element: T, position: Int): Boolean {
        try {
            list.add(position, element)
            elementAdded.invoke(this, ReadOnlyPair(element, position))
            return true
        } catch(e: Exception) {
            return false
        }
    }

    operator fun get(index: Int): T {
        return list[index]
    }

    fun <R> map(transform: (T) -> R): List<R> {
        return list.map(transform)
    }

    fun <R> mapNonNull(transform: (T) -> R?): List<R> {
        return list.map(transform).filter { it != null }.map { it!! }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (unObservedList.move(fromPosition, toPosition)) {
            if (fromPosition < toPosition) {
                elementReordered.invoke(this, IntRange(fromPosition, toPosition))

            } else {
                elementReordered.invoke(this, IntRange(toPosition, fromPosition))
            }
        }
    }

    fun indexOf(element: T): Int {
        return list.indexOf(element)
    }

    fun remove(element: T): Boolean {
        val index = list.indexOf(element)
        if(index >= 0)
        {
            list.removeAt(index)
            elementRemoved.invoke(this, ReadOnlyPair(element, index))
            return true
        }
        else{
            return false
        }
    }

    operator fun iterator(): MutableIterator<T> {
        return list.iterator()
    }

    fun toArray(): Array<out Any> {
        return list.toArray()
    }

    fun filter( func: (T)-> Boolean): List<T>{
        return list.filter(func)
    }

    val unObservedList : ArrayList<T>
        get(){
            return list
        }
}