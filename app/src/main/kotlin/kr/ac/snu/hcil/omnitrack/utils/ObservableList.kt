package kr.ac.snu.hcil.omnitrack.utils

import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class ObservableList<T>(){

    private val list = ArrayList<T>()

    val elementAdded = Event<Pair<T, Int>>()
    val elementRemoved = Event<Pair<T, Int>>()
    val elementIndexChanged = Event<Void>()

    val size: Int
        get() = list.size

    fun add(element: T): Boolean {
        if(list.add(element)) {
            elementAdded.invoke(this, Pair(element, list.size - 1))
            return true
        }
        else{
            return false
        }
    }

    operator fun get(index: Int): T {
        return list[index]
    }

    fun indexOf(element: T): Int {
        return list.indexOf(element)
    }

    fun remove(element: T): Boolean {
        val index = list.indexOf(element)
        if(index >= 0)
        {
            list.removeAt(index)
            elementRemoved.invoke(this, Pair(element, index))
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