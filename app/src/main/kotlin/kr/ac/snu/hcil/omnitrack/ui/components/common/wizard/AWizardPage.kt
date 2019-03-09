package kr.ac.snu.hcil.omnitrack.ui.components.common.wizard

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.android.common.events.Event

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
abstract class AWizardPage(protected open val parent: WizardView) {
    val isCompleteButtonAvailableChanged = Event<Boolean>()

    val goNextRequested = Event<Int>()
    val goBackRequested = Event<Int>()

    var isCompleteButtonAvailable: Boolean = false
        protected set(value) {
            if (field != value) {
                field = value
                isCompleteButtonAvailableChanged.invoke(this, value)
            }
        }

    abstract val canGoBack: Boolean
    abstract val canGoNext: Boolean

    abstract val getTitleResourceId: Int
    abstract fun onLeave()
    abstract fun onEnter()

    fun getView(context: Context): View {
        val view = makeViewInstance(context)
        view.tag = this
        return view
    }

    protected abstract fun makeViewInstance(context: Context): View

    protected fun requestGoNextPage(nextPosition: Int) {
        goNextRequested.invoke(this, nextPosition)
    }
}