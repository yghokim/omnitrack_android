package kr.ac.snu.hcil.android.common.containers

import android.util.SparseArray

abstract class CachedObjectPoolWithIntegerKey<T> {

    private val table = SparseArray<T>()

    fun get(key: Int): T {
        return table[key] ?: createNewInstance(key).apply { table.put(key, this) }
    }

    abstract fun createNewInstance(key: Int): T

}