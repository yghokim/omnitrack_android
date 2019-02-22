package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.ui.components.tutorial.TutorialManager
import javax.inject.Singleton

@Module()
class UIHelperModule {
    @Provides
    @Singleton
    fun tutorialManager(context: Context): TutorialManager {
        return TutorialManager(context)
    }
}