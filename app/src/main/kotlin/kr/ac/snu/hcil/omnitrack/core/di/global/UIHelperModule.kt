package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AttributeViewFactoryManager
import kr.ac.snu.hcil.omnitrack.ui.components.tutorial.TutorialManager
import javax.inject.Singleton

@Module(includes = [InformationHelpersModule::class])
class UIHelperModule {
    @Provides
    @Singleton
    fun tutorialManager(context: Context): TutorialManager {
        return TutorialManager(context)
    }

    @Provides
    @Singleton
    fun provideAttributeViewFactoryManager(attributeManager: Lazy<OTAttributeManager>): AttributeViewFactoryManager {
        return AttributeViewFactoryManager(attributeManager)
    }
}