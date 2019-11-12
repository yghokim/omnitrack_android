package kr.ac.snu.hcil.android.common.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Created by younghokim on 2016. 10. 25..
 */
object NetworkHelper {

    data class NetworkConnectionInfo(val internetConnected: Boolean, val isUnMetered: Boolean)

    @Suppress("DEPRECATION")
    fun getCurrentNetworkConnectionInfo(context: Context): NetworkConnectionInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT < 21) {
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null) {
                val wifi = networkInfo.type == ConnectivityManager.TYPE_WIFI
                val mobile = networkInfo.type == ConnectivityManager.TYPE_MOBILE
                return NetworkConnectionInfo(wifi || mobile, wifi)
            } else return NetworkConnectionInfo(false, false)
        } else {
            val networks = connectivityManager.allNetworks
            var networkConnected: Boolean = false
            var unMetered: Boolean = false

            networks.forEach { network ->
                val caps = connectivityManager.getNetworkCapabilities(network)
                if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                    networkConnected = true
                }
                if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true) {
                    unMetered = true
                }
            }

            return NetworkConnectionInfo(networkConnected, unMetered)

        }
    }
}