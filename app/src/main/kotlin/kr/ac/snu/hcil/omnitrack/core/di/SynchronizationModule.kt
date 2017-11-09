package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationClientSideAPI
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncQueueDbHelper
import javax.inject.Qualifier

/**
 * Created by younghokim on 2017. 11. 4..
 */
@ApplicationScope
@Module
class SynchronizationModule {

    @Provides
    @ApplicationScope
    fun provideSyncDbHelper(appContext: Context): SyncQueueDbHelper
    {
        return SyncQueueDbHelper(appContext, "db_synchronization_queue")
    }

    @Provides
    @ApplicationScope
    fun provideClientSideApi(backendDatabaseManager: RealmDatabaseManager): ISynchronizationClientSideAPI
    {
        return backendDatabaseManager
    }
}


@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class FullSync

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class Download

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class Upload



