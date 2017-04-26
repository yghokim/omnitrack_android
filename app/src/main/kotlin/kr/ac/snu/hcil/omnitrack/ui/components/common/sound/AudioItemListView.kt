package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.View;
import android.widget.TextView
import com.github.ybq.android.spinkit.SpinKitView
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

    private val loadingIndicatorView: SpinKitView
    private val iconView: AppCompatImageView
    private val durationView: TextView
    private val unitView: TextView

    init {
        inflateContent(R.layout.component_audio_item_list_view, true)
        loadingIndicatorView = findViewById(R.id.ui_loading_indicator) as SpinKitView
        iconView = findViewById(R.id.icon) as AppCompatImageView
        durationView = findViewById(R.id.ui_duration_view) as TextView
        unitView = findViewById(R.id.ui_unit_text) as TextView
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
        when(mode)
        {
            Mode.Loading->
            {
                iconView.visibility = View.INVISIBLE
                loadingIndicatorView.visibility = View.VISIBLE
                unitView.setText("Synchronizing...")
                durationView.visibility = View.GONE
            }
            Mode.Mounted->
            {
                iconView.visibility = View.VISIBLE
                loadingIndicatorView.visibility = View.INVISIBLE
                iconView.setImageResource(R.drawable.icon_waveform)
                val metadata = AudioRecordMetadata.readMetadata(mountedUri.localUri.path)
                if(metadata != null) {
                    durationView.visibility = View.VISIBLE
                    durationView.text = ((metadata.durationMillis/10)/100f).toString()
                    unitView.text = "sec"
                }
                else{
                    durationView.visibility = View.INVISIBLE
                    unitView.text = "Audio"
                }
            }
            Mode.Error->
            {
                loadingIndicatorView.visibility = View.INVISIBLE
                iconView.visibility = View.VISIBLE
                iconView.setImageResource(R.drawable.error_dark)
                durationView.visibility = View.GONE
                unitView.text = "Data Error"
            }
            Mode.Empty->{
                loadingIndicatorView.visibility = View.INVISIBLE
                iconView.visibility = View.VISIBLE
                iconView.setImageResource(R.drawable.icon_waveform_unavailable)
                iconView.setImageResource(R.drawable.icon_waveform)
                durationView.visibility = View.GONE
                unitView.setText(R.string.msg_no_audio)
            }
        }
    }
}