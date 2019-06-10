package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.data

import android.content.Context
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataDrivenTriggerManager
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerConditionViewModel
import javax.inject.Inject

class DataDrivenConditionViewModel(trigger: OTTriggerDAO, context: Context) : ATriggerConditionViewModel(trigger, OTTriggerDAO.CONDITION_TYPE_DATA) {

    private val subscriptions = CompositeDisposable()

    @Inject
    lateinit var dataDrivenTriggerManager: Lazy<OTDataDrivenTriggerManager>

    private val latestMeasuredInfoSubject = BehaviorSubject.createDefault(Nullable<Pair<Double?, Long>>(null))
    val latestMeasuredInfo: Observable<Nullable<Pair<Double?, Long>>> get() = latestMeasuredInfoSubject

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
        subscriptions.add(
                dataDrivenTriggerManager.get().makeLatestMeasuredValueObservable(trigger._id!!).subscribe { info ->
                    latestMeasuredInfoSubject.onNextIfDifferAndNotNull(info)
                }
        )
    }

    override fun refreshDaoToFront(snapshot: OTTriggerDAO) {
        println("datadriventrigger refresh triggerDAO. ${(snapshot.condition as OTDataDrivenTriggerCondition).measure?.factoryCode}")
    }

    override fun onSwitchChanged(isOn: Boolean) {

    }

    override fun afterTriggerFired(triggerTime: Long) {

    }

    override fun onDispose() {
        subscriptions.clear()
    }

    fun getServiceStateObservable(): Observable<OTExternalService.ServiceState> {
        return getCondition<OTDataDrivenTriggerCondition>()?.measure?.getFactory<OTServiceMeasureFactory>()?.getService<OTExternalService>()?.onStateChanged
                ?: Observable.just(OTExternalService.ServiceState.DEACTIVATED)
    }

}