package kr.ac.snu.hcil.android.common.events

/**
 * Created by younghokim on 16. 8. 31..
 */
interface IEventListener<T> {
    fun onEvent(sender: Any, args: T)
}
