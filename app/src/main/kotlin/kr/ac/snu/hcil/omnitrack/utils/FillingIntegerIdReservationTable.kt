package kr.ac.snu.hcil.omnitrack.utils

import android.util.SparseArray
import java.util.*

/**
 * Created by younghokim on 16. 8. 29..
 */
class FillingIntegerIdReservationTable<T> {
    private val idTable = SparseArray<T>()
    private val keyPivotedTable = Hashtable<T, Int>()

    val ids: IntArray
        get() = keyPivotedTable.values.toIntArray()

    constructor() {

    }

    constructor(entries: Collection<Pair<Int, T>>) {
        for (entry in entries) {
            idTable.put(entry.first, entry.second)
            keyPivotedTable[entry.second] = entry.first
        }
    }

    fun getKeyFromId(id: Int): T? {
        return idTable[id]
    }

    operator fun get(key: T): Int {
        val id = keyPivotedTable[key]
        if (id != null) {
            return id
        } else {
            var pointer = -1
            while (idTable[++pointer] != null) {
            }

            idTable.put(pointer, key)

            keyPivotedTable[key] = pointer

            return pointer
        }
    }

    fun removeKey(key: T) {
        val id = keyPivotedTable[key]
        if (id != null) {
            println("remove Key $key from fillingIdTable. id: $id")
            idTable.removeAt(id)
            keyPivotedTable.remove(key)
        }
    }
}