package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import android.content.res.Resources
import dagger.Module
import dagger.Provides
import io.realm.Realm
import io.realm.RealmConfiguration
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import javax.inject.Singleton

/**
 * Created by Young-Ho on 10/31/2017.
 */
@Module(subcomponents = arrayOf(DaoSerializationComponent::class))
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
    @Singleton
    fun localDatabaseConfiguration(): RealmDatabaseManager.Configuration
    {
        return RealmDatabaseManager.Configuration()
    }

    @Provides
    fun makeLocalDbRealm(configuration: RealmDatabaseManager.Configuration): Realm
    {
        return Realm.getInstance(RealmConfiguration.Builder().name(configuration.fileName).build())
    }
}