package kr.ac.snu.hcil.omnitrack.core.injections

import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.OTApplication
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 5. 17..
 */
@Module
class OTApplicationModule(private val app: OTApplication) {
    @Provides @Singleton fun provideApplicationContext(): OTApplication {
        return app
    }
}