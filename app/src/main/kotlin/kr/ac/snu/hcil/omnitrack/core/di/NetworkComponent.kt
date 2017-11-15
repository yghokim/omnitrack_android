package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Component
import kr.ac.snu.hcil.omnitrack.services.OTBinaryUploadService
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ImageInputView
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 15..
 */
@Singleton
@Component(modules = arrayOf(NetworkModule::class, AuthModule::class))
interface NetworkComponent {
    fun inject(service: OTBinaryUploadService)

    fun inject(view: ImageInputView)
}