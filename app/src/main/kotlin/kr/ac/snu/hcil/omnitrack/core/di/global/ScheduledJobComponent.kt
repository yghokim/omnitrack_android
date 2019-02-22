package kr.ac.snu.hcil.omnitrack.core.di.global

import androidx.work.PeriodicWorkRequest
import dagger.Component
import kr.ac.snu.hcil.omnitrack.core.net.OTBinaryStorageController
import kr.ac.snu.hcil.omnitrack.receivers.PackageReceiver
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Singleton
@Component(modules = [ScheduledJobModule::class, ApplicationModule::class])
interface ScheduledJobComponent {

    @ServerFullSync
    fun getFullSyncPeriodicRequestProvider(): Provider<PeriodicWorkRequest>

    fun inject(receiver: PackageReceiver)
    fun inject(controller: OTBinaryStorageController)
}