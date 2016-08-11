package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.text.Html
import android.text.Spanned
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTConnection
import kr.ac.snu.hcil.omnitrack.utils.events.Event

/**
 * Created by younghokim on 16. 8. 11..
 */
class AttributeConnectionView : LinearLayout, View.OnClickListener {

    val onRemoveButtonClicked = Event<Any>()

    var connection: OTConnection? = null
        set(value) {
            if (field != value) {
                field = value
                refresh()
            }
        }

    private lateinit var sourceView: TextView

    private lateinit var removeButton: Button

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        this.orientation = VERTICAL

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_attribute_connection, this, true)

        sourceView = findViewById(R.id.ui_source) as TextView

        removeButton = findViewById(R.id.ui_button_remove) as Button
        removeButton.setOnClickListener(this)

    }


    override fun onClick(p0: View?) {
        onRemoveButtonClicked.invoke(this, true)
    }

    fun refresh() {

        val source = connection?.source
        if (source == null) {
            sourceView.text = "No source"
        } else {
            val html = "<b>${resources.getString(source.factory.nameResourceId)}</b> | ${resources.getString(source.factory.service.nameResourceId)}"
            val content: Spanned = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(html)
            }

            sourceView.text = content
        }
    }

}