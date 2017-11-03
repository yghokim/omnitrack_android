package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Module
import dagger.Provides
import io.realm.Realm
import io.realm.RealmConfiguration
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = arrayOf(AuthModule::class, NetworkModule::class))
class LocalDatabaseModule {

    @Provides
    @Singleton
    fun localDatabaseConfiguration(): RealmDatabaseManager.Configuration {
        return RealmDatabaseManager.Configuration()
    }

    @Provides
    fun makeLocalDbRealm(configuration: RealmDatabaseManager.Configuration): Realm {
        return Realm.getInstance(RealmConfiguration.Builder().name(configuration.fileName).build())
    }
}