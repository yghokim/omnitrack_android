package kr.ac.snu.hcil.omnitrack.utils.events

/**
 * Created by younghokim on 16. 7. 11..
 *
 * Codes and design patterns from https://nvbn.github.io/2016/04/28/kotlin-events/
 *
 */
open class Event<T> () {
    private var handlers = listOf<(sender: Any, args: T) -> Unit>()

    operator fun plusAssign(handler: (sender: Any, args: T) -> Unit) {
        handlers += handler
    }

    operator fun minusAssign(handler: (sender: Any, args: T) -> Unit) {
        handlers -= handler
    }

    fun invoke(sender: Any, args: T) {
        for (subscriber in handlers) {
            subscriber(sender, args)
        }
    }
}