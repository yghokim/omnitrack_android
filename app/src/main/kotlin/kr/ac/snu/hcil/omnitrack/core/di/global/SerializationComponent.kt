package kr.ac.snu.hcil.omnitrack.core.di.global

import com.google.gson.Gson
import dagger.Component
import javax.inject.Singleton

/**
 * Created by younghokim on 2018-01-05.
 */
@Singleton
@Component(modules = [SerializationModule::class])
interface SerializationComponent {

    @ForGeneric
    fun genericGson(): Gson
}