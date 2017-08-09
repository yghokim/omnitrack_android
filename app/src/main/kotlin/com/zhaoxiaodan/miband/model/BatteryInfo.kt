package com.zhaoxiaodan.miband.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * 手环电池相关信息类
 */
class BatteryInfo private constructor() {
    /**
     * 电池当前所在的状态
     */
    enum class Status {
        UNKNOWN, LOW, FULL, CHARGING, NOT_CHARGING;


        companion object {

            fun fromByte(b: Byte): Status {
                when (b) {
                    1.toByte() -> return LOW
                    2.toByte() -> return CHARGING
                    3.toByte() -> return FULL
                    4.toByte() -> return NOT_CHARGING

                    else -> return UNKNOWN
                }
            }
        }
    }

    /**
     * 电池电量百分比, level=40 表示有40%的电量
     */
    var level: Int = 0
        private set
    /**
     * 充电循环次数
     */
    var cycles: Int = 0
        private set
    /**
     * 当前状态

     * @see Status
     */
    var status: Status? = null
        private set
    /**
     * 最后充电时间
     */
    var lastChargedDate: Calendar? = null
        private set

    override fun toString(): String {
        return "cycles:${this.cycles},level:${this.level}, status:${this.status},last:${SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.CHINA).format(this.lastChargedDate?.time)}"
    }

    companion object {

        fun fromByteData(data: ByteArray): BatteryInfo? {
            if (data.size < 10) {
                return null
            }
            val info = BatteryInfo()

            info.level = data[0].toInt()
            info.status = Status.fromByte(data[9])
            info.cycles = 0xffff and (0xff and data[7].toInt() or (0xff and data[8].toInt() shl 8))
            info.lastChargedDate = Calendar.getInstance()

            info.lastChargedDate!!.set(Calendar.YEAR, data[1] + 2000)
            info.lastChargedDate!!.set(Calendar.MONTH, data[2].toInt())
            info.lastChargedDate!!.set(Calendar.DATE, data[3].toInt())

            info.lastChargedDate!!.set(Calendar.HOUR_OF_DAY, data[4].toInt())
            info.lastChargedDate!!.set(Calendar.MINUTE, data[5].toInt())
            info.lastChargedDate!!.set(Calendar.SECOND, data[6].toInt())

            return info
        }
    }

}
