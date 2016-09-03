package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import android.content.Intent
import android.support.v4.app.FragmentActivity
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client

/**
 * Created by younghokim on 16. 9. 3..
 */
abstract class OAuth2BasedExternalService(identifier: String, minimumSDK: Int) : OTExternalService(identifier, minimumSDK), OAuth2Client.OAuth2ResultListener {

    private val authClient: OAuth2Client by lazy {
        makeNewAuth2Client(OTExternalService.requestCodeDict[this])
    }

    var credential: OAuth2Client.Credential?
        get() = OAuth2Client.Credential.restore(preferences, identifier)
        set(value) {
            if (value != null)
                value?.store(preferences, identifier)
            else {
                credential?.remove(preferences, identifier)
            }
        }

    abstract fun makeNewAuth2Client(requestCode: Int): OAuth2Client

    override fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)?) {
        authClient.authorize(context as FragmentActivity, this)
    }

    override fun onDeactivate() {
        val cd = credential
        if (cd != null)
            authClient.signOut(cd) {
                result ->
                if (result) {
                    credential = null
                }
            }
    }

    override fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?) {
        val credential = credential
        if (credential != null) {
            //TODO check token expiration
            preparedHandler?.invoke(true)
        } else {
            preparedHandler?.invoke(false)
        }
    }

    override fun onFailed(error: String?) {
        println("OAuth2Service auth error: $error")
        pendingConnectionListener?.invoke(false)
    }

    override fun onSuccess(credential: OAuth2Client.Credential) {
        credential.store(preferences, identifier)
        pendingConnectionListener?.invoke(true)
    }

    override fun handleActivityActivationResultOk(resultData: Intent?) {
        if (resultData != null)
            authClient.handleLoginActivityResult(resultData)
    }

}