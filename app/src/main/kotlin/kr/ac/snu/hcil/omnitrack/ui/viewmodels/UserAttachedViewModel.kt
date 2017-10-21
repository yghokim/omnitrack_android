package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.utils.Nullable

/**
 * Created by Young-Ho on 6/4/2017.
 */
open class UserAttachedViewModel : ViewModel() {
    private val _userIdSubject = BehaviorSubject.createDefault<Nullable<String>>(Nullable<String>(null))
    var userId: String?
        get() = _userIdSubject.value.datum
        set(value) {
            if (_userIdSubject.value.datum != value) {
                _userIdSubject.onNext(Nullable(value))
                if (value != null) {
                    onUserAttached(value)
                } else {
                    onDispose()
                }
            }
        }

    val userIdObservable: Observable<Nullable<String>> get() = _userIdSubject

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