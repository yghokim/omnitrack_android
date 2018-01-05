package kr.ac.snu.hcil.omnitrack.core.di.configured

import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.annotations.RealmModule
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.OTExperimentDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.OTExperimentInvitationDAO
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.net.IResearchServerAPI
import kr.ac.snu.hcil.omnitrack.core.net.OTOfficialServerApiController
import java.io.File
import javax.inject.Qualifier

/**
 * Created by younghokim on 2018. 1. 3..
 */

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class Research

@Module(includes = [NetworkModule::class, ConfiguredModule::class])
class ResearchModule {

    @Provides
    @Configured
    @Research
    fun researchDatabaseConfiguration(@ConfigurationDirectory configDirectory: File): RealmConfiguration {
        return RealmConfiguration.Builder()
                .directory(configDirectory)
                .name("research.db")
                .modules(ResearchRealmModule())
                .run {
                    if (BuildConfig.DEBUG) {
                        this.deleteRealmIfMigrationNeeded()
                    } else this
                }
                .build()
    }

    @Provides
    @Configured
    @Research
    fun makeResearchDbRealmProvider(@Research configuration: RealmConfiguration): Factory<Realm> {
        return Factory<Realm> { Realm.getInstance(configuration) }
    }

    @Provides
    @Configured
    fun provideResearchServerController(controller: OTOfficialServerApiController): IResearchServerAPI {
        return controller
    }

}

@RealmModule(classes = [
    OTExperimentDAO::class,
    OTExperimentInvitationDAO::class
])
class ResearchRealmModule