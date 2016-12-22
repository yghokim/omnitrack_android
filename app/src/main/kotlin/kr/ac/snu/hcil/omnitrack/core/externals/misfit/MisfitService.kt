package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import android.content.Context
import android.content.Intent
import android.support.v4.app.FragmentActivity
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * Created by Young-Ho on 9/1/2016.
 */
object MisfitService : OTExternalService("MisfitService", 0) {

    const val PREFERENCE_ACCESS_TOKEN = "misfit_access_token"

    init {
        _measureFactories.add(MisfitSleepMeasureFactory)
        _measureFactories.add(MisfitStepMeasureFactory)
        assignRequestCode(this)
    }

    fun getStoredAccessToken(): String? {
        if (preferences.contains(PREFERENCE_ACCESS_TOKEN)) {
            return preferences.getString(PREFERENCE_ACCESS_TOKEN, "")
        } else return null
    }

    override fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)?) {
        MisfitApi.authorize(context as FragmentActivity)
    }

    override fun onDeactivate() {
    }

    override val thumbResourceId: Int = R.drawable.service_thumb_misfit

    override fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?) {
        println("stored Misfit Token: ${getStoredAccessToken()}")
        if (getStoredAccessToken() != null) {
            preparedHandler?.invoke(true)
        } else {
            preparedHandler?.invoke(false)
        }
    }

    override fun handleActivityActivationResultOk(resultData: Intent?) {

        val code = resultData?.getStringExtra(AuthConstants.PARAM_CODE)
        if (code != null) {
            MisfitApi.getTokenExchangeRequest(code).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        token ->
                        println("my access token $token")
                        preferences.edit().putString(PREFERENCE_ACCESS_TOKEN, token).apply()
                        pendingConnectionListener?.invoke(true)
                    }, {
                        exception ->
                        cancelActivationProcess()
                    })
        }
    }

    override val nameResourceId: Int = R.string.service_misfit_name
    override val descResourceId: Int = R.string.service_misfit_desc
}