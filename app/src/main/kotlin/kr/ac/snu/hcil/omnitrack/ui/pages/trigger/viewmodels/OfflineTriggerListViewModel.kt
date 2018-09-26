package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import android.app.Application
import android.os.Bundle
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.typeadapters.ServerCompatibleTypeAdapter
import kr.ac.snu.hcil.omnitrack.core.di.configured.ForTrigger
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 10. 24..
 */
open class OfflineTriggerListViewModel(app: Application) : ATriggerListViewModel(app) {

    @field:[Inject ForTrigger]
    protected lateinit var triggerTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTTriggerDAO>>


    override fun onInject(configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.inject(this)
    }

    fun init(savedInstanceState: Bundle?) {
        currentTriggerViewModels.forEach { it.unregister() }
        currentTriggerViewModels.clear()

        if (savedInstanceState != null) {
            val serializedDaos = savedInstanceState.getStringArray("triggerDaos")
            if (serializedDaos != null && serializedDaos.size > 0) {
                currentTriggerViewModels.addAll(serializedDaos.map { TriggerViewModel(configuredContext, triggerTypeAdapter.get().fromJson(it), realm) })
                notifyNewTriggerViewModels()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray("triggerDaos", currentTriggerViewModels.map { triggerTypeAdapter.get().toJson(it.dao) }.toTypedArray())
    }

    override fun addNewTriggerImpl(dao: OTTriggerDAO) {
        if (dao.objectId != null) {
            val match = currentTriggerViewModels.find { it.objectId == dao.objectId }
            if (match != null) {
                match.apply(dao)
                return
            }
        }

        currentTriggerViewModels.add(TriggerViewModel(configuredContext, dao, realm))
        notifyNewTriggerViewModels()
    }

    override fun removeTrigger(objectId: String) {
        currentTriggerViewModels.find { it.objectId == objectId }?.let {
            it.unregister()
            currentTriggerViewModels.remove(it)
            notifyNewTriggerViewModels()
        }
    }
}