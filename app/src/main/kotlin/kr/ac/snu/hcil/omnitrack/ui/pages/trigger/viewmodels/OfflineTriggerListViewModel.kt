package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import android.app.Application
import android.os.Bundle
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.typeadapters.ServerCompatibleTypeAdapter
import kr.ac.snu.hcil.omnitrack.core.di.global.ForTrigger
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 10. 24..
 */
open class OfflineTriggerListViewModel(app: Application) : ATriggerListViewModel(app) {

    @field:[Inject ForTrigger]
    protected lateinit var triggerTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTTriggerDAO>>


    override fun onInject(app: OTAndroidApp) {
        getApplication<OTApp>().applicationComponent.inject(this)
    }

    fun init(savedInstanceState: Bundle?) {
        currentTriggerViewModels.forEach { it.unregister() }
        currentTriggerViewModels.clear()

        if (savedInstanceState != null) {
            val serializedDaos = savedInstanceState.getStringArray("triggerDaos")
            if (serializedDaos != null && serializedDaos.isNotEmpty()) {
                currentTriggerViewModels.addAll(serializedDaos.map { TriggerViewModel(getApplication(), triggerTypeAdapter.get().fromJson(it), realm) })
                notifyNewTriggerViewModels()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray("triggerDaos", currentTriggerViewModels.map { triggerTypeAdapter.get().toJson(it.dao) }.toTypedArray())
    }

    override fun addNewTriggerImpl(dao: OTTriggerDAO) {
        if (dao._id != null) {
            val match = currentTriggerViewModels.find { it._id == dao._id }
            if (match != null) {
                match.apply(dao)
                return
            }
        }

        currentTriggerViewModels.add(TriggerViewModel(getApplication(), dao, realm))
        notifyNewTriggerViewModels()
    }

    override fun removeTrigger(objectId: String) {
        currentTriggerViewModels.find { it._id == objectId }?.let {
            it.unregister()
            currentTriggerViewModels.remove(it)
            notifyNewTriggerViewModels()
        }
    }
}