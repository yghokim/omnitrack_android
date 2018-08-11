package kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardPage
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardViewPagerAdapter
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.WizardView
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages.SourceConfigurationPage
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages.SourceSelectionPage
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages.TimeQueryPage

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
class ConnectionWizardView : WizardView {

    companion object {
        const val PAGE_INDEX_SOURCE_SELECTION = 0
        const val PAGE_INDEX_TIME_QUERY = 1
        const val PAGE_INDEX_CONFIGURATION = 2

        //const val PAGE_INDEX_POST_PROCESSING = 2
    }

    private lateinit var pendingConnection: OTConnection

    val connection: OTConnection
        get() = pendingConnection


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
    }

    fun init(attribute: OTAttributeDAO) {
        init(attribute, OTConnection())
    }

    /**
     * used to modify existing connection
     */
    fun init(attribute: OTAttributeDAO, connection: OTConnection) {
        setAdapter(Adapter(attribute, context))
        pendingConnection = connection
    }


    override fun onEnterPage(page: AWizardPage, position: Int) {
        super.onEnterPage(page, position)
        when (position) {
            PAGE_INDEX_SOURCE_SELECTION ->
                return
            PAGE_INDEX_TIME_QUERY ->
                if (!pendingConnection.isRangedQueryAvailable) {
                    throw Exception("This source do not support Time Query. Wrong wizard page.")
                }
        }
    }

    override fun onLeavePage(page: AWizardPage, position: Int) {
        when (position) {
            PAGE_INDEX_SOURCE_SELECTION ->
                pendingConnection.source = (page as SourceSelectionPage).selectedInformation?.getSource()
            PAGE_INDEX_TIME_QUERY -> {
                if (!pendingConnection.isRangedQueryAvailable) {
                    throw Exception("This source do not support Time Query. Wrong wizard page.")
                } else {
                    val tq = (page as TimeQueryPage).timeQuery
                    println(tq)
                    pendingConnection.rangedQuery = tq
                }
            }
            PAGE_INDEX_CONFIGURATION -> {
                println("apply configuration")
                (page as SourceConfigurationPage).applyConfiguration(pendingConnection)
            }
        }
    }


    inner class Adapter(attribute: OTAttributeDAO, context: Context) : AWizardViewPagerAdapter(context) {
        val pages = Array<AWizardPage>(3) {
            index ->
            when (index) {
                PAGE_INDEX_SOURCE_SELECTION -> SourceSelectionPage(this@ConnectionWizardView, attribute)
                PAGE_INDEX_TIME_QUERY -> TimeQueryPage(this@ConnectionWizardView)
                PAGE_INDEX_CONFIGURATION -> SourceConfigurationPage(this@ConnectionWizardView)
                else -> throw Exception("wrong index")
            }
        }

        override fun getCount(): Int {
            return pages.size
        }

        override fun getPageAt(position: Int): AWizardPage {
            return pages[position]
        }
    }

}