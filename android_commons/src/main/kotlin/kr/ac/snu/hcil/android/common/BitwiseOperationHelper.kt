package kr.ac.snu.hcil.android.common

/**
 * Created by younghokim on 16. 8. 23..
 */
object BitwiseOperationHelper {


    fun setBooleanAt(integer: Int, value: Boolean, leftShift: Int): Int {
        if (value) {
            return integer or (0b1 shl leftShift)
        } else {
            return integer and (0b1 shl leftShift).inv()
        }
    }

    fun getBooleanAt(integer: Int, shift: Int): Boolean {
        return ((integer shr shift) and 1) == 1
    }

    fun getIntAt(integer: Int, shift: Int, mask: Int): Int {

        return ((integer shr shift) and mask)
    }

    fun setIntAt(integer: Int, value: Int, shift: Int, mask: Int): Int {
        val cleared = integer and (mask shl shift).inv()
        return cleared or (value shl shift)
    }

}