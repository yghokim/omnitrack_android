package kr.ac.snu.hcil.omnitrack.ui.components.common.wizard

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.omnitrack.utils.events.Event

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
abstract class AWizardPage {
    val goNextAvailableChanged = Event<Pair<Long, Boolean>>()
    val goBackAvailableChanged = Event<Pair<Long, Boolean>>()
    val goNextRequested = Event<Int>()
    val goBackRequested = Event<Int>()

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

    abstract protected fun makeViewInstance(context: Context): View

    protected fun requestGoNextPage(nextPosition: Int) {
        goNextRequested.invoke(this, nextPosition)
    }
}