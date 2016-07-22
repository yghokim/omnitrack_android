package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
abstract class APropertyView<T>(layoutId: Int, context: Context, attrs: AttributeSet?) : AInputView<T>(layoutId, context, attrs) {

    protected lateinit var titleView: TextView

    var title: CharSequence
        get() = titleView.text
        set(value) {
            titleView.text = value
        }

    constructor(layoutId: Int, context: Context) : this(layoutId, context, null)

    init {

        titleView = findViewById(R.id.title) as TextView

    }
}