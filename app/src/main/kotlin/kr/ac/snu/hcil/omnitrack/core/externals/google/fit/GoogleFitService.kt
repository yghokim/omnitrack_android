package kr.ac.snu.hcil.omnitrack.core.externals.google.fit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 8. 8
 */
object GoogleFitService : OTExternalService("GoogleFitService", 19) {

    override val nameResourceId: Int = R.string.service_googlefit_name
    override val descResourceId: Int = R.string.service_googlefit_desc
    override val thumbResourceId: Int = R.drawable.service_thumb_googlefit

    private var client: GoogleApiClient? = null

    val usedApis: Array<Api<out Api.ApiOptions.NotRequiredOptions>> by lazy {

        val set = HashSet<Api<out Api.ApiOptions.NotRequiredOptions>>()
        for (m in _measureFactories) {
            if (m is GoogleFitMeasureFactory) {
                set.add(m.usedAPI)
            }
        }

        set.toTypedArray()
    }

    val usedScopes: Array<Scope> by lazy {

        val set = HashSet<Scope>()
        for (m in _measureFactories) {
            if (m is GoogleFitMeasureFactory) {
                set.add(m.usedScope)
            }
        }

        set.toTypedArray()
    }


    //===================================================================================================
    private var currentActivationHandler: ((Boolean) -> Unit)? = null

    private var currentPreparationHandler: ((Boolean) -> Unit)? = null


    private val activationConnectionCallbacks = object : GoogleApiClient.ConnectionCallbacks {
        override fun onConnectionSuspended(reason: Int) {
            println("Google Fit activation connection is pending.. - $reason")
        }

        override fun onConnected(reason: Bundle?) {
            println("activation success.")
            currentActivationHandler?.invoke(true)
        }
    }

    private val preparationConnectionCallbacks = object : GoogleApiClient.ConnectionCallbacks {
        override fun onConnectionSuspended(reason: Int) {
            println("Google Fit preparation connection is pending.. - $reason")
        }

        override fun onConnected(reason: Bundle?) {

            println("preparation success.")
            currentPreparationHandler?.invoke(true)

        }
    }

    private val preparationConnectionFailedListener = GoogleApiClient.OnConnectionFailedListener {
        result ->
        println("Google fit preparation connection failed - ${result.toString()}")
        println(result.errorMessage)
        currentPreparationHandler?.invoke(false)
    }

    //==================================================================================================


    init {
        _measureFactories.add(GoogleFitStepsFactory)
        assignRequestCode(this)
    }

    override fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)?) {
        println("activate Google Fit...")
        if (client?.isConnected == true) {
            connectedHandler?.invoke(true)
        } else {
            currentActivationHandler = connectedHandler
            client = buildClientBuilderBase(context)
                    .addConnectionCallbacks(activationConnectionCallbacks)
                    .addOnConnectionFailedListener {
                        result ->
                        println("connectionFailed - ${result.errorCode}, ${result.errorMessage}")
                        if (result.hasResolution()) {
                            result.startResolutionForResult(context as Activity, requestCodeDict[this])
                        }
                    }
                    .build()
            client?.connect()
        }
    }

    override fun onDeactivate() {
        client?.disconnect()
        client = null
    }

    override fun handleActivityActivationResultOk(resultData: Intent?) {
        prepareServiceAsync(currentActivationHandler)
    }

    override fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?) {
        if (client?.isConnected == true) {

            preparedHandler?.invoke(true)
        } else {
            currentPreparationHandler = preparedHandler
            client = buildClientBuilderBase()
                    .addConnectionCallbacks(preparationConnectionCallbacks)
                    .addOnConnectionFailedListener(preparationConnectionFailedListener)
                    .build()
            client?.connect()
        }
    }

    fun getClientAsync(handler: (client: GoogleApiClient?) -> Unit) {
        if (client != null) {
            handler.invoke(client)
        } else {
            prepareServiceAsync {
                success ->
                if (success == true) {
                    handler.invoke(client)
                } else {
                    handler.invoke(null)
                }
            }
        }
    }

    private fun buildClientBuilderBase(context: Context = OTApplication.app): GoogleApiClient.Builder {
        val builder = GoogleApiClient.Builder(context)

        for (api in usedApis) {
            builder.addApi(api)
        }

        for (scope in usedScopes) {
            builder.addScope(scope)
        }
        return builder
    }

    abstract class GoogleFitMeasureFactory : OTMeasureFactory() {
        abstract val usedAPI: Api<out Api.ApiOptions.NotRequiredOptions>
        abstract val usedScope: Scope
    }
}