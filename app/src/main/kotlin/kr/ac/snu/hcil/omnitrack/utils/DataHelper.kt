package kr.ac.snu.hcil.omnitrack.utils

/**
 * Created by Young-Ho on 9/10/2016.
 */
object DataHelper {
    data class BinWithLong<T>(val x0: Long, val x1: Long, val values: List<T>)

    inline fun <T> ConvertSortedListToBinWithLong(cuts: Array<Long>, list: List<T>, getKey: (T) -> Long, from: Int = 0, to: Int = list.size - 1): Array<BinWithLong<T>> {
        if (list.isEmpty()) {
            return emptyArray<BinWithLong<T>>()
        }

        var currentItemPointer: Int = from
        var currentCutPointer: Int = 0
        var cut0 = cuts[currentCutPointer]
        var cut1 = cuts[currentCutPointer + 1]
        val result = ArrayList<BinWithLong<T>>()
        val currentBucket = ArrayList<T>()

        while (currentItemPointer <= (to - from)) {
            val key = getKey(list[currentItemPointer])
            if (key >= cut0) {
                if (key < cut1) {
                    currentBucket.add(list[currentItemPointer])
                    currentItemPointer++
                } else {
                    //add new bucket
                    if (currentBucket.isNotEmpty()) {
                        result.add(BinWithLong(cut0, cut1, currentBucket.toList()))
                        currentBucket.clear()
                    }

                    currentCutPointer++
                    cut0 = cuts[currentCutPointer]
                    cut1 = cuts[currentCutPointer + 1]
                }
            }
            continue
        }

        if (currentBucket.isNotEmpty()) {
            result.add(BinWithLong(cut0, cut1, currentBucket.toList()))
        }

        return result.toTypedArray()
    }
}