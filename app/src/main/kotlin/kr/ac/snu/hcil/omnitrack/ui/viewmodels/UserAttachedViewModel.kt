package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.arch.lifecycle.ViewModel
import kr.ac.snu.hcil.omnitrack.core.OTUser
import rx.subscriptions.CompositeSubscription

/**
 * Created by Young-Ho on 6/4/2017.
 */
open class UserAttachedViewModel : ViewModel() {
    var user: OTUser? = null
        set(value) {
            if (field != value) {
                field = value
                if (value != null) {
                    onUserAttached(value)
                } else {
                    onDispose()
                }
            }
        }
    protected val internalSubscriptions = CompositeSubscription()

    protected open fun onUserAttached(newUser: OTUser) {

    }

    protected open fun onDispose() {
        internalSubscriptions.clear()
    }

    override fun onCleared() {
        super.onCleared()
        onDispose()
        user = null
    }
}