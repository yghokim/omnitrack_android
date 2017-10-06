package kr.ac.snu.hcil.omnitrack.utils.events

import rx.Observable
import rx.subjects.BehaviorSubject

/**
 * Created by younghokim on 16. 7. 11..
 *
 * Codes and design patterns from https://nvbn.github.io/2016/04/28/kotlin-events/
 *
 */
open class Event<T> {
    private var handlers = hashSetOf<(sender: Any, args: T) -> Unit>()
    private var listeners = hashSetOf<IEventListener<T>>()

    private val _observable = BehaviorSubject.create<Pair<Any, T>>()

    val observable: Observable<Pair<Any, T>> get() = _observable

    var suspend: Boolean = false


    operator fun plusAssign(listener: IEventListener<T>) {
        if (!listeners.contains(listener))
            listeners.add(listener)
    }

    operator fun minusAssign(listener: IEventListener<T>) {
        listeners.remove(listener)
    }


    operator fun plusAssign(handler: (sender: Any, args: T) -> Unit) {
        handlers.add(handler)
    }

    operator fun minusAssign(handler: (sender: Any, args: T) -> Unit) {
        handlers.remove(handler)
    }


    fun invoke(sender: Any, args: T) {
        if (!suspend) {
            for (subscriber in handlers) {
                subscriber(sender, args)
            }

            for (subscriber in listeners) {
                subscriber.onEvent(sender, args)
            }

            _observable.onNext(Pair(sender, args))
        }
    }

    fun clear() {
        handlers.clear()
        listeners.clear()
    }
}