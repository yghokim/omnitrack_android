package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Intent
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger

/**
 * Created by younghokim on 2017. 2. 22..
 */
interface ITriggerConfigurationCoordinator {

    fun applyConfigurationToTrigger(trigger: OTTrigger)

    fun writeConfigurationToIntent(out: Intent)

    fun importTriggerConfiguration(trigger: OTTrigger)

    fun validateConfigurations(errorMessagesOut: MutableList<String>): Boolean
}