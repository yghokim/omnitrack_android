package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.os.Bundle
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger

/**
 * Created by younghokim on 2017. 2. 22..
 */
interface ITriggerConfigurationCoordinator {

    fun applyConfigurationToTrigger(trigger: OTTrigger)

    fun writeConfigurationToBundle(out: Bundle)
    fun readConfigurationFromBundle(bundle: Bundle)


    fun importTriggerConfiguration(trigger: OTTrigger)

    fun validateConfigurations(errorMessagesOut: MutableList<String>): Boolean
}