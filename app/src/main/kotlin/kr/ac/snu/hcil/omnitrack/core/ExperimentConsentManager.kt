package kr.ac.snu.hcil.omnitrack.core

import android.content.Intent
import android.support.v7.app.AppCompatActivity

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


    fun startProcess(activity: AppCompatActivity, resultListener: ResultListener? = null) {
        mActivity = activity
        mResultListener = resultListener

        /*
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
                    dse?.printStackTrace()
                    println(dse?.message)
                    println(dse?.cause?.message)
                    mResultListener?.onConsentFailed()
                    finishProcess()
                }

                override fun onDatasetsMerged(dataset: Dataset?, datasetNames: MutableList<String>?): Boolean {
                    return false
                }

            })

        } else {

        }
        */
    }

    fun handleActivityResult(deleteAccountIfDenied: Boolean, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_EXPERIMENT_SIGN_IN) {
            if (resultCode != AppCompatActivity.RESULT_OK || data == null) {
                //TODO: delete user if possible.
                if (deleteAccountIfDenied) {

                }
                //TODO: Sign out
            } else {
                /*
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

                })*/
            }
        }
    }

    fun finishProcess() {
        mActivity = null
        mResultListener = null
    }
}