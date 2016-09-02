package kr.ac.snu.hcil.omnitrack.core.externals.rescuetime

import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.TextInputDialogHelper

/**
 * Created by Young-Ho Kim on 2016-09-02.
 */
object RescueTimeService : OTExternalService("RescueTimeService", 0) {

    const val PREFERENCE_API_KEY = "rescuetime_api_key"
    const val PREFERENCE_ACCESS_TOKEN = "rescuetime_access_token"


    override val thumbResourceId: Int = R.drawable.service_thumb_rescuetime

    override val nameResourceId: Int = R.string.service_rescuetime_name

    override val descResourceId: Int = R.string.service_rescuetime_desc

    init {
        _measureFactories += arrayOf(RescueTimeProductivityMeasureFactory, RescueTimeComputerUsageDurationMeasureFactory)
    }

    override fun onDeactivate() {
        preferences.edit().remove(PREFERENCE_API_KEY).remove(PREFERENCE_ACCESS_TOKEN).apply()
    }

    fun getStoredApiKey(): String? {
        if (preferences.contains(PREFERENCE_API_KEY)) {
            return preferences.getString(PREFERENCE_API_KEY, "")
        } else return null
    }

    override fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?) {
        preparedHandler?.invoke(getStoredApiKey() != null)
    }

    override fun handleActivityActivationResultOk(resultData: Intent?) {

    }

    override fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)?) {
        TextInputDialogHelper.makeDialog(context, R.string.msg_rescuetime_api_key_input_title, R.string.msg_rescuetime_api_key_input_hint, true, { text ->
            if (!text.isNullOrBlank()) {
                preferences.edit().putString(PREFERENCE_API_KEY, text.toString()).apply()
                connectedHandler?.invoke(true)
            }
        }, {
            connectedHandler?.invoke(false)
        })
                .show()
    }
}