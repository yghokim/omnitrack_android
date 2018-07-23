package kr.ac.snu.hcil.omnitrack.ui.pages.export

import android.app.Application
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import javax.inject.Inject

class PackageExportViewModel(application: Application) : UserAttachedViewModel(application) {

    @field:[Inject Configured]
    protected lateinit var serverController: ISynchronizationServerSideAPI

    private var selectedTrackerIds = HashSet<String>()
    private var selectedTriggerIds = HashSet<String>()
    private var _selectedReminderIds = HashSet<String>()

    private val trackerInfoListSubject = BehaviorSubject.create<List<OTTrackerDAO.SimpleTrackerInfo>>()
    private val loggingTriggerInfoListSubject = BehaviorSubject.create<List<OTTriggerDAO.SimpleTriggerInfo>>()

    val trackerInfoListObservable: Observable<List<OTTrackerDAO.SimpleTrackerInfo>> get() = trackerInfoListSubject
    val loggingTriggerInfoListObservable: Observable<List<OTTriggerDAO.SimpleTriggerInfo>> get() = loggingTriggerInfoListSubject


    private val trackerSelectionListSubject = BehaviorSubject.createDefault<List<String>>(emptyList<String>())
    private val loggingTriggerSelectionListSubject = BehaviorSubject.createDefault<List<String>>(emptyList<String>())
    private val reminderSelectionListSubject = BehaviorSubject.createDefault<List<String>>(emptyList<String>())
    val trackerSelectionListObservable: Observable<List<String>> get() = trackerSelectionListSubject
    val loggingTriggerSelectionListObservable: Observable<List<String>> get() = loggingTriggerSelectionListSubject
    val reminderSelectionListObservable: Observable<List<String>> get() = reminderSelectionListSubject

    val selectedReminderIds: List<String>
        get() = reminderSelectionListSubject.value?.toList() ?: emptyList()

    override fun onInject(configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.inject(this)
    }

    override fun onUserAttached(newUserId: String) {
        super.onUserAttached(newUserId)
        this.selectedTrackerIds.clear()
        this.selectedTriggerIds.clear()

        trackerInfoListSubject.onNext(dbManager.get()
                .makeTrackersOfUserQuery(newUserId, realm)
                .findAll().map { it.getSimpleInfo(true) }.toList())

        loggingTriggerInfoListSubject.onNext(dbManager.get()
                .makeTriggersOfUserQuery(newUserId, realm)
                .equalTo("actionType", OTTriggerDAO.ACTION_TYPE_LOG)
                .findAll().map { it.getSimpleInfo(true) }.toList())
    }

    fun setTrackerIdChecked(checked: Boolean, id: String): Boolean {
        return setEntityIdChecked(checked, id, selectedTrackerIds, trackerSelectionListSubject)
    }

    fun setLoggingTriggerIdChecked(checked: Boolean, id: String): Boolean {
        return setEntityIdChecked(checked, id, selectedTriggerIds, loggingTriggerSelectionListSubject)
    }

    fun setReminderIdChecked(checked: Boolean, id: String): Boolean {
        return setEntityIdChecked(checked, id, _selectedReminderIds, reminderSelectionListSubject)
    }


    private fun setEntityIdChecked(checked: Boolean, id: String, set: HashSet<String>, subject: BehaviorSubject<List<String>>): Boolean {
        val changed = if (!checked) {
            set.remove(id)
        } else set.add(id)

        if (changed) {
            subject.onNext(set.toList())
        }

        return changed
    }

    fun extractPackage(): Single<String> {
        return serverController.getTrackingPackageJson(
                selectedTrackerIds.toTypedArray(),
                (selectedReminderIds + selectedTriggerIds).toTypedArray()
        )
    }

}