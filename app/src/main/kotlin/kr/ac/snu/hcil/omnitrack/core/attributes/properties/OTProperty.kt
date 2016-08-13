package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.serialization.ISerializableValueContainer
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 7. 12..
 */
abstract class OTProperty<T>(initialValue: T, val key: Int, val title: String) : ISerializableValueContainer {

    data class PropertyChangedEventArgs<T>(val key: Int, val old: T, val new: T)

    val onValueChanged = Event<PropertyChangedEventArgs<T>>()

    var value: T by Delegates.observable(initialValue)
    {
        prop, old, new ->
        if (old != new) {
            onValueChanged.invoke(this, PropertyChangedEventArgs(key, old, new))
        }
    }

    abstract fun parseValue(serialized: String): T

    override fun setValueFromSerializedString(serialized: String) {
        value = parseValue(serialized)
    }

    fun buildView(context: Context): APropertyView<T> {
        val view = onBuildView(context)
        view.title = title
        view.value = value
        return view
    }

    abstract protected fun onBuildView(context: Context): APropertyView<T>
}