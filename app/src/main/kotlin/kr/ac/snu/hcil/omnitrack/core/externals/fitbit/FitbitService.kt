package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.ThirdPartyAppDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.externals.OAuth2BasedExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client

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


    val DEFAULT_SCOPES = arrayOf(SCOPE_ACTIVITY, SCOPE_SLEEP, SCOPE_HEARTRATE).joinToString(" ")

    override val thumbResourceId: Int = R.drawable.service_thumb_fitbit
    override val descResourceId: Int = R.string.service_fitbit_desc
    override val nameResourceId: Int = R.string.service_fitbit_name

    override fun onRegisterMeasureFactories(): Array<OTMeasureFactory> {
        return arrayOf(
                FitbitStepCountMeasureFactory,
                FitbitDistanceMeasureFactory,
                FitbitRecentSleepTimeMeasureFactory,
                FitbitHeartRateMeasureFactory
        )
    }

    override fun onRegisterDependencies(): Array<OTSystemDependencyResolver> {
        return super.onRegisterDependencies() + arrayOf(
                ThirdPartyAppDependencyResolver.Builder(OTApplication.app)
                        .setAppName("Fitbit")
                        .setPackageName("com.fitbit.FitbitMobile")
                        .isMandatory(false)
                        .build()
        )
    }

    override fun makeNewAuth2Client(): OAuth2Client {
        val config = OAuth2Client.OAuth2Config()
        config.clientId = OTApplication.app.resources.getString(R.string.fitbit_client_id)
        config.clientSecret = OTApplication.app.resources.getString(R.string.fitbit_client_secret)
        config.scope = DEFAULT_SCOPES
        config.authorizationUrl = AUTHORIZATION_URL
        config.tokenRequestUrl = TOKEN_REQUEST_URL
        config.revokeUrl = REVOKE_URL

        return OAuth2Client(config)
    }

}