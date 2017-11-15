package kr.ac.snu.hcil.omnitrack.core.di

import com.firebase.jobdispatcher.Job
import dagger.Component
import kr.ac.snu.hcil.omnitrack.core.net.OTBinaryStorageController
import kr.ac.snu.hcil.omnitrack.receivers.PackageReceiver
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Singleton
@Component(modules = arrayOf(ScheduledJobModule::class, ApplicationModule::class))
interface ScheduledJobComponent {

    @BinaryStorageServer
    fun getBinaryUploadJob(): Provider<Job>

    fun inject(service: OTVersionCheckService)
    fun inject(receiver: PackageReceiver)
    fun inject(controller: OTBinaryStorageController)
}