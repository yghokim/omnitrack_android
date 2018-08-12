package kr.ac.snu.hcil.omnitrack.core.system

import android.content.Context
import android.net.Uri
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.R
import java.net.URLEncoder

/**
 * Created by younghokim on 2018. 2. 8..
 */
object OTAppVersionCheckManager {
    val updateInfoUrl: String by lazy {
        Uri.parse(BuildConfig.OMNITRACK_SYNCHRONIZATION_SERVER_URL +
                "/api/clients/latest").buildUpon()
                .appendQueryParameter("platform", "Android")
                .apply {
                    if (BuildConfig.DEFAULT_EXPERIMENT_ID != null) {
                        this.appendQueryParameter("experimentId", BuildConfig.DEFAULT_EXPERIMENT_ID)
                    }
                }
                .appendQueryParameter("host", URLEncoder.encode(BuildConfig.OMNITRACK_SYNCHRONIZATION_SERVER_URL, "UTF-8"))
                .build().toString()
    }

    fun makeAppUpdater(context: Context): AppUpdater {
        return AppUpdater(context)
                .setUpdateFrom(UpdateFrom.JSON)
                .setUpdateJSON(updateInfoUrl)
                .setDisplay(Display.DIALOG)
                .setButtonUpdate(R.string.msg_app_update_update_button)
                .setContentOnUpdateAvailable(R.string.msg_app_update_message)
                .setTitleOnUpdateAvailable(R.string.msg_app_update_title)
                .setAppId(BuildConfig.APPLICATION_ID)
                .setIcon(R.drawable.icon_simple)
    }


    fun makeAppUpdaterUtils(context: Context): AppUpdaterUtils {
        return AppUpdaterUtils(context)
                .setUpdateFrom(UpdateFrom.JSON)
                .setUpdateJSON(updateInfoUrl)
    }
}