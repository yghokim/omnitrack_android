package kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTConnection
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardPage

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
class SourceConfigurationPage(val attribute: OTAttribute<out Any>) : AWizardPage() {

    override val getTitleResourceId: Int = R.string.msg_connection_wizard_title_time_query

    override val canGoBack: Boolean = false
    override val canGoNext: Boolean
        get() {
            return true
        }

    private var view: SourceConfigurationPanel? = null

    init {
        isCompleteButtonAvailable = true
    }

    fun applyConfiguration(connection: OTConnection) {
        view?.applyConfiguration(connection)
    }

    override fun onLeave() {
    }

    override fun onEnter() {
    }

    override fun makeViewInstance(context: Context): View {
        view = SourceConfigurationPanel(context)

        return view!!
    }
}