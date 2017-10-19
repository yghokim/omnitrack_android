package kr.ac.snu.hcil.omnitrack.core

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTUserRolePOJO
import kr.ac.snu.hcil.omnitrack.ui.pages.experiment.ExperimentSignInActivity

/**
 * Created by Young-Ho Kim on 2017-01-31.
 */
object ExperimentConsentManager {

    const val REQUEST_CODE_EXPERIMENT_SIGN_IN = 6550

    interface ResultListener {
        fun onConsentApproved()
        fun onConsentFailed()
        fun onConsentDenied()
    }

    private var mActivity: AppCompatActivity? = null
    private var mResultListener: ResultListener? = null


    fun startProcess(activity: AppCompatActivity, userId: String, resultListener: ResultListener? = null) {
        mActivity = activity
        mResultListener = resultListener

        if (OTApp.instance.systemSharedPreferences.getBoolean(OTUser.PREFERENCES_KEY_CONSENT_APPROVED, false)) {
            mResultListener?.onConsentApproved()
            finishProcess()
        } else {

            OTApp.instance.synchronizationServerController.getUserRoles().subscribe({ result ->
                val userRole = result.find { it.role == "ServiceUser" }
                if (userRole == null || !userRole.isConsentApproved) {
                    println("consent form was not approved or is first-time login.")
                    activity.startActivityForResult(Intent(activity, ExperimentSignInActivity::class.java), REQUEST_CODE_EXPERIMENT_SIGN_IN)
                } else {
                    println("consent form has been approved.")
                    onApproved()
                    finishProcess()
                }
            }, { error ->
                error.printStackTrace()
                mResultListener?.onConsentFailed()
                finishProcess()
            })
        }

        /*

        DatabaseManager.experimentProfileRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
                println("Db Error: ${p0?.message}")
                mResultListener?.onConsentFailed()
                finishProcess()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue(ExperimentProfile::class.java)
                if (profile == null || !profile.isConsentApproved) {
                    println("consent form was not approved or is first-time login.")
                    activity.startActivityForResult(Intent(activity, ExperimentSignInActivity::class.java), REQUEST_CODE_EXPERIMENT_SIGN_IN)
                } else {
                    println("consent form has been approved.")
                    println(profile)
                    mResultListener?.onConsentApproved()
                    finishProcess()
                }
            }

        })*/
    }

    private fun onApproved() {
        OTApp.instance.systemSharedPreferences.edit().putBoolean(OTUser.PREFERENCES_KEY_CONSENT_APPROVED, true).apply()
        mResultListener?.onConsentApproved()
    }

    fun handleActivityResult(deleteAccountIfDenied: Boolean, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_EXPERIMENT_SIGN_IN) {
            if (resultCode != AppCompatActivity.RESULT_OK || data == null) {
                if (deleteAccountIfDenied) {
                    //DatabaseManager.experimentProfileRef?.removeValue()

                    OTAuthManager.deleteUser(object : OTAuthManager.SignInResultsHandler {
                        override fun onCancel() {
                        }

                        override fun onError(e: Throwable) {

                        }

                        override fun onSuccess() {

                        }

                    })
                }

                mResultListener?.onConsentFailed()
                finishProcess()
            } else {
                val profile = OTUserRolePOJO()
                profile.role = "ServiceUser"
                profile.isConsentApproved = true
                val information = HashMap<String, Any>()
                information["age"] = data.getStringExtra(OTApp.ACCOUNT_DATASET_EXPERIMENT_KEY_AGE_GROUP)
                information["country"] = data.getStringExtra(OTApp.ACCOUNT_DATASET_EXPERIMENT_KEY_COUNTRY)
                information["gender"] = data.getStringExtra(OTApp.ACCOUNT_DATASET_EXPERIMENT_KEY_GENDER)
                information["purposes"] = data.getStringArrayListExtra(OTApp.ACCOUNT_DATASET_EXPERIMENT_KEY_PURPOSES)
                profile.information = information

                OTApp.instance.synchronizationServerController.postUserRoleConsentResult(profile)
                        .subscribe({ success ->
                            if (success == true) {
                                onApproved()
                                finishProcess()
                            } else {
                                mResultListener?.onConsentFailed()
                                finishProcess()
                            }
                        }, { error ->
                            error.printStackTrace()
                            mResultListener?.onConsentFailed()
                            finishProcess()
                        })

                /*
                val currentExpRef = DatabaseManager.experimentProfileRef
                currentExpRef?.setValue(profile, { databaseError, _ ->
                    if (databaseError == null) {
                        mResultListener?.onConsentApproved()
                        finishProcess()
                    } else {
                        mResultListener?.onConsentFailed()
                        finishProcess()
                    }
                })*/
            }
        }
    }

    fun finishProcess() {
        mActivity = null
        mResultListener = null
    }
}