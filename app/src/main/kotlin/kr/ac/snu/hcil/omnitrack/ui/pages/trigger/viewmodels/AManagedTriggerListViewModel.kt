package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import android.app.Application
import io.realm.RealmQuery
import io.realm.RealmResults
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 10. 20..
 *
 * This class is a ViewModel of Android Architecture Component to be used for displaying and editing the triggers of a specific user.
 *
 * @author Young-Ho Kim
 *
 *
 */
abstract class AManagedTriggerListViewModel(app: Application) : ATriggerListViewModel(app) {

    @Inject
    protected lateinit var syncManager: OTSyncManager

    private var currentTriggerRealmResults: RealmResults<OTTriggerDAO>? = null

    /**
     * @param originalQuery Receives the original query in vanilla viewmodel. modify this
     * @return the modified query for Triggers. The vanilla implementation just returns the originalQuery.
     */
    protected open fun hookTriggerQuery(originalQuery: RealmQuery<OTTriggerDAO>): RealmQuery<OTTriggerDAO> {
        return originalQuery
    }

    override fun onInject(configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.inject(this)
    }


    protected fun init() {
        onDispose()

        currentTriggerRealmResults = hookTriggerQuery(realm.where(OTTriggerDAO::class.java).equalTo(BackendDbManager.FIELD_REMOVED_BOOLEAN, false))
                .findAllAsync()

        currentTriggerRealmResults?.addChangeListener { snapshot, changeSet ->
            if (changeSet == null) {
                //initial set
                currentTriggerViewModels.clear()
                currentTriggerViewModels.addAll(
                        snapshot.map { TriggerViewModel(configuredContext, it, realm) }
                )
            } else {
                //changes
                //deal with deletions
                val removes = changeSet.deletions.map { i -> currentTriggerViewModels[i] }
                removes.forEach { it.unregister() }
                currentTriggerViewModels.removeAll(removes)

                //deal with additions
                changeSet.insertions.forEach { i ->
                    currentTriggerViewModels.add(i, TriggerViewModel(configuredContext, snapshot[i]!!, realm))
                }

                //deal with refresh
                changeSet.changes.forEach { i ->
                }
            }

            notifyNewTriggerViewModels()
        }
    }

    override fun addNewTriggerImpl(dao: OTTriggerDAO) {

        beforeAddNewTrigger(dao)

        dbManager.get().saveTrigger(dao, realm)
        syncManager.registerSyncQueue(ESyncDataType.TRIGGER, SyncDirection.UPLOAD, ignoreDirtyFlags = false)

    }

    override fun removeTrigger(objectId: String) {
        val viewModel = currentTriggerViewModels.find { it.objectId == objectId }
        if (viewModel != null) {
            dbManager.get().removeTrigger(viewModel.dao, false, realm)
            syncManager.registerSyncQueue(ESyncDataType.TRIGGER, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
        }

    }

    protected fun onDispose() {
        currentTriggerViewModels.clear()
        currentTriggerRealmResults?.removeAllChangeListeners()
    }

    override fun onCleared() {
        onDispose()
        super.onCleared()
    }

}