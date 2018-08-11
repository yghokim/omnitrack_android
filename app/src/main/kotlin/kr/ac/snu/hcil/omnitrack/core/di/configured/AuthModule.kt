package kr.ac.snu.hcil.omnitrack.core.di.configured

import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfiguration
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import javax.inject.Qualifier

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = arrayOf(FirebaseModule::class, NetworkModule::class, ConfiguredModule::class))
class AuthModule {

    @Provides
    fun getCurrentSignInLevel(authManager: OTAuthManager): OTAuthManager.SignedInLevel {
        return authManager.currentSignedInLevel
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