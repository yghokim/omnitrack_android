package kr.ac.snu.hcil.omnitrack

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.di.global.*

interface OTAndroidApp {
    val deviceId: String
    val applicationComponent: ApplicationComponent
    val scheduledJobComponent: ScheduledJobComponent
    val serializationComponent: SerializationComponent
    val firebaseComponent: FirebaseComponent
    val triggerSystemComponent: TriggerSystemComponent
    val researchComponent: ResearchComponent
    val daoSerializationComponent: DaoSerializationComponent

    fun getPackageName(): String
    fun refreshConfiguration(context: Context)
}