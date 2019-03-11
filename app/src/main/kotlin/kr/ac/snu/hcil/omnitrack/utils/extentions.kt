package kr.ac.snu.hcil.omnitrack.utils

import io.reactivex.Completable
import io.reactivex.disposables.Disposables
import io.realm.Realm


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