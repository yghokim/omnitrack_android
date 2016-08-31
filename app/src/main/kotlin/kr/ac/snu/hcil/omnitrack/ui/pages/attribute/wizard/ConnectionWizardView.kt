package kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard

import android.content.Context
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.core.OTConnection
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardPage
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardViewPagerAdapter
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.WizardView
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages.SourceSelectionPage
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages.TimeQueryPage

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
class ConnectionWizardView : WizardView {

    companion object {
        const val PAGE_INDEX_SOURCE_SELECTION = 0
        const val PAGE_INDEX_TIME_QUERY = 1
        const val PAGE_INDEX_POST_PROCESSING = 2
    }

    private lateinit var pendingConnection: OTConnection

    val connection: OTConnection
        get() = pendingConnection


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
    }

    fun init(attribute: OTAttribute<out Any>) {
        init(attribute, OTConnection())
    }

    /**
     * used to modify existing connection
     */
    fun init(attribute: OTAttribute<out Any>, connection: OTConnection) {
        setAdapter(Adapter(attribute))
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
        }
    }


    class Adapter(attribute: OTAttribute<out Any>) : AWizardViewPagerAdapter() {
        val pages = Array<AWizardPage>(2) {
            index ->
            when (index) {
                PAGE_INDEX_SOURCE_SELECTION -> SourceSelectionPage(attribute)
                PAGE_INDEX_TIME_QUERY -> TimeQueryPage(attribute)
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