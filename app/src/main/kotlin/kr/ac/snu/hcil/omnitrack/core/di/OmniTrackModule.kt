package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseStorageHelper
import kr.ac.snu.hcil.omnitrack.core.net.*
import kr.ac.snu.hcil.omnitrack.services.OTFirebaseUploadService
import javax.inject.Singleton

/**
 * Created by younghokim on 2017-11-01.
 */
@Module
class OmniTrackModule(val app: OTApp) {

    @Provides
    @Singleton
    fun provideOfficialBackend(): OTOfficialServerApiController
    {
        return OTOfficialServerApiController(app)
    }

    @Provides
    @Singleton
    fun provideBinaryUploadServiceController(): ABinaryUploadService.ABinaryUploadServiceController {
        return OTFirebaseUploadService.ServiceController(app)
    }

    @Provides
    @Singleton
    fun provideSynchronizationServerController(): ISynchronizationServerSideAPI {
        return provideOfficialBackend()
    }

    @Provides
    @Singleton
    fun provideUserReportServerController(): IUserReportServerAPI {
        return provideOfficialBackend()
    }

    @Provides
    @Singleton
    fun provideBinaryDownloadApi(): IBinaryDownloadAPI
    {
        return FirebaseStorageHelper()
    }

    @Provides
    @Singleton
    fun provideAuthManager(): OTAuthManager {
        return OTAuthManager(app)
    }

    @Provides
    fun getCurrentSignInLevel(authManager: OTAuthManager): OTAuthManager.SignedInLevel {
        return authManager.currentSignedInLevel
    }
}