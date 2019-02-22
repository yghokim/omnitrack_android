package kr.ac.snu.hcil.omnitrack.core.configuration

import android.content.Context
import androidx.annotation.StringRes
import androidx.work.WorkManager
import kr.ac.snu.hcil.omnitrack.core.di.configured.*
import kr.ac.snu.hcil.omnitrack.core.di.global.ApplicationComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 17..
 */
@Singleton
class ConfiguredContext @Inject constructor(val applicationComponent: ApplicationComponent) {

    val applicationContext: Context
        get() {
            return applicationComponent.applicationContext().get()
        }

    private val configuredModule: ConfiguredModule by lazy {
        ConfiguredModule(this)
    }

    private val authModule: AuthModule by lazy {
        AuthModule()
    }

    private val backendDatabaseModule: BackendDatabaseModule by lazy {
        BackendDatabaseModule()
    }

    private val networkModule: NetworkModule by lazy {
        NetworkModule()
    }

    private val synchronizationModule: SynchronizationModule by lazy {
        SynchronizationModule()
    }

    private val triggerSystemModule: TriggerSystemModule by lazy {
        TriggerSystemModule()
    }

    private val daoSerializationModule: DaoSerializationModule by lazy {
        DaoSerializationModule()
    }

    private val researchModule: ResearchModule by lazy {
        ResearchModule()
    }


    val configuredAppComponent: ConfiguredAppComponent by lazy {
        applicationComponent.configuredAppComponentBuilder()
                .plus(configuredModule)
                .plus(authModule)
                .plus(networkModule)
                .plus(backendDatabaseModule)
                .plus(triggerSystemModule)
                .plus(synchronizationModule)
                .plus(researchModule)
                .plus(ScriptingModule())
                .plus(InformationHelpersModule())
                .build()
    }

    val daoSerializationComponent: DaoSerializationComponent by lazy {
        applicationComponent.daoSerializationComponentBuilder()
                .plus(daoSerializationModule)
                .plus(backendDatabaseModule)
                .plus(authModule)
                .plus(configuredModule)
                .build()
    }

    val triggerSystemComponent: TriggerSystemComponent by lazy {
        applicationComponent.triggerSystemComponentBuilder()
                .plus(authModule)
                .plus(networkModule)
                .plus(configuredModule)
                .plus(triggerSystemModule)
                .plus(backendDatabaseModule)
                .build()
    }

    val researchComponent: ResearchComponent by lazy {
        applicationComponent.researchComponentBuilder()
                .plus(configuredModule)
                .plus(researchModule)
                .plus(networkModule)
                .plus(authModule)
                .build()
    }

    fun getString(@StringRes id: Int): String {
        return applicationComponent.wrappedResources().get().getString(id)
    }

    fun activateOnSystem() {
        //triggerSystemComponent.getTriggerAlarmController().activateOnSystem()

        WorkManager.getInstance().enqueue(applicationComponent.application().scheduledJobComponent.getFullSyncPeriodicRequestProvider().get())
        //applicationComponent.jobDispatcher().mustSchedule(scheduledJobComponent.getFullSyncPeriodicJob().get())
    }

    fun deactivateOnSystem() {
        configuredAppComponent.shortcutPanelManager().disposeShortcutPanel()
    }
}