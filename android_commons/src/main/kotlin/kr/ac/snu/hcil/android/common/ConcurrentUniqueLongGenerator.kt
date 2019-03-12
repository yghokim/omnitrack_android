package kr.ac.snu.hcil.android.common

import java.util.concurrent.atomic.AtomicInteger

/**
 * Emits atomic unique long. guaranteed 1000 longs in one second window.
 * Created by younghokim on 2017. 10. 29..
 */
class ConcurrentUniqueLongGenerator {
    private val uniqueSeed = AtomicInteger()

    fun getNewUniqueLong(createdAt: Long = System.currentTimeMillis()): Long {
        val nanoStamp = createdAt * 1000 + uniqueSeed.getAndIncrement()
        uniqueSeed.compareAndSet(1000, 0)
        return nanoStamp
    }
}