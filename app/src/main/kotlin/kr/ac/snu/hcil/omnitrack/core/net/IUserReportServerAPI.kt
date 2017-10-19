package kr.ac.snu.hcil.omnitrack.core.net

import com.google.gson.JsonObject
import io.reactivex.Single

/**
 * Created by younghokim on 2017. 10. 19..
 */
interface IUserReportServerAPI {
    fun sendUserReport(inquiryData: JsonObject): Single<Boolean>
}