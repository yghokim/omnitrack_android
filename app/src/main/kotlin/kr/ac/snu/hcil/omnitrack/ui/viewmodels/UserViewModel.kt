package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.arch.lifecycle.AndroidViewModel
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTUser
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription

/**
 * Created by younghokim on 2017-05-30.
 */
class UserViewModel(application: OTApplication) : AndroidViewModel(application) {
    private val loadUserSubscriptions = CompositeSubscription()
    private val signedInUser: BehaviorSubject<OTUser> = BehaviorSubject.create<OTUser>()

    val signedInUserObservable: Observable<OTUser> get() = signedInUser

    fun refresh() {
        if (loadUserSubscriptions.hasSubscriptions()) {
            loadUserSubscriptions.add(
                    OTApplication.app.currentUserObservable.subscribe { user ->
                        signedInUser.onNext(user)
                    }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadUserSubscriptions.clear()
    }
}