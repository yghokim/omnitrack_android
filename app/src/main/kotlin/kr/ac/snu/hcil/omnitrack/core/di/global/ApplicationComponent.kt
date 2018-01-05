package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.Component
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfigurationController
import kr.ac.snu.hcil.omnitrack.core.di.configured.*
import kr.ac.snu.hcil.omnitrack.services.OTInformationUploadService
import kr.ac.snu.hcil.omnitrack.services.OTReminderService
import kr.ac.snu.hcil.omnitrack.services.messaging.OTFirebaseInstanceIdService
import kr.ac.snu.hcil.omnitrack.services.messaging.OTFirebaseMessagingService
import kr.ac.snu.hcil.omnitrack.utils.ConfigurableJobService
import kr.ac.snu.hcil.omnitrack.utils.ConfigurableWakefulService
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetProvider
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetService
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 15..
 */
@Singleton
@Component(modules = [ApplicationModule::class, JobDispatcherModule::class, AppDatabaseModule::class, SerializationModule::class, SystemIdentifierFactoryModule::class])
interface ApplicationComponent {

    fun configurationController(): OTConfigurationController

    @Default
    fun defaultPreferences(): SharedPreferences

    fun application(): OTApp

    fun applicationContext(): Context

    @ForGeneric
    fun genericGson(): Gson

    fun configuredAppComponentBuilder(): ConfiguredAppComponent.Builder
    fun scheduledJobComponentBuilder(): ScheduledJobComponent.Builder
    fun triggerSystemComponentBuilder(): TriggerSystemComponent.Builder
    fun firebaseComponentBuilder(): FirebaseComponent.Builder
    fun daoSerializationComponentBuilder(): DaoSerializationComponent.Builder
    fun researchComponentBuilder(): ResearchComponent.Builder

    fun inject(service: OTFirebaseInstanceIdService)
    fun inject(service: OTFirebaseMessagingService)
    fun inject(service: OTInformationUploadService)
    fun inject(service: ConfigurableJobService)
    fun inject(service: ConfigurableWakefulService)
    fun inject(service: OTReminderService)

    fun inject(service: OTShortcutPanelWidgetService)
    fun inject(service: OTShortcutPanelWidgetUpdateService)
    fun inject(provider: OTShortcutPanelWidgetProvider)
}