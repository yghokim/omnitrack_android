package kr.ac.snu.hcil.omnitrack.core.di.global

import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module(includes = [ApplicationModule::class, NetworkModule::class])
class AuthModule {

    @Provides
    fun getCurrentSignInLevel(authManager: OTAuthManager): OTAuthManager.SignedInLevel {
        return authManager.currentSignedInLevel
    }
}