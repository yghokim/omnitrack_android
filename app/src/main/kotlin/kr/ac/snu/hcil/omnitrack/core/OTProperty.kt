package kr.ac.snu.hcil.omnitrack.core

import android.content.Context
import kr.ac.snu.hcil.omnitrack.ui.components.properties.APropertyView

/**
 * Created by younghokim on 16. 7. 12..
 */
abstract class OTProperty<T>(val key: Int, val title: String) {

    abstract fun buildView(context: Context): APropertyView<T>

}