package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Lazy
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.ExperimentConsentManager
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = arrayOf(NetworkModule::class))
class AuthModule(val app: OTApp) {

    @Provides
    @Singleton
    fun provideAuthManager(server: Lazy<ISynchronizationServerSideAPI>): OTAuthManager {
        return OTAuthManager(app, server)
    }

    @Provides
    fun getCurrentSignInLevel(authManager: OTAuthManager): OTAuthManager.SignedInLevel {
        return authManager.currentSignedInLevel
    }

    @Provides
    @Singleton
    fun getExperimentConsentManager(authManager: OTAuthManager, synchronizationServerController: ISynchronizationServerSideAPI): ExperimentConsentManager {
        return ExperimentConsentManager(authManager, synchronizationServerController)
    }
}