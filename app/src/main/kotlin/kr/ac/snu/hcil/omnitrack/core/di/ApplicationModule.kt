package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.OTApp

/**
 * Created by Young-Ho on 10/31/2017.
 */
@Module(subcomponents = arrayOf(DaoSerializationComponent::class, ScheduledJobComponent::class))
class ApplicationModule(private val mApp: OTApp) {

    @Provides
    fun wrappedContext(): Context
    {
        return mApp.contextCompat
    }

    @Provides
    fun wrappedResources(): Resources
    {
        return mApp.resourcesWrapped
    }

    @Provides
    fun sharedPreferences(): SharedPreferences
    {
        return PreferenceManager.getDefaultSharedPreferences(mApp)
    }
}