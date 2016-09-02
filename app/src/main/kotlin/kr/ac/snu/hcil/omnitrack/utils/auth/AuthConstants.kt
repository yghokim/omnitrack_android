package kr.ac.snu.hcil.omnitrack.utils.auth

import okhttp3.MediaType
import java.text.SimpleDateFormat

/**
 * Created by Young-Ho Kim on 2016-09-01.
 */
object AuthConstants {
    const val PARAM_CLIENT_ID = "client_id"
    const val PARAM_CLIENT_SECRET = "client_secret"
    const val PARAM_ACCESS_TOKEN = "access_token"

    const val PARAM_SCOPE = "scope"
    const val PARAM_RESPONSE_TYPE = "response_type"
    const val PARAM_REDIRECT_URI = "redirect_uri"
    const val PARAM_CODE = "code"
    const val PARAM_GRANT_TYPE = "grant_type"

    const val VALUE_REDIRECT_URI = "http://localhost/redirect_callback"
    const val VALUE_RESPONSE_TYPE_CODE = "code"
    const val VALUE_RESPONSE_TYPE_TOKEN = "token"

    val CONTENT_TYPE_JSON = MediaType.parse("application/json; charset=utf-8")


    val DATE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

    val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
}