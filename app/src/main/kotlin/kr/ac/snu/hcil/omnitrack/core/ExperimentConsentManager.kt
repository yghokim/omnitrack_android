package kr.ac.snu.hcil.omnitrack.core

import android.content.Intent
import android.support.annotation.Keep
import android.support.v7.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseHelper
import kr.ac.snu.hcil.omnitrack.ui.pages.experiment.ExperimentSignInActivity

/**
 * Created by Young-Ho Kim on 2017-01-31.
 */
object ExperimentConsentManager {

    const val REQUEST_CODE_EXPERIMENT_SIGN_IN = 6550

    @Keep
    data class ExperimentProfile(
            var isConsentApproved: Boolean = false,
            var age: String? = null,
            var gender: String? = null,
            var occupation: String? = null,
            var country: String? = null) {

        override fun toString(): String {
            return "isConsentApproved: ${isConsentApproved}, age: ${age}, gender: ${gender}, occupation: ${occupation}, country: ${country}"
        }
    }

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

        FirebaseHelper.experimentProfileRef?.addListenerForSingleValueEvent(object : ValueEventListener {
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

        })
    }

    fun handleActivityResult(deleteAccountIfDenied: Boolean, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_EXPERIMENT_SIGN_IN) {
            if (resultCode != AppCompatActivity.RESULT_OK || data == null) {
                if (deleteAccountIfDenied) {
                    //FirebaseHelper.experimentProfileRef?.removeValue()

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
                val profile = ExperimentProfile()
                profile.isConsentApproved = true
                profile.age = data.getStringExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_AGE_GROUP)
                profile.country = data.getStringExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_COUNTRY)
                profile.gender = data.getStringExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_GENDER)
                profile.occupation = data.getStringExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_OCCUPATION)

                val currentExpRef = FirebaseHelper.experimentProfileRef
                currentExpRef?.setValue(profile, DatabaseReference.CompletionListener { databaseError, databaseReference ->
                    if (databaseError == null) {
                        mResultListener?.onConsentApproved()
                        finishProcess()
                    } else {
                        mResultListener?.onConsentFailed()
                        finishProcess()
                    }
                })
            }
        }
    }

    fun finishProcess() {
        mActivity = null
        mResultListener = null
    }
}