package com.zhaoxiaodan.miband

interface ActionCallback {
    fun onSuccess(data: Any?)

    fun onFail(errorCode: Int, msg: String)
}
