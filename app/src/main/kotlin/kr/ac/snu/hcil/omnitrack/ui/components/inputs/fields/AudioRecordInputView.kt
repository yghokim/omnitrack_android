package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import com.github.ybq.android.spinkit.SpinKitView
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import io.realm.Realm
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.core.net.OTLocalMediaCacheManager
import kr.ac.snu.hcil.omnitrack.core.types.OTServerFile
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioRecorderView
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class AudioRecordInputView(context: Context, attrs: AttributeSet? = null) : AFieldInputView<OTServerFile>(R.layout.input_audio_record, context, attrs) {

    @Inject
    lateinit var localCacheManager: Lazy<OTLocalMediaCacheManager>

    @Inject
    lateinit var dbManager: Lazy<BackendDbManager>

    @field:[Inject Backend]
    lateinit var realmProvider: Factory<Realm>

    override val typeId: Int = VIEW_TYPE_AUDIO_RECORD

    private var inLoadingMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    loadingIndicator.visibility = View.VISIBLE
                    valueView.visibility = View.INVISIBLE
                    locked = true
                } else {
                    valueView.visibility = View.VISIBLE
                    loadingIndicator.visibility = View.GONE
                    locked = false
                }
            }
        }

    override var value: OTServerFile? = null
        set(rawValue) {
            val value = if (rawValue?.serverPath?.isBlank() == true) null else rawValue
            if (field != value) {
                field = value
                if (value == null) {
                    valueView.audioFileUriChanged.suspend = true
                    valueView.audioFileUri = Uri.EMPTY
                    valueView.audioFileUriChanged.suspend = false
                } else {
                    loadingSubscription.set(
                            localCacheManager.get().getCachedUri(value).observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
                                context.runOnUiThread {
                                    inLoadingMode = true
                                }
                            }.subscribe({ (_, localUri) ->
                                valueView.audioFileUriChanged.suspend = true
                                valueView.audioFileUri = localUri
                                valueView.audioFileUriChanged.suspend = false
                                inLoadingMode = false
                            }, { error ->
                                error?.printStackTrace()
                                valueView.audioFileUriChanged.suspend = true
                                valueView.audioFileUri = Uri.EMPTY
                                valueView.audioFileUriChanged.suspend = false
                                inLoadingMode = false
                            })
                    )
                }

                onValueChanged(value)
            }
        }

    val valueView: AudioRecorderView = findViewById(R.id.ui_audio_recorder)
    val loadingIndicator: SpinKitView = findViewById(R.id.ui_loading_indicator)

    private var audioTitleInformation: String = ""

    private val loadingSubscription = SerialDisposable()
    private val subscriptions = CompositeDisposable()

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)

        valueView.audioFileUriChanged += { _, uri ->
            println("picker uri changed to $uri")
            if (uri == Uri.EMPTY) {
                value = null
            } else if (uri != value?.let { localCacheManager.get().getCachedUriImmediately(it.serverPath) } ?: Uri.EMPTY) {
                val newServerPath = localCacheManager.get().generateRandomServerPath(uri)
                val newServerFile = OTServerFile.fromLocalFile(newServerPath, uri, context)
                subscriptions.add(
                        localCacheManager.get().insertOrUpdateNewLocalMedia(uri, newServerFile).subscribe { serverFile ->
                            value = serverFile
                        })
            }
        }
    }

    private fun convertNewUriToServerFile(uri: Uri): Single<Nullable<OTServerFile>> {
        if (uri == Uri.EMPTY) {
            return Single.just(Nullable<OTServerFile>(null))
        } else if (uri != value?.let { localCacheManager.get().getCachedUriImmediately(it.serverPath) } ?: Uri.EMPTY) {
            val newServerPath = localCacheManager.get().generateRandomServerPath(uri)
            val newServerFile = OTServerFile.fromLocalFile(newServerPath, uri, context)
            return localCacheManager.get().insertOrUpdateNewLocalMedia(uri, newServerFile).map { _ ->
                Nullable(newServerFile)
            }.onErrorReturn { err -> err.printStackTrace(); Nullable() }
        } else return Single.just(Nullable(value))
    }

    override fun focus() {

    }

    override fun forceApplyValueAsync(): Single<Nullable<out Any>> {
        @Suppress("UNCHECKED_CAST")
        return valueView.stopRecordingAndApplyUri().flatMap { (uri) -> convertNewUriToServerFile(uri ?: Uri.EMPTY) } as Single<Nullable<out Any>>
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loadingSubscription.set(null)
        subscriptions.clear()
    }

    override fun onAttributeBound(attributeObjectId: String) {
        valueView.mediaSessionId = attributeObjectId

        val realm = realmProvider.get()
        val attributeInfo = realm.where(OTFieldDAO::class.java).equalTo("_id", attributeObjectId).findFirst()
        if (attributeInfo != null) {
            val trackerName = attributeInfo.trackerId?.let {
                val trackerInfo = dbManager.get().getTrackerQueryWithId(it, realm).findFirst()
                trackerInfo?.name ?: "No Tracker"
            } ?: "No Tracker"

            valueView.audioTitle = "${attributeInfo.name} | $trackerName"
        }
        realm.close()
    }

    override fun onPause() {
        super.onPause()
        valueView.dispose()
    }
}