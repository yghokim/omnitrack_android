package kr.ac.snu.hcil.omnitrack.utils

import kr.ac.snu.hcil.omnitrack.utils.events.Event

/**
 * Created by younghokim on 16. 7. 11..
 */
interface IListEventListener <T> {
    val elementAddedEvent: Event<Pair<T, Int>>
    val elementRemovedEvent: Event<Pair<T, Int>>
}