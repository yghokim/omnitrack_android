package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import android.content.SharedPreferences
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.net.OAuth2Client
import kr.ac.snu.hcil.omnitrack.core.dependency.OAuth2LoginDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver

/**
 * Created by Young-Ho Kim on 16. 9. 3
 */
abstract class OAuth2BasedExternalService(context: Context, preferences: SharedPreferences, identifier: String, minimumSDK: Int) : OTExternalService(context, preferences, identifier, minimumSDK), OAuth2Client.OAuth2CredentialRefreshedListener {

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
                OAuth2LoginDependencyResolver(authClient, identifier, preferences, context.resources.getString(nameResourceId))

        )
    }

    abstract fun makeNewAuth2Client(): OAuth2Client

    override fun onDeactivate(): Completable {
        return Completable.defer {
            val cd = credential
            if (cd != null) {
                return@defer authClient.signOut(cd).doOnError {
                    println("$identifier OAuth2 sign out failed.")
                }.doOnComplete {
                    println("$identifier OAuth2 signed out.")
                }.doOnTerminate {
                    credential = null
                }.subscribeOn(Schedulers.io())
            } else return@defer Completable.complete()
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