package kr.ac.snu.hcil.omnitrack.core.configuration

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.di.configured.*
import kr.ac.snu.hcil.omnitrack.core.di.global.ApplicationComponent

/**
 * Created by younghokim on 2017. 12. 17..
 */
class ConfiguredContext(val configuration: OTConfiguration, val applicationComponent: ApplicationComponent) {

    val isActive: Boolean
        get() {
            return applicationComponent.configurationController().currentConfiguredContext == this
        }

    val applicationContext: Context
        get() {
            return applicationComponent.applicationContext()
        }

    private val configuredModule: ConfiguredModule by lazy {
        ConfiguredModule(configuration, this)
    }

    private val authModule: AuthModule by lazy {
        AuthModule(applicationComponent.application())
    }

    private val firebaseModule: FirebaseModule by lazy {
        FirebaseModule()
    }

    private val loggingModule: UsageLoggingModule by lazy {
        UsageLoggingModule()
    }

    private val backendDatabaseModule: BackendDatabaseModule by lazy {
        BackendDatabaseModule()
    }

    private val scheduledJobModule: ScheduledJobModule by lazy {
        ScheduledJobModule()
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
                .plus(firebaseModule)
                .plus(authModule)
                .plus(networkModule)
                .plus(backendDatabaseModule)
                .plus(triggerSystemModule)
                .plus(scheduledJobModule)
                .plus(synchronizationModule)
                .plus(loggingModule)
                .plus(researchModule)
                .plus(ScriptingModule())
                .plus(InformationHelpersModule())
                .build()
    }

    val firebaseComponent: FirebaseComponent by lazy {
        applicationComponent.firebaseComponentBuilder()
                .plus(firebaseModule)
                .plus(configuredModule)
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

    val scheduledJobComponent: ScheduledJobComponent by lazy {
        applicationComponent.scheduledJobComponentBuilder()
                .plus(configuredModule)
                .plus(scheduledJobModule)
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
                .plus(firebaseModule)
                .build()
    }

    fun activateOnSystem() {
        triggerSystemComponent.getTriggerAlarmController().activateOnSystem()
    }

    fun deactivateOnSystem() {
        configuredAppComponent.shortcutPanelManager().disposeShortcutPanel()
    }
}