package kr.ac.snu.hcil.omnitrack.core.net

import com.google.gson.Gson
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.core.serialization.getStringCompat
import retrofit2.HttpException

object ServerError {
    const val ERROR_CODE_UNCERTIFIED_CLIENT = "ClientNotCertified"
    const val ERROR_CODE_ILLEGAL_ARTUMENTS = "IllegalArguments"
    const val ERROR_CODE_WRONG_CREDENTIAL = "CredentialWrong"
    const val ERROR_CODE_ILLEGAL_INVITATION_CODE = "IllegalInvitationCode"
    const val ERROR_CODE_USER_ALREADY_EXISTS = "UserAlreadyExists"
    const val ERROR_CODE_USERNAME_NOT_MATCH_RESEARCHER = "UsernameNotMatchResearcher"
    const val ERROR_CODE_ACCOUNT_NOT_EXISTS = "AccountNotExists"

    fun extractServerErrorCode(gson: Gson, exception: HttpException): String? {

        val code = gson.fromJson(exception.response().errorBody()?.charStream(), JsonObject::class.java)
        return code?.getStringCompat("error")
    }
}