package kr.ac.snu.hcil.omnitrack.core.di.configured

import android.content.Context
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationClientSideAPI
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncQueueDbHelper
import java.io.File

/**
 * Created by younghokim on 2017. 11. 4..
 */
@Module(includes = arrayOf(BackendDatabaseModule::class))
class SynchronizationModule {

    @Provides
    @Configured
    fun provideSyncDbHelper(appContext: Context,
                            @ConfigurationDirectory configuredDirectory: File): SyncQueueDbHelper
    {
        return SyncQueueDbHelper(appContext, File(configuredDirectory, "db_synchronization_queue.sqlite").toString())
    }

    @Provides
    @Configured
    fun provideClientSideApi(backendDatabaseManager: BackendDbManager): ISynchronizationClientSideAPI
    {
        return backendDatabaseManager
    }
}



