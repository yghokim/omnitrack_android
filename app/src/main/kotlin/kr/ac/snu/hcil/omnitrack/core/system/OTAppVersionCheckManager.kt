package kr.ac.snu.hcil.omnitrack.core.system

import android.content.Context
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
        val result = BuildConfig.OMNITRACK_SYNCHRONIZATION_SERVER_URL +
                "/api/clients/latest?platform=Android&host=${URLEncoder.encode(BuildConfig.OMNITRACK_SYNCHRONIZATION_SERVER_URL, "UTF-8")}"
        result
    }

    fun makeAppUpdater(context: Context): AppUpdater {
        return AppUpdater(context)
                .setUpdateFrom(UpdateFrom.JSON)
                .setUpdateJSON(updateInfoUrl)
                .setDisplay(Display.DIALOG)
                .setButtonUpdate(R.string.msg_app_update_update_button)
                .setContentOnUpdateAvailable(R.string.msg_app_update_message)
                .setTitleOnUpdateAvailable(R.string.msg_app_update_title)
    }


    fun makeAppUpdaterUtils(context: Context): AppUpdaterUtils {
        return AppUpdaterUtils(context)
                .setUpdateFrom(UpdateFrom.JSON)
                .setUpdateJSON(updateInfoUrl)
    }
}