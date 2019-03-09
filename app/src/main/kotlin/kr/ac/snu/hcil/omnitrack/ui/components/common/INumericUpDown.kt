package kr.ac.snu.hcil.omnitrack.ui.components.common

import kr.ac.snu.hcil.android.common.events.Event

interface INumericUpDown {

    enum class ChangeType { INC, DEC, MANUAL }
    data class ChangeArgs(val newValue: Int, val changeType: ChangeType, val delta: Int)

    companion object {

        const val MODE_PLUS_MINUS = 0
        const val MODE_UP_DOWN = 1

        const val FAST_CHANGE_INTERVAL = 100L
    }

    var minValue: Int
    var maxValue: Int
    val value: Int
    var displayedValues: Array<String>?
    var formatter: ((Int) -> String)?
    var quantityResId: Int?
    var zeroPad: Int
    val valueChanged: Event<ChangeArgs>
    var allowLongPress: Boolean
    fun setValue(newValue: Int, changeType: ChangeType = ChangeType.MANUAL, delta: Int = 0)
}