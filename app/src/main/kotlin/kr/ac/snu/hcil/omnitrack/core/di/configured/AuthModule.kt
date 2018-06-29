package kr.ac.snu.hcil.omnitrack.core.di.configured

import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.ExperimentConsentManager
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfiguration
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import javax.inject.Qualifier

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = arrayOf(FirebaseModule::class, NetworkModule::class, ConfiguredModule::class))
class AuthModule(val app: OTApp) {

    @Provides
    fun getCurrentSignInLevel(authManager: OTAuthManager): OTAuthManager.SignedInLevel {
        return authManager.currentSignedInLevel
    }

    @Provides
    @Configured
    fun getExperimentConsentManager(authManager: OTAuthManager, synchronizationServerController: ISynchronizationServerSideAPI): ExperimentConsentManager {
        return ExperimentConsentManager(authManager, synchronizationServerController)
    }

    @Provides
    @Configured
    @ForGeneralAuth
    fun getGoogleSignInOptions(config: OTConfiguration): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestEmail()
                .requestIdToken(config.googleAuthClientId)
                .build()
    }
}


@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForGeneralAuth