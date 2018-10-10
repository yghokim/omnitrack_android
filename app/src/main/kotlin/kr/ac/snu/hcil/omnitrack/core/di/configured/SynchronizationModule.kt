package kr.ac.snu.hcil.omnitrack.core.di.configured

import android.content.Context
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationClientSideAPI
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncQueueDbHelper

/**
 * Created by younghokim on 2017. 11. 4..
 */
@Module(includes = [BackendDatabaseModule::class])
class SynchronizationModule {

    @Provides
    @Configured
    fun provideSyncDbHelper(appContext: Context): SyncQueueDbHelper
    {
        return SyncQueueDbHelper(appContext, "db_synchronization_queue.sqlite")
    }

    @Provides
    @Configured
    fun provideClientSideApi(backendDatabaseManager: BackendDbManager): ISynchronizationClientSideAPI
    {
        return backendDatabaseManager
    }
}



