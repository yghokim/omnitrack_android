package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardPage
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardViewPagerAdapter
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.WizardView

/**
 * Created by junhoe on 2017. 10. 30..
 */
class ServiceWizardView: WizardView {

    companion object {
        const val PAGE_TRACKER_SELECTION = 0
        const val PAGE_FIELD_SELECTION = 1
        const val PAGE_QUERY_RANGE_SELECTION = 2
    }

    val connection = OTConnection()

    var trackerId: String? = null
    lateinit var attributeDAO: OTAttributeDAO
    val currentMeasureFactory: OTMeasureFactory

    constructor(context: Context?, measureFactory: OTMeasureFactory) : super(context) {
        currentMeasureFactory = measureFactory
        connection.source = measureFactory.makeMeasure()
        Log.i("Omnitrack", measureFactory.javaClass.name)
        setAdapter(Adapter())
    }

    constructor(context: Context?, measureFactory: OTMeasureFactory, attrs: AttributeSet?) : super(context, attrs) {
        currentMeasureFactory = measureFactory
        connection.source = measureFactory.makeMeasure()
        setAdapter(Adapter())
    }

    override fun onLeavePage(page: AWizardPage, position: Int) {
        when (position) {
            PAGE_TRACKER_SELECTION ->
                    trackerId = (page as TrackerSelectionPage).selectedTrackerId
            PAGE_FIELD_SELECTION ->
                    attributeDAO = (page as FieldSelectionPage).attributeDAO
            PAGE_QUERY_RANGE_SELECTION -> {
                (page as QueryRangeSelectionPage).applyConfiguration(connection)
                attributeDAO.serializedConnection = connection.getSerializedString()
            }
        }
    }

    inner class Adapter : AWizardViewPagerAdapter() {

        val pages = Array(3) {
            index ->
            when (index) {
                PAGE_TRACKER_SELECTION -> TrackerSelectionPage(this@ServiceWizardView)
                PAGE_FIELD_SELECTION -> FieldSelectionPage(this@ServiceWizardView)
                PAGE_QUERY_RANGE_SELECTION -> QueryRangeSelectionPage(this@ServiceWizardView)
                else -> throw Exception("wrong index")
            }
        }

        override fun getPageAt(position: Int): AWizardPage = pages[position]
        override fun getCount(): Int = pages.size
    }
}