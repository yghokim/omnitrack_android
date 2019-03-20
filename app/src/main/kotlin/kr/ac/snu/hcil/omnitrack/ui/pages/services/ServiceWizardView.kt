package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.content.Context
import android.util.AttributeSet
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.android.common.view.wizard.AWizardPage
import kr.ac.snu.hcil.android.common.view.wizard.AWizardViewPagerAdapter
import kr.ac.snu.hcil.android.common.view.wizard.WizardView
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import javax.inject.Inject

/**
 * Created by junhoe on 2017. 10. 30..
 */
class ServiceWizardView: WizardView {

    companion object {
        const val PAGE_TRACKER_SELECTION = 0
        const val PAGE_ATTRIBUTE_SELECTION = 1
        const val PAGE_QUERY_RANGE_SELECTION = 2
    }

    @field:[Inject Backend]
    lateinit var realmProvider: Factory<Realm>

    @Inject
    lateinit var synchManager: Lazy<OTSyncManager>

    val connection = OTConnection()
    val currentMeasureFactory: OTMeasureFactory
    lateinit var trackerDao: OTTrackerDAO

    private var attributeDAO: OTAttributeDAO? = null

    private val adapter = Adapter(context)

    constructor(context: Context, measureFactory: OTMeasureFactory) : super(context) {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
        currentMeasureFactory = measureFactory
        connection.source = measureFactory.makeMeasure()
        setAdapter(adapter)
    }

    constructor(context: Context, measureFactory: OTMeasureFactory, attrs: AttributeSet?) : super(context, attrs) {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
        currentMeasureFactory = measureFactory
        connection.source = measureFactory.makeMeasure()
        setAdapter(Adapter(context))
    }

    override fun onLeavePage(page: AWizardPage, position: Int) {
        when (position) {
            PAGE_TRACKER_SELECTION ->
                    trackerDao = (page as TrackerSelectionPage).selectedTrackerDAO
            PAGE_ATTRIBUTE_SELECTION ->
                    attributeDAO = (page as AttributeSelectionPage).attributeDAO
            PAGE_QUERY_RANGE_SELECTION -> {

            }
        }
    }

    override fun onComplete() {
        super.onComplete()

        (adapter.getPageAt(PAGE_QUERY_RANGE_SELECTION) as QueryRangeSelectionPage).applyConfiguration(connection)
        val realm = realmProvider.get()
        realm.executeTransactionIfNotIn {

            attributeDAO?.serializedConnection = connection.getSerializedString(context)
            if (attributeDAO?.isManaged == false) {
                trackerDao.attributes.add(attributeDAO)
            }

            trackerDao.synchronizedAt = null
            if (!trackerDao.isManaged) {
                realm.insert(trackerDao)
            }
        }
        realm.close()
        synchManager.get().registerSyncQueue(ESyncDataType.TRACKER, SyncDirection.UPLOAD)
    }

    inner class Adapter(context: Context) : AWizardViewPagerAdapter(context) {

        val pages = Array(3) {
            index ->
            when (index) {
                PAGE_TRACKER_SELECTION -> TrackerSelectionPage(this@ServiceWizardView)
                PAGE_ATTRIBUTE_SELECTION -> AttributeSelectionPage(this@ServiceWizardView)
                PAGE_QUERY_RANGE_SELECTION -> QueryRangeSelectionPage(this@ServiceWizardView)
                else -> throw Exception("wrong index")
            }
        }

        override fun getPageAt(position: Int): AWizardPage = pages[position]
        override fun getCount(): Int = pages.size
    }
}