package kr.ac.snu.hcil.omnitrack.ui.components.properties

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.properties.APropertyView

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
class ShortTextPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<String>(R.layout.component_property_shorttext, context, attrs) {

    companion object{
         val NOT_EMPTY_VALIDATOR : ((String)->Boolean) = { it != "" }

    }

    override var value: String
        get() = valueView.text.toString()
        set(value) {
            valueView.text = value
        }

    lateinit private var valueView : TextView

    init{
        title = "Short Text Property"
        valueView = findViewById(R.id.value) as TextView
    }

    override fun focus() {
        valueView.requestFocus()
    }

    override fun onValidated(result: Boolean)
    {
        if(result == false)
        {
            valueView.error = validationErrorMessageList.joinToString("\n")
        }
    }

}