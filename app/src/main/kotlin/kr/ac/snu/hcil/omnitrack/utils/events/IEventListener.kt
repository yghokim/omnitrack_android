package kr.ac.snu.hcil.omnitrack.utils.events

/**
 * Created by younghokim on 16. 8. 31..
 */
interface IEventListener<T> {
    fun onEvent(sender: Any, args: T)
}
