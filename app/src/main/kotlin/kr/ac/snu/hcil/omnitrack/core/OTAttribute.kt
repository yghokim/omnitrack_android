package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
open abstract class OTAttribute(value: Any) {

    data class ValueChangedEventArgs(val from: Any, val to:Any)

    val valueChanged = Event<ValueChangedEventArgs>()

    //var value: T
    var value: Any by Delegates.observable(value){
        prop, old, new->
            valueChanged.invoke(this, ValueChangedEventArgs(old, new))
    }

}