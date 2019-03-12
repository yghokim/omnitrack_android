package kr.ac.snu.hcil.android.common

import io.reactivex.subjects.BehaviorSubject

fun <T> BehaviorSubject<T>.onNextIfDifferAndNotNull(i: T?) {
    if (i != null) {
        if (this.hasValue()) {
            if (this.value != i) {
                this.onNext(i)
            }
        } else this.onNext(i)
    }
}