package kr.ac.snu.hcil.omnitrack.core.di.configured

import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.annotations.RealmModule
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.analytics.OTUsageLoggingManager
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.UsageLog
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import javax.inject.Qualifier

/**
 * Created by younghokim on 2017. 11. 28..
 */
@Module()
class UsageLoggingModule {
    @Provides
    @Configured
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
    @Configured
    @UsageLogger
    fun makeUsageLogDbRealm(@UsageLogger configuration: RealmConfiguration): Realm {
        return Realm.getInstance(configuration)
    }

    @Provides
    @Configured
    @UsageLogger
    fun makeUsageLogDbRealmFactory(@UsageLogger configuration: RealmConfiguration): Factory<Realm> {
        return object : Factory<Realm> {
            override fun get(): Realm {
                return Realm.getInstance(configuration)
            }
        }
    }

    @Provides
    @Configured
    fun getLoggingManager(configuredContext: ConfiguredContext): IEventLogger {
        return OTUsageLoggingManager(configuredContext)
    }

}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class UsageLogger

@RealmModule(classes = [UsageLog::class])
class UsageLogsRealmModule