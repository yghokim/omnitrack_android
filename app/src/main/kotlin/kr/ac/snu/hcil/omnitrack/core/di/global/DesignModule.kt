package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.res.Resources
import android.graphics.Color
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.R
import javax.inject.Qualifier
import javax.inject.Singleton

@Module(includes = [ApplicationModule::class])
class DesignModule {

    @Provides
    @Singleton
    @ColorPalette
    fun getColorPalette(resources: Resources): IntArray {
        return resources.getStringArray(R.array.colorPaletteArray).map { Color.parseColor(it) }.toIntArray()
    }
}


@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ColorPalette