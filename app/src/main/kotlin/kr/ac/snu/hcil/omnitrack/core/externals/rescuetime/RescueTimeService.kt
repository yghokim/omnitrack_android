package kr.ac.snu.hcil.omnitrack.core.externals.rescuetime

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OAuth2BasedExternalService
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-02.
 */
object RescueTimeService : OAuth2BasedExternalService("RescueTimeService", 0) {

    //const val PREFERENCE_API_KEY = "rescuetime_api_key"
    //const val PREFERENCE_ACCESS_TOKEN = "rescuetime_access_token"


    interface ISummaryCalculator<T> {
        fun calculate(list: List<JSONObject>, startDate: Date, endDate: Date): T?
    }

    const val AUTHORIZATION_URL = "https://www.rescuetime.com/oauth/authorize"
    const val TOKEN_REQUEST_URL = "https://www.rescuetime.com/oauth/token"
    const val REVOKE_URL = "https://www.rescuetime.com/oauth/revoke"

    val DEFAULT_SCOPES = arrayOf("time_data", "category_data", "productivity_data").joinToString(" ")

    const val URL_ROOT = "rescuetime.com"
    const val SUBURL_ANALYTICS_API = "api/oauth"
    const val SUBURL_DATA = "data"
    const val SUBURL_SUMMARY = "daily_summary_feed"

    const val SUMMARY_VARIABLE_PRODUCTIVITY = "productivity_pulse"
    const val SUMMARY_VARIABLE_TOTAL_HOURS = "total_hours"

    override val thumbResourceId: Int = R.drawable.service_thumb_rescuetime

    override val nameResourceId: Int = R.string.service_rescuetime_name

    override val descResourceId: Int = R.string.service_rescuetime_desc

    init {
        _measureFactories += arrayOf(RescueTimeProductivityMeasureFactory, RescueTimeComputerUsageDurationMeasureFactory)

        assignRequestCode(this)
    }

    override fun makeNewAuth2Client(requestCode: Int): OAuth2Client {
        val config = OAuth2Client.OAuth2Config()
        config.clientId = OTApplication.app.resources.getString(R.string.rescuetime_client_id)
        config.clientSecret = OTApplication.app.resources.getString(R.string.rescuetime_client_secret)
        config.authorizationUrl = AUTHORIZATION_URL
        config.tokenRequestUrl = TOKEN_REQUEST_URL
        config.revokeUrl = REVOKE_URL
        config.scope = DEFAULT_SCOPES
        config.redirectUri = OTApplication.app.resources.getString(R.string.rescuetime_redirect_uri)

        return OAuth2Client(config, requestCode)
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

    fun <T> requestSummary(startDate: Date, endDate: Date, calculator: ISummaryCalculator<T>, resultHandler: (T?) -> Unit) {
        request(makeSummaryRequestUrl(SUBURL_SUMMARY), Converter<T>(startDate, endDate, calculator), resultHandler)
    }

    internal class Converter<T>(val startDate: Date, val endDate: Date, val calculator: ISummaryCalculator<T>) : OAuth2Client.OAuth2RequestConverter<T?> {
        override fun process(requestResultStrings: Array<String>): T? {
            try {
                val array = JSONArray(requestResultStrings.first())

                val list = ArrayList<JSONObject>()

                for (i in 0..array.length() - 1) {
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