package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView

/**
 * Created by younghokim on 16. 7. 12..
 */
abstract class OTPropertyHelper<T>() {

    abstract fun getSerializedValue(value: T): String

    abstract fun parseValue(serialized: String): T

    fun buildView(title: String, initialValue: T, context: Context): APropertyView<T> {
        val view = onBuildView(context)
        if (title != null)
            view.title = title
        view.value = initialValue
        return view
    }

    abstract protected fun onBuildView(context: Context): APropertyView<T>
}