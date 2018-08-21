package kr.ac.snu.hcil.omnitrack

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.di.global.ApplicationComponent
import kr.ac.snu.hcil.omnitrack.core.di.global.JobDispatcherComponent
import kr.ac.snu.hcil.omnitrack.core.di.global.SerializationComponent

interface OTAndroidApp {
    val deviceId: String
    val applicationComponent: ApplicationComponent
    val jobDispatcherComponent: JobDispatcherComponent
    val serializationComponent: SerializationComponent
    val currentConfiguredContext: ConfiguredContext
    fun getPackageName(): String
    fun refreshConfiguration(context: Context)
}