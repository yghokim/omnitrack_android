package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Component
import kr.ac.snu.hcil.omnitrack.services.OTBinaryUploadService
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 15..
 */
@Singleton
@Component(modules = arrayOf(NetworkModule::class))
interface NetworkComponent {
    fun inject(service: OTBinaryUploadService)
}