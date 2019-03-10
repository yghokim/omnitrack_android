package kr.ac.snu.hcil.omnitrack.utils

import io.reactivex.Completable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import java.util.*


fun Long.toDatetimeString(): String {
    return TimeHelper.FORMAT_ISO_8601.format(Date(this))
}

fun Long.toDateString(): String {
    return TimeHelper.FORMAT_YYYY_MM_DD.format(Date(this))
}

fun <T> BehaviorSubject<T>.onNextIfDifferAndNotNull(i: T?) {
    if (i != null) {
        if (this.hasValue()) {
            if (this.value != i) {
                this.onNext(i)
            }
        } else this.onNext(i)
    }
}

fun Realm.executeTransactionIfNotIn(transaction: (Realm) -> Unit) {
    if (this.isInTransaction) {
        transaction.invoke(this)
    } else {
        this.executeTransaction(transaction)
    }
}

fun Realm.executeTransactionAsObservable(transaction: (Realm) -> Unit): Completable {
    return Completable.create { disposable ->
        val task =
                this.executeTransactionAsync(transaction, {
                    if (!disposable.isDisposed) {
                        disposable.onComplete()
                    }
                }, { err ->
                    if (!disposable.isDisposed) {
                        disposable.onError(err)
                    }
                })

        disposable.setDisposable(Disposables.fromAction { if (!task.isCancelled) task.cancel() })
    }
}