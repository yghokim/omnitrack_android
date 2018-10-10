package kr.ac.snu.hcil.omnitrack.core.di.configured

import com.firebase.jobdispatcher.Job
import dagger.Subcomponent
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.net.OTBinaryStorageController
import kr.ac.snu.hcil.omnitrack.receivers.PackageReceiver
import javax.inject.Provider

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Configured
@Subcomponent(modules = [ScheduledJobModule::class])
interface ScheduledJobComponent {

    @Subcomponent.Builder
    interface Builder {
        fun plus(module: ConfiguredModule): Builder
        fun plus(module: ScheduledJobModule): Builder
        fun build(): ScheduledJobComponent
    }

    fun getInformationUploadJobBuilderProvider(): Provider<Job.Builder>

    @ServerSync
    fun getFullSyncPeriodicJob(): Provider<Job>

    fun inject(receiver: PackageReceiver)
    fun inject(controller: OTBinaryStorageController)
}