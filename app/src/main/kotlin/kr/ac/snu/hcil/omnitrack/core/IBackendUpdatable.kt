package kr.ac.snu.hcil.omnitrack.core

import rx.Observable

/**
 * Created by younghokim on 2017. 6. 23..
 */
interface IBackendUpdatable {
    val isOnline: Boolean
    fun goOnline()
    fun offline()
    val onlineStatusChanged: Observable<Boolean>
}