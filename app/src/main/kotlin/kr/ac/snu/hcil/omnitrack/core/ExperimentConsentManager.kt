package kr.ac.snu.hcil.omnitrack.core

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.amazonaws.mobile.AWSConfiguration
import com.amazonaws.mobile.AWSMobileClient
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager
import com.amazonaws.mobileconnectors.cognito.Dataset
import com.amazonaws.mobileconnectors.cognito.Record
import com.amazonaws.mobileconnectors.cognito.SyncConflict
import com.amazonaws.mobileconnectors.cognito.exceptions.DataStorageException
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler
import kr.ac.snu.hcil.omnitrack.OTApplication
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
    private var mSyncManager: CognitoSyncManager? = null
    private var mResultListener: ResultListener? = null

    private var experimentDataset: Dataset? = null

    fun startProcess(activity: AppCompatActivity, syncManager: CognitoSyncManager, resultListener: ResultListener? = null) {
        mActivity = activity
        mSyncManager = syncManager
        mResultListener = resultListener

        experimentDataset = syncManager.openOrCreateDataset(OTApplication.ACCOUNT_DATASET_EXPERIMENT)
        if (experimentDataset != null) {
            experimentDataset?.synchronize(object : Dataset.SyncCallback {
                override fun onDatasetDeleted(dataset: Dataset?, datasetName: String?): Boolean {
                    return false
                }

                override fun onConflict(dataset: Dataset?, conflicts: MutableList<SyncConflict>?): Boolean {
                    return false
                }

                override fun onSuccess(dataset: Dataset?, updatedRecords: MutableList<Record>?) {
                    val isConsentApproved = experimentDataset?.get(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_IS_CONSENT_APPROVED)
                    Log.v("OmniTrack", "IsConsent Approved : ${isConsentApproved}")
                    if (isConsentApproved == null || isConsentApproved.toBoolean() != true) {
                        println("consent form was not approved or is first-time login.")
                        activity.startActivityForResult(Intent(activity, ExperimentSignInActivity::class.java), REQUEST_CODE_EXPERIMENT_SIGN_IN)
                    } else {
                        println("consent form has been approved.")
                        println(experimentDataset?.get(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_GENDER))
                        println(experimentDataset?.get(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_AGE_GROUP))
                        println(experimentDataset?.get(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_OCCUPATION))
                        println(experimentDataset?.get(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_COUNTRY))

                        mResultListener?.onConsentApproved()
                        finishProcess()
                    }
                }

                override fun onFailure(dse: DataStorageException?) {
                    mResultListener?.onConsentFailed()
                    finishProcess()
                }

                override fun onDatasetsMerged(dataset: Dataset?, datasetNames: MutableList<String>?): Boolean {
                    return false
                }

            })

        } else {

        }
    }

    fun handleActivityResult(deleteAccountIfDenied: Boolean, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_EXPERIMENT_SIGN_IN) {
            if (resultCode != AppCompatActivity.RESULT_OK || data == null) {
                //TODO: delete user if possible.
                if (deleteAccountIfDenied) {
                    val userPool = CognitoUserPool(mActivity, AWSConfiguration.AMAZON_COGNITO_USER_POOL_ID, AWSConfiguration.AMAZON_COGNITO_USER_POOL_CLIENT_ID, AWSConfiguration.AMAZON_COGNITO_USER_POOL_CLIENT_SECRET)
                    val user = userPool.getUser(AWSMobileClient.defaultMobileClient().identityManager.underlyingProvider.cachedIdentityId)
                    user.deleteUserInBackground(object : GenericHandler {
                        override fun onSuccess() {
                            Log.v("OMNITRACK", "removed user.")
                            mResultListener?.onConsentDenied()
                            finishProcess()
                        }

                        override fun onFailure(exception: Exception) {
                            Log.v("OMNITRACK", "removal failed.")
                            exception.printStackTrace()
                            mResultListener?.onConsentDenied()
                            finishProcess()
                        }

                    })
                }
                AWSMobileClient.defaultMobileClient().identityManager.signOut()
            } else {
                experimentDataset?.put(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_IS_CONSENT_APPROVED, true.toString())
                arrayOf(
                        OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_GENDER,
                        OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_AGE_GROUP,
                        OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_OCCUPATION,
                        OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_COUNTRY)
                        .forEach {
                            experimentDataset?.put(it, data.getStringExtra(it))
                        }
                experimentDataset?.put(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_EMAIL, AWSMobileClient.defaultMobileClient().identityManager.userEmail)

                experimentDataset?.synchronize(object : Dataset.SyncCallback {
                    override fun onDatasetDeleted(dataset: Dataset, datasetName: String?): Boolean {
                        return true
                    }

                    override fun onConflict(dataset: Dataset, conflicts: MutableList<SyncConflict>?): Boolean {
                        return true
                    }

                    override fun onSuccess(dataset: Dataset, updatedRecords: MutableList<Record>?) {
                        mResultListener?.onConsentApproved()
                        finishProcess()
                    }

                    override fun onFailure(dse: DataStorageException?) {
                    }

                    override fun onDatasetsMerged(dataset: Dataset, datasetNames: MutableList<String>?): Boolean {
                        return true
                    }

                })
            }
        }
    }

    fun finishProcess() {
        mActivity = null
        mSyncManager = null
        mResultListener = null
        experimentDataset = null
    }
}