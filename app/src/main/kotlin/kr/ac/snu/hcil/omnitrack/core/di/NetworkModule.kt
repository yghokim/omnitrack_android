package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.net.*
import javax.inject.Singleton

/**
 * Created by younghokim on 2017-11-01.
 */
@Module(includes = arrayOf(ApplicationModule::class))
class NetworkModule {

    @Provides
    @Singleton
    fun provideOfficialServerController(app: OTApp): OTOfficialServerApiController
    {
        return OTOfficialServerApiController(app)
    }

    @Provides
    @Singleton
    fun provideBinaryStorageController(context: Context, core: IBinaryStorageCore): OTBinaryStorageController {
        return OTBinaryStorageController(context, core)
    }

    @Provides
    @Singleton
    fun provideBinaryStorageCore(): IBinaryStorageCore {
        return OTFirebaseStorageCore()
    }

    @Provides
    @Singleton
    fun provideSynchronizationServerController(app: OTApp): ISynchronizationServerSideAPI {
        return provideOfficialServerController(app)
    }

    @Provides
    @Singleton
    fun provideUserReportServerController(app: OTApp): IUserReportServerAPI {
        return provideOfficialServerController(app)
    }
}