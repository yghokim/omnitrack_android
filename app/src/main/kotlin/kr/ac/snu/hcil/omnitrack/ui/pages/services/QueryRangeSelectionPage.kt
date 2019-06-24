package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.content.Context
import android.view.View
import kr.ac.snu.hcil.android.common.view.wizard.AWizardPage
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.ui.pages.field.wizard.pages.SourceConfigurationPanel

/**
 * Created by junhoe on 2017. 11. 6..
 */
class QueryRangeSelectionPage(override val parent: ServiceWizardView) : AWizardPage(parent) {

    override val canGoBack: Boolean = true
    override val canGoNext: Boolean = false
    override val getTitleResourceId: Int = R.string.msg_service_wizard_title_query_range_selection

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
        view?.initialize(parent.connection)
    }

    override fun makeViewInstance(context: Context): View {
        view = SourceConfigurationPanel(context)
        return view!!
    }

}