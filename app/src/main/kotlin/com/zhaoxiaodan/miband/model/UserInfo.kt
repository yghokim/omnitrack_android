package com.zhaoxiaodan.miband.model

import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer

class UserInfo {

    /**
     * @return the uid
     */
    var uid: Int = 0
        private set
    /**
     * @return the gender
     */
    var gender: Int = 0
        private set
    /**
     * @return the age
     */
    var age: Int = 0
        private set
    private var height: Int = 0        // cm
    private var weight: Int = 0        // kg
    /**
     * @return the alias
     */
    var alias = ""
        private set
    /**
     * @return the type
     */
    var type: Int = 0
        private set

    private constructor() {

    }

    constructor(uid: Int, gender: Int, age: Int, height: Int, weight: Int, alias: String, type: Int) {
        this.uid = uid
        this.gender = gender
        this.age = age
        this.height = (height and 0xFF)
        this.weight = weight
        this.alias = alias
        this.type = type
    }

    fun getBytes(mBTAddress: String): ByteArray {
        val aliasBytes: ByteArray
        try {
            aliasBytes = this.alias.toByteArray(charset("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            aliasBytes = ByteArray(0)
        }

        val bf = ByteBuffer.allocate(20)
        bf.put((uid and 0xff).toByte())
        bf.put((uid shr 8 and 0xff).toByte())
        bf.put((uid shr 16 and 0xff).toByte())
        bf.put((uid shr 24 and 0xff).toByte())
        bf.put(this.gender.toByte())
        bf.put(this.age.toByte())
        bf.put(this.height.toByte())
        bf.put(this.weight.toByte())
        bf.put(this.type.toByte())
        bf.put(4.toByte())
        bf.put(0.toByte())

        if (aliasBytes.size <= 8) {
            bf.put(aliasBytes)
            bf.put(ByteArray(8 - aliasBytes.size))
        } else {
            bf.put(aliasBytes, 0, 8)
        }

        val crcSequence = ByteArray(19)
        for (u in crcSequence.indices)
            crcSequence[u] = bf.array()[u]

        val crcb = (getCRC8(crcSequence) xor Integer.parseInt(mBTAddress.substring(mBTAddress.length - 2), 16) and 0xff).toByte()
        bf.put(crcb)
        return bf.array()
    }

    private fun getCRC8(seq: ByteArray): Int {
        var len = seq.size
        var i = 0
        var crc: Int = 0x00

        while (len-- > 0) {
            var extract = seq[i++].toInt()
            for (tempI in 8 downTo 1) {
                var sum = (crc and 0xff xor (extract and 0xff))
                sum = (sum and 0xff and 0x01)
                crc = (crc and 0xff).ushr(1)
                if (sum.toInt() != 0) {
                    crc = (crc and 0xff xor 0x8c)
                }
                extract = (extract and 0xff).ushr(1)
            }
        }
        return crc and 0xff
    }

    override fun toString(): String {
        return "uid: ${this.uid},gender: ${this.gender},age: ${this.age}, height: ${this.getHeight()}, weight: ${this.getWeight()}, alias: ${this.alias}, type: ${this.type}"
    }

    /**
     * @return the height
     */
    fun getHeight(): Int {
        return height and 0xFF
    }

    /**
     * @return the weight
     */
    fun getWeight(): Int {
        return weight and 0xFF
    }

    companion object {

        fun fromByteData(byteData: ByteArray): UserInfo? {
            if (byteData.size < 20) {
                return null
            }
            val info = UserInfo()

            val data = byteData.map { it.toInt() }.toIntArray()

            info.uid = data[3] shl 24 or (data[2] and 0xFF) shl 16 or (data[1] and 0xFF) shl 8 or (data[0] and 0xFF)
            info.gender = data[4]
            info.age = data[5]
            info.height = data[6]
            info.weight = data[7]
            info.type = data[8]
            try {
                info.alias = /*String(byteData, 9, 8, "UTF-8")*/ byteData.toString()
            } catch (e: UnsupportedEncodingException) {
                info.alias = ""
            }

            return info
        }
    }
}
