package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.ui.components.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 7. 12..
 */
abstract class OTProperty<T>(initialValue: T, val key: Int, val title: String) {

    data class PropertyChangedEventArgs<T>(val key: Int, val old: T, val new: T)

    val onValueChanged = Event<PropertyChangedEventArgs<T>>()

    var value: T by Delegates.observable(initialValue)
    {
        prop, old, new ->
        if (old != new) {
            onValueChanged.invoke(this, PropertyChangedEventArgs(key, old, new))
        }
    }

    abstract fun getSerializedValue(): String
    abstract fun parseValue(serialized: String): T

    fun setSerializedValue(serializedValue: String) {
        value = parseValue(serializedValue)
    }

    abstract fun buildView(context: Context): APropertyView<T>
}