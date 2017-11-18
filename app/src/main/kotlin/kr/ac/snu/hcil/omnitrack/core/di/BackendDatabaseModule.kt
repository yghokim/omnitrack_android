package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Module
import dagger.Provides
import io.realm.Realm
import io.realm.RealmConfiguration
import kr.ac.snu.hcil.omnitrack.BuildConfig
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = arrayOf(AuthModule::class, NetworkModule::class, DaoSerializationModule::class))
class BackendDatabaseModule {

    @Provides
    @Singleton
    fun backendDatabaseConfiguration(): RealmConfiguration {
        return RealmConfiguration.Builder()
                .name("backend.db")
                .run {
                    if (BuildConfig.DEBUG) {
                        this.deleteRealmIfMigrationNeeded()
                    } else this
                }
                .build()
    }

    @Provides
    fun makeBackendDbRealm(configuration: RealmConfiguration): Realm {
        return Realm.getInstance(configuration)
    }
}