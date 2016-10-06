package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OAuth2BasedExternalService
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import java.util.*

/**
 * Created by younghokim on 16. 9. 2..
 */
object FitbitService : OAuth2BasedExternalService("FitbitService", 0) {

    const val SCOPE_ACTIVITY = "activity"
    const val SCOPE_HEARTRATE = "heartrate"
    const val SCOPE_SLEEP = "sleep"

    const val AUTHORIZATION_URL = "https://www.fitbit.com/oauth2/authorize"
    const val TOKEN_REQUEST_URL = "https://api.fitbit.com/oauth2/token"
    const val REVOKE_URL = "https://api.fitbit.com/oauth2/revoke"

    const val REQUEST_URL_SIMPLE_COMMAND_DATE_FORMAT = "https://api.fitbit.com/1/user/-/%s/date/%s.json"


    const val REQUEST_COMMAND_SUMMARY = "activities"
    const val REQUEST_COMMAND_SLEEP = "sleep"


    val DEFAULT_SCOPES = arrayOf(SCOPE_ACTIVITY, SCOPE_SLEEP, SCOPE_HEARTRATE).joinToString(" ")

    override val thumbResourceId: Int = R.drawable.service_thumb_fitbit
    override val descResourceId: Int = R.string.service_fitbit_desc
    override val nameResourceId: Int = R.string.service_fitbit_name

    init {
        _measureFactories.add(FitbitStepCountMeasureFactory)
        _measureFactories.add(FitbitRecentSleepTimeMeasureFactory)
        _measureFactories.add(FitbitHeartRateMeasureFactory)

        assignRequestCode(this)
    }

    override fun makeNewAuth2Client(requestCode: Int): OAuth2Client {
        val config = OAuth2Client.OAuth2Config()
        config.clientId = OTApplication.app.resources.getString(R.string.fitbit_client_id)
        config.clientSecret = OTApplication.app.resources.getString(R.string.fitbit_client_secret)
        config.scope = DEFAULT_SCOPES
        config.authorizationUrl = AUTHORIZATION_URL
        config.tokenRequestUrl = TOKEN_REQUEST_URL
        config.revokeUrl = REVOKE_URL

        return OAuth2Client(config, requestCode)
    }

    fun makeRequestUrlWithCommandAndDate(command: String, date: Date): String {
        return String.format(REQUEST_URL_SIMPLE_COMMAND_DATE_FORMAT, command, AuthConstants.DATE_FORMAT.format(date))
    }

}