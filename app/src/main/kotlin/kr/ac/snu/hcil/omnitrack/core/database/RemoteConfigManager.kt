package kr.ac.snu.hcil.omnitrack.core.database

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kr.ac.snu.hcil.omnitrack.BuildConfig
import rx.Single

/**
 * Created by Young-Ho Kim on 2017-04-10.
 */
object RemoteConfigManager {
    private val configInstance: FirebaseRemoteConfig by lazy {
        val settings = FirebaseRemoteConfigSettings.Builder()
                //.setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        val instance = FirebaseRemoteConfig.getInstance()
        instance.setConfigSettings(settings)

        instance
    }

    fun getServerLatestVersionName(): Single<String> {
        return Single.create {
            subscriber ->
            configInstance.fetch(if (BuildConfig.DEBUG) {
                0
            } else {
                3600
            }).addOnCompleteListener {
                task ->
                if (task.isSuccessful) {
                    configInstance.activateFetched()

                    val versionName = configInstance.getString("latest_version")
                    if (versionName != null) {
                        subscriber.onSuccess(versionName)
                    } else {
                        subscriber.onError(Exception("no value returned."))
                    }
                } else {
                    subscriber.onError(Exception("grabbing failed"))
                }
            }
        }
    }

    fun laterVersionExists(): Single<Boolean> {
        return getServerLatestVersionName().map {
            versionName ->
            if (BuildConfig.DEBUG) {
                true
            } else isNewVersionGreater(BuildConfig.VERSION_NAME, versionName)
        }
    }

    fun isNewVersionGreater(existingVersion: String, newVersion: String): Boolean {
        val existingVersionArray = existingVersion.split("\\s|.")
        val newVersionArray = newVersion.split("\\s|.")

        val maxIndex = Math.max(existingVersionArray.size, newVersionArray.size)
        for (i in 0..maxIndex - 1) {
            val oldValue = try {
                existingVersionArray[i]
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }

            val newValue = try {
                newVersionArray[i]
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }

            if (oldValue < newValue) {
                return true
            }
        }
        return false
    }
}