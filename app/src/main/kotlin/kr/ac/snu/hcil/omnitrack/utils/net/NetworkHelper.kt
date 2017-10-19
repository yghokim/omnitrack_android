package kr.ac.snu.hcil.omnitrack.utils.net

import android.content.Context
import android.net.ConnectivityManager
import kr.ac.snu.hcil.omnitrack.OTApp

/**
 * Created by younghokim on 2016. 10. 25..
 */
object NetworkHelper {

    data class NetworkConnectionInfo(val wifiConnected: Boolean, val mobileConnected: Boolean) {
        val isInternetConnected: Boolean get() = wifiConnected || mobileConnected
    }

    fun isConnectedToInternet(): Boolean {
        val connectivityManager = OTApp.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo != null) {
            val wifi = networkInfo.type == ConnectivityManager.TYPE_WIFI
            val mobile = networkInfo.type == ConnectivityManager.TYPE_MOBILE
            OTApp.logger.writeSystemLog("internet connection: wifi- $wifi, mobile- $mobile", "NetworkHelper")
            return wifi || mobile
        } else return false
    }

    fun getCurrentNetworkConnectionInfo(): NetworkConnectionInfo {
        val connectivityManager = OTApp.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo != null) {
            val wifi = networkInfo.type == ConnectivityManager.TYPE_WIFI
            val mobile = networkInfo.type == ConnectivityManager.TYPE_MOBILE
            return NetworkConnectionInfo(wifi, mobile)
        } else return NetworkConnectionInfo(false, false)
    }
}