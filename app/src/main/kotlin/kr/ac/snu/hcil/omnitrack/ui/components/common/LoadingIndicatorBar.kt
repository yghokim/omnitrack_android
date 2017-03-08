package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by younghokim on 2017. 3. 8..
 */
class LoadingIndicatorBar : FrameLayout {

    private val progressBar: ProgressBar
    private val messageView: TextView

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        inflateContent(R.layout.component_loading_indicator_bar, true)
        progressBar = findViewById(R.id.ui_progress_bar) as ProgressBar
        messageView = findViewById(R.id.ui_message) as TextView

    }
}