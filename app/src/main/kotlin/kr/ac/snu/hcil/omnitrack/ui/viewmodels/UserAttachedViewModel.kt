package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.app.Application
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.android.common.containers.Nullable

/**
 * Created by Young-Ho on 6/4/2017.
 */
open class UserAttachedViewModel(application: Application) : RealmViewModel(application) {

    private val _userIdSubject = BehaviorSubject.createDefault<Nullable<String>>(Nullable(null))
    var userId: String?
        get() = _userIdSubject.value?.datum
        set(value) {
            if (_userIdSubject.value?.datum != value) {
                _userIdSubject.onNext(Nullable(value))
                if (value != null) {
                    onUserAttached(value)
                } else {
                    onUserDisposed()
                }
            }
        }

    val userIdObservable: Observable<Nullable<String>> get() = _userIdSubject

    protected open fun onUserAttached(newUserId: String) {

    }

    protected open fun onUserDisposed() {
    }

    override fun onCleared() {
        super.onCleared()
        onUserDisposed()
        userId = null
    }
}