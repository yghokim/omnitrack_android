package kr.ac.snu.hcil.omnitrack.core.externals.rescuetime

import android.content.Context
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OAuth2BasedExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-02.
 */
class RescueTimeService(context: Context) : OAuth2BasedExternalService(context, "RescueTimeService", 0) {

    companion object {
        const val AUTHORIZATION_URL = "https://www.rescuetime.com/oauth/authorize"
        const val TOKEN_REQUEST_URL = "https://www.rescuetime.com/oauth/token"
        const val REVOKE_URL = "https://www.rescuetime.com/oauth/revoke"
        const val URL_ROOT = "rescuetime.com"
        const val SUBURL_ANALYTICS_API = "api/oauth"
        const val SUBURL_DATA = "data"
        const val SUBURL_SUMMARY = "daily_summary_feed"

        const val SUMMARY_VARIABLE_PRODUCTIVITY = "productivity_pulse"
        const val SUMMARY_VARIABLE_TOTAL_HOURS = "total_hours"

    }

    override fun isSupportedInSystem(): Boolean {
        return BuildConfig.RESCUETIME_CLIENT_ID != null && BuildConfig.RESCUETIME_CLIENT_SECRET != null && BuildConfig.RESCUETIME_REDIRECT_URI != null
    }

    //const val PREFERENCE_API_KEY = "rescuetime_api_key"
    //const val PREFERENCE_ACCESS_TOKEN = "rescuetime_access_token"


    interface ISummaryCalculator<T> {
        fun calculate(list: List<JSONObject>, startDate: Date, endDate: Date): T?
    }


    val DEFAULT_SCOPES = arrayOf("time_data", "category_data", "productivity_data").joinToString(" ")

    override val thumbResourceId: Int = R.drawable.service_thumb_rescuetime

    override val nameResourceId: Int = R.string.service_rescuetime_name

    override val descResourceId: Int = R.string.service_rescuetime_desc


    override fun onRegisterMeasureFactories(): Array<OTMeasureFactory> {
        return arrayOf(
                RescueTimeProductivityMeasureFactory(context, this),
                RescueTimeComputerUsageDurationMeasureFactory(context, this)
        )
    }

    override fun makeNewAuth2Client(): OAuth2Client {
        val config = OAuth2Client.OAuth2Config()
        config.clientId = BuildConfig.RESCUETIME_CLIENT_ID
        config.clientSecret = BuildConfig.RESCUETIME_CLIENT_SECRET
        config.authorizationUrl = AUTHORIZATION_URL
        config.tokenRequestUrl = TOKEN_REQUEST_URL
        config.revokeUrl = REVOKE_URL
        config.scope = DEFAULT_SCOPES
        config.redirectUri = BuildConfig.RESCUETIME_REDIRECT_URI

        return OAuth2Client(context, config)
    }

    fun makeSummaryRequestUrl(subUrl: String, parameters: Map<String, String> = mapOf()): String {
        val uriBuilder = HttpUrl.Builder().scheme("https").host(URL_ROOT).addPathSegments(SUBURL_ANALYTICS_API).addPathSegments(subUrl)
        uriBuilder.addQueryParameter(AuthConstants.PARAM_ACCESS_TOKEN, credential?.accessToken)

        for (paramEntry in parameters) {
            uriBuilder.addQueryParameter(paramEntry.key, paramEntry.value)
        }

        println(uriBuilder.toString())

        return uriBuilder.toString()
    }

    fun <T> getSummaryRequest(startDate: Date, endDate: Date, calculator: ISummaryCalculator<T>): Single<Nullable<T>> {
        return getRequest(Converter(startDate, endDate, calculator), makeSummaryRequestUrl(SUBURL_SUMMARY)) as Single<Nullable<T>>
    }

    internal class Converter<T>(val startDate: Date, val endDate: Date, val calculator: ISummaryCalculator<T>) : OAuth2Client.OAuth2RequestConverter<T?> {
        override fun process(requestResultStrings: Array<String>): T? {
            try {
                val array = JSONArray(requestResultStrings.first())

                val list = ArrayList<JSONObject>()

                for (i in 0 until array.length()) {
                    val element = array.getJSONObject(i)
                    val date = AuthConstants.DATE_FORMAT.parse(element.getString("date"))
                    if (date >= startDate && date < endDate) {
                        list.add(element)
                    }
                }

                return calculator.calculate(list, startDate, endDate)
            } catch(e: Exception) {
                e.printStackTrace()
                return null
            }
        }

    }
}