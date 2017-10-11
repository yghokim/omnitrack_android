package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import com.github.ybq.android.spinkit.SpinKitView
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioRecorderView
import rx.subscriptions.CompositeSubscription

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class AudioRecordInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<SynchronizedUri>(R.layout.input_audio_record, context, attrs) {
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

    override var value: SynchronizedUri? = SynchronizedUri()
        set(value) {
            if (field != value) {
                subscriptions.clear()
                field = value
                if (value?.isLocalUriValid == true) {
                    valueView.audioFileUriChanged.suspend = true
                    valueView.audioFileUri = value.localUri
                    valueView.audioFileUriChanged.suspend = false
                } else if (value?.isSynchronized == true) {
                    inLoadingMode = true
                    subscriptions.add(
                            OTApplication.app.storageHelper.downloadFileTo(value.serverUri.path, value.localUri).subscribe(
                                    {
                                        uri ->
                                        valueView.audioFileUriChanged.suspend = true
                                        valueView.audioFileUri = uri
                                        valueView.audioFileUriChanged.suspend = false
                                        inLoadingMode = false
                                    }, {
                                error ->
                                error?.printStackTrace()
                                valueView.audioFileUriChanged.suspend = true
                                valueView.audioFileUri = Uri.EMPTY
                                valueView.audioFileUriChanged.suspend = false
                                inLoadingMode = false
                            }
                            )
                    )
                } else {
                    //null
                    valueView.audioFileUriChanged.suspend = true
                    valueView.audioFileUri = Uri.EMPTY
                    valueView.audioFileUriChanged.suspend = false
                    inLoadingMode = false
                }
            }
        }

    val valueView: AudioRecorderView = findViewById(R.id.ui_audio_recorder)
    val loadingIndicator: SpinKitView = findViewById(R.id.ui_loading_indicator)

    private var audioTitleInformation: String = ""

    private var subscriptions = CompositeSubscription()

    init {

        valueView.audioFileUriChanged += {
            sender, uri ->
            this.value = SynchronizedUri(uri)
            this.onValueChanged(value)
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        subscriptions.clear()
    }

    override fun onAttributeBound(attributeObjectId: String) {
        valueView.mediaSessionId = attributeObjectId

        val realm = OTApplication.app.databaseManager.getRealmInstance()
        val attributeInfo = realm.where(OTAttributeDAO::class.java).equalTo("objectId", attributeObjectId).findFirst()
        if (attributeInfo != null) {
            val trackerName = attributeInfo.trackerId?.let {
                val trackerInfo = OTApplication.app.databaseManager.getTrackerQueryWithId(it, realm).findFirst()
                trackerInfo?.name ?: "No Tracker"
            } ?: "No Tracker"

            valueView.audioTitle = "${attributeInfo.name} | ${trackerName}"
        }
    }

    override fun onPause() {
        super.onPause()
        valueView.dispose()
    }
}