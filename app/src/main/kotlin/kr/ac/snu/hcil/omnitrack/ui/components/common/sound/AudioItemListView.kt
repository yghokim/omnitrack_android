package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import rx.internal.util.SubscriptionList

/**
 * Created by Young-Ho on 4/23/2017.
 */
class AudioItemListView : ConstraintLayout {
    enum class Mode { Empty, Error, Mounted, Loading }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var mode: Mode = Mode.Empty
        set(value) {
            if (field != value) {
                onModeChanged(value, field)
            }
        }

    private val subscriptions = SubscriptionList()

    var mountedUri: SynchronizedUri = SynchronizedUri()
        set(value) {
            if (field != value) {
                field = value
                if (!mountedUri.isEmpty) {
                    if (mountedUri.isLocalUriValid) {
                        mode = Mode.Mounted
                    } else if (mountedUri.isSynchronized) {
                        mode = Mode.Loading
                        startDownload()
                    }
                } else {
                    mode = Mode.Empty
                }
            }
        }

    init {
        inflateContent(R.layout.component_audio_item_list_view, true)
    }

    private fun startDownload() {
        subscriptions.add(
                OTApplication.app.storageHelper.downloadFileTo(mountedUri.serverUri.toString(), mountedUri.localUri)
                        .subscribe({
                            uri ->
                            println("audio file download complete: ${uri.toString()}")
                            mode = Mode.Mounted
                        }, { mode = Mode.Error })
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        subscriptions.clear()
    }

    private fun onModeChanged(mode: Mode, oldMode: Mode) {

    }
}