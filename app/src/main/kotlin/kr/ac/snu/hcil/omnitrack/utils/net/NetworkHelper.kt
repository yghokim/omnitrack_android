package kr.ac.snu.hcil.omnitrack.utils.net

import android.content.Context
import android.net.ConnectivityManager
import kr.ac.snu.hcil.omnitrack.OTApplication

/**
 * Created by younghokim on 2016. 10. 25..
 */
object NetworkHelper {
    fun isConnectedToInternet(): Boolean {
        val connectivityManager = OTApplication.app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo != null) {
            val wifi = networkInfo.type == ConnectivityManager.TYPE_WIFI
            val mobile = networkInfo.type == ConnectivityManager.TYPE_MOBILE
            OTApplication.logger.writeSystemLog("internet connection: wifi- $wifi, mobile- $mobile", "NetworkHelper")
            return wifi || mobile
        } else return false
    }
}