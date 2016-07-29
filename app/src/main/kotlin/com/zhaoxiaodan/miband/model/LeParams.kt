package com.zhaoxiaodan.miband.model

class LeParams {
    var connIntMin: Int = 0
    var connIntMax: Int = 0
    var connInt: Int = 0

    var latency: Int = 0
    var timeout: Int = 0
    var advInt: Int = 0

    companion object {

        fun fromByte(b: ByteArray): LeParams {
            val params = LeParams()

            params.connIntMin = 0xffff and (0xff and b[0].toInt() or (0xff and b[1].toInt() shl 8))
            params.connIntMax = 0xffff and (0xff and b[2].toInt() or (0xff and b[3].toInt() shl 8))
            params.latency = 0xffff and (0xff and b[4].toInt() or (0xff and b[5].toInt() shl 8))
            params.timeout = 0xffff and (0xff and b[6].toInt() or (0xff and b[7].toInt() shl 8))
            params.connInt = 0xffff and (0xff and b[8].toInt() or (0xff and b[9].toInt() shl 8))
            params.advInt = 0xffff and (0xff and b[10].toInt() or (0xff and b[11].toInt() shl 8))

            params.connIntMin = (params.connIntMin * 1.25).toInt()
            params.connIntMax = (params.connIntMax * 1.25).toInt()
            params.advInt = (params.advInt * 0.625).toInt()
            params.timeout *= 10

            return params
        }
    }
}
