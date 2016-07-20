package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
open abstract class OTAttribute() {


    data class PropertyChangedEventArgs(val property:Int, val old : Any?, val new : Any?)
    /*
    data class ValueChangedEventArgs(val from: Any, val to:Any)

    val valueChanged = Event<ValueChangedEventArgs>()

    //var value: T
    var value: Any by Delegates.observable(value){
        prop, old, new->
            onValueChanged(old, new)
    }

    protected fun onValueChanged(old: Any, new: Any)
    {
        valueChanged.invoke(this, ValueChangedEventArgs(old, new))
    }
*/
    abstract val layoutId: Int

    abstract val settingsProperties: Array<OTProperty<Any>>

    val propertyChanged = Event<PropertyChangedEventArgs>()

    protected fun notifyPropertyChanged(property: Int, old: Any?, new: Any?)
    {
        propertyChanged.invoke(this, PropertyChangedEventArgs(property, old, new))
    }

    fun commitPropertyChanges(){

    }

}