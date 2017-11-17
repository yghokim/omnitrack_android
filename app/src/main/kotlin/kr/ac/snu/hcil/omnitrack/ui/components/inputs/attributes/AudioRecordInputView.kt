package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import com.github.ybq.android.spinkit.SpinKitView
import dagger.Lazy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.OTServerFile
import kr.ac.snu.hcil.omnitrack.core.net.OTLocalMediaCacheManager
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioRecorderView
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class AudioRecordInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<OTServerFile>(R.layout.input_audio_record, context, attrs) {

    @Inject
    lateinit var localCacheManager: Lazy<OTLocalMediaCacheManager>

    @Inject
    lateinit var dbManager: Lazy<RealmDatabaseManager>

    @Inject
    lateinit var realmProvider: Provider<Realm>

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
                            localCacheManager.get().getCachedUri(value.serverPath).observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
                                context.runOnUiThread {
                                    inLoadingMode = true
                                }
                            }.subscribe({ (refreshed, localUri) ->
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

        (context.applicationContext as OTApp).applicationComponent.inject(this)

        valueView.audioFileUriChanged += {
            sender, uri ->
            println("picker uri changed to $uri")
            if (uri == Uri.EMPTY) {
                value = null
            } else if (!uri.equals(value?.let { localCacheManager.get().getCachedUriImmediately(it.serverPath) } ?: Uri.EMPTY)) {
                val newServerPath = localCacheManager.get().generateRandomServerPath(uri)
                subscriptions.add(
                        localCacheManager.get().insertOrUpdateNewLocalMedia(uri, newServerPath).subscribe { serverUri ->
                            value = OTServerFile.fromLocalFile(serverUri, uri, context)
                        })
            }
        }
        /*
        valueView.fileRemoved += {
            sender, time ->
            this.onValueChanged(SynchronizedUri(valueView.audioFileUri))
        }

        valueView.recordingComplete += {
            sender, length ->
            this.onValueChanged(SynchronizedUri(valueView.audioFileUri))
        }*/
    }

    override fun focus() {

    }

    override fun forceApplyValueAsync(): Single<Nullable<out Any>> {
        return valueView.stopRecordingAndApplyUri() as Single<Nullable<out Any>>
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loadingSubscription.set(null)
        subscriptions.clear()
    }

    override fun onAttributeBound(attributeObjectId: String) {
        valueView.mediaSessionId = attributeObjectId

        val realm = realmProvider.get()
        val attributeInfo = realm.where(OTAttributeDAO::class.java).equalTo("objectId", attributeObjectId).findFirst()
        if (attributeInfo != null) {
            val trackerName = attributeInfo.trackerId?.let {
                val trackerInfo = dbManager.get().getTrackerQueryWithId(it, realm).findFirst()
                trackerInfo?.name ?: "No Tracker"
            } ?: "No Tracker"

            valueView.audioTitle = "${attributeInfo.name} | ${trackerName}"
        }
        realm.close()
    }

    override fun onPause() {
        super.onPause()
        valueView.dispose()
    }
}