package kr.ac.snu.hcil.omnitrack.core.di.global

import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = [ApplicationModule::class, NetworkModule::class])
class AuthModule {

    @Provides
    fun getCurrentSignInLevel(authManager: OTAuthManager): OTAuthManager.SignedInLevel {
        return authManager.currentSignedInLevel
    }

    @Provides
    @Singleton
    @ForGeneralAuth
    fun getGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .requestEmail()
                .requestIdToken(BuildConfig.FIREBASE_AUTH_CLIENT_ID)
                .build()
    }
}


@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ForGeneralAuth