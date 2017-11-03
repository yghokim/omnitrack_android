package kr.ac.snu.hcil.omnitrack.core.di

import android.content.SharedPreferences
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
    fun provideAuthManager(pref: SharedPreferences, server: Lazy<ISynchronizationServerSideAPI>): OTAuthManager {
        return OTAuthManager(app, pref, server)
    }

    @Provides
    fun getCurrentSignInLevel(authManager: OTAuthManager): OTAuthManager.SignedInLevel {
        return authManager.currentSignedInLevel
    }

    @Provides
    @Singleton
    fun getExperimentConsentManager(authManager: OTAuthManager, systemPreferences: SharedPreferences, synchronizationServerController: ISynchronizationServerSideAPI): ExperimentConsentManager {
        return ExperimentConsentManager(authManager, systemPreferences, synchronizationServerController)
    }
}