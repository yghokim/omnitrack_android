package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import com.google.gson.Gson
import dagger.Component
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.di.configured.ConfiguredAppComponent
import kr.ac.snu.hcil.omnitrack.core.di.configured.DaoSerializationComponent
import kr.ac.snu.hcil.omnitrack.core.di.configured.ResearchComponent
import kr.ac.snu.hcil.omnitrack.core.di.configured.TriggerSystemComponent
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.services.messaging.OTFirebaseMessagingService
import kr.ac.snu.hcil.omnitrack.ui.components.common.ColorPaletteView
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.MultiLineChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages.SourceSelectionPage
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.EventTriggerConfigurationPanel
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetProvider
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetService
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 15..
 */
@Singleton
@Component(modules = [ApplicationModule::class, FirebaseModule::class, UsageLoggingModule::class, ScheduledJobModule::class, UIHelperModule::class, SerializationModule::class, DesignModule::class, ExternalServiceModule::class, SystemIdentifierFactoryModule::class])
interface ApplicationComponent {

    fun configuredContext(): ConfiguredContext

    @Default
    fun defaultPreferences(): SharedPreferences

    fun application(): OTAndroidApp

    fun applicationContext(): Provider<Context>

    fun wrappedResources(): Provider<Resources>

    @ForGeneric
    fun genericGson(): Gson

    fun configuredAppComponentBuilder(): ConfiguredAppComponent.Builder
    fun triggerSystemComponentBuilder(): TriggerSystemComponent.Builder
    fun daoSerializationComponentBuilder(): DaoSerializationComponent.Builder
    fun researchComponentBuilder(): ResearchComponent.Builder

    fun inject(service: OTFirebaseMessagingService)

    fun inject(service: OTShortcutPanelWidgetService)
    fun inject(service: OTShortcutPanelWidgetUpdateService)
    fun inject(provider: OTShortcutPanelWidgetProvider)

    fun inject(view: ColorPaletteView)
    fun inject(drawer: MultiLineChartDrawer)

    fun inject(externalService: OTExternalService)

    fun inject(panel: EventTriggerConfigurationPanel)
    fun inject(page: SourceSelectionPage)
}