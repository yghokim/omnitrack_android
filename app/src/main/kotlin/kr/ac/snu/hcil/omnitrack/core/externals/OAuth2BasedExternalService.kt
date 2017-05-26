package kr.ac.snu.hcil.omnitrack.core.externals

import android.app.Activity
import android.content.Context
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.dependency.OAuth2LoginDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import kr.ac.snu.hcil.omnitrack.utils.Result
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import rx.Observable

/**
 * Created by Young-Ho Kim on 16. 9. 3
 */
abstract class OAuth2BasedExternalService(identifier: String, minimumSDK: Int) : OTExternalService(identifier, minimumSDK), OAuth2Client.OAuth2CredentialRefreshedListener {

    protected val authClient: OAuth2Client by lazy {
        makeNewAuth2Client()
    }

    var credential: OAuth2Client.Credential?
        get() = OAuth2Client.Credential.restore(preferences, identifier)
        set(value) {
            if (value != null)
                value.store(preferences, identifier)
            else {
                credential?.remove(preferences, identifier)
            }
        }

    override fun onRegisterDependencies(): Array<OTSystemDependencyResolver> {
        return super.onRegisterDependencies() + arrayOf(
                OAuth2LoginDependencyResolver(authClient, identifier, preferences, OTApplication.app.getString(nameResourceId))

        )
    }

    abstract fun makeNewAuth2Client(): OAuth2Client

    override fun onActivateAsync(context: Context): Observable<Boolean> {
        return authClient.authorize(context as Activity, OTApplication.getString(nameResourceId))
                .doOnNext { credential ->
                    credential.store(preferences, identifier)
                }
                .onErrorReturn { error -> null }
                .map {
                    credential ->
                    credential != null
                }
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

    override fun onCredentialRefreshed(newCredential: OAuth2Client.Credential) {
        println("store refreshed credential")
        credential = newCredential
    }

    fun <T> getRequest(converter: OAuth2Client.OAuth2RequestConverter<T>, vararg requestUrls: String): Observable<Result<T>> {
        val credential = credential
        return if (credential != null) {
            authClient.getRequest(credential, converter, this, *requestUrls)
        } else Observable.error(Exception("Auth Credential is not stored."))
    }

}