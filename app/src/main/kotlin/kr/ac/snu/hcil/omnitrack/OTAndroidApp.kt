package kr.ac.snu.hcil.omnitrack

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.di.ApplicationComponent

interface OTAndroidApp {
    val deviceId: String
    val applicationComponent: ApplicationComponent

    fun getPackageName(): String
    fun refreshConfiguration(context: Context)
}