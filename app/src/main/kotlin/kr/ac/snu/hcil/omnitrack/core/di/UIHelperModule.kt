package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactoryManager
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
    fun provideAttributeViewFactoryManager(fieldManager: Lazy<OTFieldManager>): OTFieldViewFactoryManager {
        return OTFieldViewFactoryManager(fieldManager)
    }
}