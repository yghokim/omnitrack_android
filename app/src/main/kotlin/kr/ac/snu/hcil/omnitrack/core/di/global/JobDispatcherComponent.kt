package kr.ac.snu.hcil.omnitrack.core.di.global

import dagger.Component
import kr.ac.snu.hcil.omnitrack.receivers.PackageReceiver
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 18..
 */
@Singleton
@Component(modules = [JobDispatcherModule::class, ApplicationModule::class])
interface JobDispatcherComponent {

    fun inject(service: OTVersionCheckService)
    fun inject(receiver: PackageReceiver)
}