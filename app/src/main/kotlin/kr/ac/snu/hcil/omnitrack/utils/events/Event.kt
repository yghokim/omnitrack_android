package kr.ac.snu.hcil.omnitrack.utils.events

/**
 * Created by younghokim on 16. 7. 11..
 *
 * Codes and design patterns from https://nvbn.github.io/2016/04/28/kotlin-events/
 *
 */
open class Event<T> () {
    private var handlers = listOf<(sender: Any, args: T) -> Unit>()
    private var listeners = listOf<IEventListener<T>>()


    var suspend: Boolean = false


    operator fun plusAssign(listener: IEventListener<T>) {
        if (!listeners.contains(listener))
            listeners += listener
    }

    operator fun minusAssign(listener: IEventListener<T>) {
        listeners -= listener
    }


    operator fun plusAssign(handler: (sender: Any, args: T) -> Unit) {
        if (!handlers.contains(handler))
            handlers += handler
    }

    operator fun minusAssign(handler: (sender: Any, args: T) -> Unit) {
        handlers -= handler
    }


    fun invoke(sender: Any, args: T) {
        if(!suspend) {
            for (subscriber in handlers) {
                subscriber(sender, args)
            }

            for (subscriber in listeners) {
                subscriber.onEvent(sender, args)
            }

        }
    }
}