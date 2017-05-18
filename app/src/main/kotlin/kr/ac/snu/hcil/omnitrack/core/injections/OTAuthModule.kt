package kr.ac.snu.hcil.omnitrack.core.injections

import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager

/**
 * Created by younghokim on 2017. 5. 17..
 */
@Module
class OTAuthModule {

    @Provides
    fun provideSignedInLevel(): OTAuthManager.SignedInLevel {
        return OTAuthManager.currentSignedInLevel
    }
}