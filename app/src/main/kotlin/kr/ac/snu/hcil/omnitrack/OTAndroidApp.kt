package kr.ac.snu.hcil.omnitrack

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.di.global.ApplicationComponent
import kr.ac.snu.hcil.omnitrack.core.di.global.FirebaseComponent
import kr.ac.snu.hcil.omnitrack.core.di.global.ScheduledJobComponent
import kr.ac.snu.hcil.omnitrack.core.di.global.SerializationComponent

interface OTAndroidApp {
    val deviceId: String
    val applicationComponent: ApplicationComponent
    val scheduledJobComponent: ScheduledJobComponent
    val serializationComponent: SerializationComponent
    val firebaseComponent: FirebaseComponent
    val currentConfiguredContext: ConfiguredContext
    fun getPackageName(): String
    fun refreshConfiguration(context: Context)
}