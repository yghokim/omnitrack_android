package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import android.content.Context
import android.content.SharedPreferences
import kr.ac.snu.hcil.android.common.net.OAuth2Client
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.ThirdPartyAppDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.externals.OAuth2BasedExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory

/**
 * Created by younghokim on 16. 9. 2..
 */
class FitbitService(context: Context, pref: SharedPreferences) : OAuth2BasedExternalService(context, pref, "FitbitService", 0) {
    companion object {
        const val SCOPE_ACTIVITY = "activity"
        const val SCOPE_HEARTRATE = "heartrate"
        const val SCOPE_SLEEP = "sleep"

        const val AUTHORIZATION_URL = "https://www.fitbit.com/oauth2/authorize"
        const val TOKEN_REQUEST_URL = "https://api.fitbit.com/oauth2/token"
        const val REVOKE_URL = "https://api.fitbit.com/oauth2/revoke"
    }

    override fun isSupportedInSystem(): Boolean {
        return BuildConfig.FITBIT_CLIENT_ID != null && BuildConfig.FITBIT_CLIENT_SECRET != null
    }



    val DEFAULT_SCOPES = arrayOf(SCOPE_ACTIVITY, SCOPE_SLEEP, SCOPE_HEARTRATE).joinToString(" ")

    override val thumbResourceId: Int = R.drawable.service_thumb_fitbit
    override val descResourceId: Int = R.string.service_fitbit_desc
    override val nameResourceId: Int = R.string.service_fitbit_name

    override fun onRegisterMeasureFactories(): Array<OTServiceMeasureFactory> {
        return arrayOf(
                FitbitStepCountMeasureFactory(context, this),
                FitbitDistanceMeasureFactory(context, this),
                FitbitRecentSleepTimeMeasureFactory(context, this),
                FitbitHeartRateMeasureFactory(context, this)
        )
    }

    override fun onRegisterDependencies(): Array<OTSystemDependencyResolver> {
        return super.onRegisterDependencies() + arrayOf(
                ThirdPartyAppDependencyResolver.Builder(context)
                        .setAppName("Fitbit")
                        .setPackageName("com.fitbit.FitbitMobile")
                        .isMandatory(false)
                        .build()
        )
    }

    override fun makeNewAuth2Client(): OAuth2Client {
        val config = OAuth2Client.OAuth2Config()
        config.clientId = BuildConfig.FITBIT_CLIENT_ID
        config.clientSecret = BuildConfig.FITBIT_CLIENT_SECRET
        config.scope = DEFAULT_SCOPES
        config.authorizationUrl = AUTHORIZATION_URL
        config.tokenRequestUrl = TOKEN_REQUEST_URL
        config.revokeUrl = REVOKE_URL

        return OAuth2Client(context, config)
    }

}