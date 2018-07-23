package kr.ac.snu.hcil.omnitrack.utils

import kr.ac.snu.hcil.omnitrack.utils.events.IEventListener

class SelectableInfo<T>(val info: T, selected: Boolean) {
    var onSelectionChangedHandler: IEventListener<Boolean>? = null

    var selected: Boolean = selected
        set(value) {
            if (field != value) {
                field = value
                onSelectionChangedHandler?.onEvent(this, value)
            }
        }
}