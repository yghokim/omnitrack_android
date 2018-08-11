package kr.ac.snu.hcil.omnitrack.core.di.configured

import android.content.Context
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.ui.components.tutorial.TutorialManager

@Module()
class UIHelperModule {
    @Provides
    @Configured
    fun tutorialManager(context: Context): TutorialManager {
        return TutorialManager(context)
    }
}