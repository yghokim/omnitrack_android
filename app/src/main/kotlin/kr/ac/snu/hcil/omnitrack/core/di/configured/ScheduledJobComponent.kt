package kr.ac.snu.hcil.omnitrack.core.di.configured

import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import dagger.Subcomponent
import dagger.internal.Factory
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

    @InformationUpload
    fun getInformationUploadRequestBuilderFactory(): Factory<OneTimeWorkRequest.Builder>

    @ServerFullSync
    fun getFullSyncPeriodicRequestProvider(): Provider<PeriodicWorkRequest>

    fun inject(receiver: PackageReceiver)
    fun inject(controller: OTBinaryStorageController)
}