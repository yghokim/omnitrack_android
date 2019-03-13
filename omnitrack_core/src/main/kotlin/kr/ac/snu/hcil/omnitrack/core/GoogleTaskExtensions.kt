package kr.ac.snu.hcil.omnitrack.core

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.Single
import io.reactivex.SingleEmitter
import java.util.concurrent.CancellationException

//Inspired and forked by https://github.com/ashdavies/rx-tasks

internal fun <T : Any> Task<T>.asRequired(): T = asResult()
        ?: throw IllegalStateException("Task $this returned empty result")

internal fun <T : Any> Task<T>.asResult(): T? = if (isComplete) {
    if (!isCanceled) exception
            ?.let { throw it }
            ?: result
    else throw CancellationException("Task $this was cancelled normally")
} else throw IllegalStateException("Task $this not complete")

internal class CompletableEmitterListener(private val emitter: CompletableEmitter) : OnCompleteListener<Void> {
    override fun onComplete(task: Task<Void>) = try {
        task.asResult()
        emitter.onComplete()
    } catch (exception: Exception) {
        emitter.onError(exception)
    }
}

internal class SingleEmitterListener<T : Any>(private val emitter: SingleEmitter<T>) : OnCompleteListener<T> {
    override fun onComplete(task: Task<T>) = try {
        emitter.onSuccess(task.asRequired())
    } catch (exception: Exception) {
        emitter.onError(exception)
    }
}


fun Task<Void>.toCompletable(): Completable {
    return if (this.isComplete)
        Completable.fromCallable { this.asResult() } else Completable.create { emitter ->
        this.addOnCompleteListener(CompletableEmitterListener(emitter))
    }
}

fun <T : Any> Task<T>.toSingle(): Single<T> {
    return if (this.isComplete) Single.fromCallable { this.asRequired() } else Single.create {
        this.addOnCompleteListener(SingleEmitterListener(it))
    }
}