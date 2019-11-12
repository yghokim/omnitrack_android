package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.annotations.RealmModule
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.analytics.OTUsageLoggingManager
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.UsageLog
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 28..
 */
@Module(includes = [ApplicationModule::class])
class UsageLoggingModule {
    @Provides
    @Singleton
    @UsageLogger
    fun usageLogDatabaseConfiguration(): RealmConfiguration {
        return RealmConfiguration.Builder()
                .name("usage_logs.db")
                .modules(UsageLogsRealmModule())
                .run {
                    if (BuildConfig.DEBUG) {
                        this.deleteRealmIfMigrationNeeded()
                    } else this
                }
                .build()
    }

    @Provides
    @Singleton
    @UsageLogger
    fun makeUsageLogDbRealm(@UsageLogger configuration: RealmConfiguration): Realm {
        return Realm.getInstance(configuration)
    }

    @Provides
    @Singleton
    @UsageLogger
    fun makeUsageLogDbRealmFactory(@UsageLogger configuration: RealmConfiguration): Factory<Realm> {
        return object : Factory<Realm> {
            override fun get(): Realm {
                return Realm.getInstance(configuration)
            }
        }
    }

    @Provides
    @Singleton
    fun getLoggingManager(app: OTAndroidApp, context: Context): IEventLogger {
        return OTUsageLoggingManager(app, context)
    }

}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class UsageLogger

@RealmModule(classes = [UsageLog::class])
class UsageLogsRealmModule