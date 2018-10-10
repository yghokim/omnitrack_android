package kr.ac.snu.hcil.omnitrack.core.attributes.properties

import android.content.Context
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView

/**
 * Created by younghokim on 16. 7. 12..
 */
abstract class OTPropertyHelper<T> {

    abstract fun getSerializedValue(value: T): String

    abstract fun parseValue(serialized: String): T

    abstract fun makeView(context: Context): APropertyView<T>
}