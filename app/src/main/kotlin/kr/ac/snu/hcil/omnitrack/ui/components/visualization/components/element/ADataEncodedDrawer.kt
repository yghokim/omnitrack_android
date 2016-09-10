package kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.element

import kr.ac.snu.hcil.omnitrack.ui.components.visualization.IDrawer

/**
 * Created by younghokim on 16. 9. 8..
 */
abstract class ADataEncodedDrawer<T> : IDrawer {
    var datum: T? = null
}