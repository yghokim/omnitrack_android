package kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardPage

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
class TimeQueryPage : AWizardPage() {

    override val getTitleResourceId: Int = R.string.msg_connection_wizard_title_time_query

    override val canGoBack: Boolean = false
    override val canGoNext: Boolean
        get() {
            return true
        }

    override fun onLeave() {

    }

    override fun onEnter() {
    }

    override fun makeViewInstance(context: Context): View {
        return View(context)
    }


    inner class View : LinearLayout {
        constructor(context: Context?) : super(context)
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    }
}