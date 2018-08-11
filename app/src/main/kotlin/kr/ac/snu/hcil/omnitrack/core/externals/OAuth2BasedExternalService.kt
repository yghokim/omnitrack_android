package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.dependency.OAuth2LoginDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client

/**
 * Created by Young-Ho Kim on 16. 9. 3
 */
abstract class OAuth2BasedExternalService(context: Context, identifier: String, minimumSDK: Int) : OTExternalService(context, identifier, minimumSDK), OAuth2Client.OAuth2CredentialRefreshedListener {

    protected val authClient: OAuth2Client by lazy {
        makeNewAuth2Client()
    }

    var credential: OAuth2Client.Credential?
        get() = OAuth2Client.Credential.restore(externalServiceManager.get().preferences, identifier)
        set(value) {
            if (value != null)
                value.store(externalServiceManager.get().preferences, identifier)
            else {
                credential?.remove(externalServiceManager.get().preferences, identifier)
            }
        }

    override fun onRegisterDependencies(): Array<OTSystemDependencyResolver> {
        return super.onRegisterDependencies() + arrayOf(
                OAuth2LoginDependencyResolver(authClient, identifier, externalServiceManager.get().preferences, context.resources.getString(nameResourceId))

        )
    }

    abstract fun makeNewAuth2Client(): OAuth2Client

    override fun onDeactivate() {
        val cd = credential
        if (cd != null) {
            authClient.signOut(cd) {
                result ->
                if (result) {
                    println("${identifier} OAuth2 signed out.")
                } else {
                    println("${identifier} OAuth2 sign out failed.")
                }
            }
            credential = null
        }
    }

    override fun onCredentialRefreshed(newCredential: OAuth2Client.Credential) {
        println("store refreshed credential")
        credential = newCredential
    }

    fun <T> getRequest(converter: OAuth2Client.OAuth2RequestConverter<T>, vararg requestUrls: String): Single<Nullable<T>> {
        val credential = credential
        return if (credential != null) {
            authClient.getRequest(credential, converter, this, *requestUrls)
        } else Single.error(Exception("Auth Credential is not stored."))
    }

}