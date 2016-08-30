package kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard

import android.content.Context
import android.util.AttributeSet
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

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
    }

    fun init(attribute: OTAttribute<out Any>) {
        setAdapter(Adapter(attribute))
    }

    override fun onEnterPage(page: AWizardPage, position: Int) {
    }

    override fun onLeavePage(page: AWizardPage, position: Int) {
    }


    class Adapter(attribute: OTAttribute<out Any>) : AWizardViewPagerAdapter() {
        val pages = Array<AWizardPage>(2) {
            index ->
            when (index) {
                PAGE_INDEX_SOURCE_SELECTION -> SourceSelectionPage(attribute)
                PAGE_INDEX_TIME_QUERY -> TimeQueryPage()
                else -> throw Exception("wrong index")
            }
        }

        override fun getCount(): Int {
            return pages.size
        }

        override fun getPageAt(position: Int): AWizardPage {
            return pages[position]
        }

        override fun canComplete(): Boolean {
            return false
        }
    }

}