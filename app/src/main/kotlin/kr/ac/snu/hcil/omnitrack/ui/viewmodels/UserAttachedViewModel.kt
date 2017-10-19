package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.arch.lifecycle.ViewModel

/**
 * Created by Young-Ho on 6/4/2017.
 */
open class UserAttachedViewModel : ViewModel() {
    var userId: String? = null
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
    protected open fun onUserAttached(newUserId: String) {

    }

    protected open fun onDispose() {
    }

    override fun onCleared() {
        super.onCleared()
        onDispose()
        userId = null
    }
}