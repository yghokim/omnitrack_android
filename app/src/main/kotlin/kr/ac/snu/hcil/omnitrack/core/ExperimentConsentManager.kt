package kr.ac.snu.hcil.omnitrack.core

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import kr.ac.snu.hcil.omnitrack.ui.pages.experiment.ExperimentSignInActivity
import rx_activity_result2.RxActivityResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Young-Ho Kim on 2017-01-31.
 */
@Singleton
class ExperimentConsentManager @Inject constructor(val authManager: OTAuthManager, val synchronizationServerController: ISynchronizationServerSideAPI) {

    fun startProcess(activity: AppCompatActivity): Single<Boolean> {

        return Single.defer {
            if (authManager.getIsConsentApproved()) {
                return@defer Single.just(true)
            } else {
                return@defer RxActivityResult.on(activity).startIntent(Intent(activity, ExperimentSignInActivity::class.java)).singleOrError()
                        .flatMap { activityResult ->

                            if (activityResult.resultCode() == AppCompatActivity.RESULT_OK) {
                                val data = activityResult.data()
                                val profile = OTUserRolePOJO()
                                profile.role = "ServiceUser"
                                profile.isConsentApproved = true
                                val information = HashMap<String, Any>()
                                information["age"] = data.getStringExtra(OTApp.ACCOUNT_DATASET_EXPERIMENT_KEY_AGE_GROUP)
                                information["country"] = data.getStringExtra(OTApp.ACCOUNT_DATASET_EXPERIMENT_KEY_COUNTRY)
                                information["gender"] = data.getStringExtra(OTApp.ACCOUNT_DATASET_EXPERIMENT_KEY_GENDER)
                                information["purposes"] = data.getStringArrayListExtra(OTApp.ACCOUNT_DATASET_EXPERIMENT_KEY_PURPOSES)
                                profile.information = information

                                return@flatMap synchronizationServerController.postUserRoleConsentResult(profile).map { success ->
                                    if (success == true) {
                                        authManager.setIsConsentApproved(true)
                                        return@map true
                                    } else {
                                        throw Exception("postUserConsentRole failed")
                                    }
                                }
                            } else {
                                return@flatMap Single.just(false)
                            }
                        }
            }
        }
    }

}