package kr.ac.snu.hcil.omnitrack.ui.components.common.choice

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 3/11/2017.
 */
class ChoiceFormView : LinearLayout {

    data class Entry(val id: String, var text: String, var isCustom: Boolean = false) {
        val isValid: Boolean
            get() = if (isCustom) {
                text.isNotBlank()
            } else {
                true
            }
    }

    var allowMultipleSelection: Boolean by Delegates.observable(false) {
        prop, old, new ->
        if (old != new) {
            adapter.notifyDataSetChanged()
        }
    }

    var entries: Array<Entry>? by Delegates.observable(null as Array<Entry>?) {
        prop, old, new ->
        if (old != new) {
            selectedIndices.clear()
            adapter.notifyDataSetChanged()
        }
    }

    val isSelectionEmpty: Boolean get() = selectedIndices.isEmpty()

    private val selectedIndices = java.util.TreeSet<Int>()

    val valueChanged = Event<Void?>()

    val selectedEntries: Array<Entry> get() {
        return selectedIndices.map { entries?.get(it)!! }.toTypedArray()
    }

    private val recyclerView: RecyclerView

    private val adapter: Adapter = Adapter()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        orientation = LinearLayout.VERTICAL
        recyclerView = RecyclerView(context)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        recyclerView.preserveFocusAfterLayout = true
        recyclerView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        recyclerView.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter

        addView(recyclerView)

    }

    val VIEWHOLDER_TYPE_NORMAL = 0
    val VIEWHOLDER_TYPE_CUSTOM = 1

    private inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun getItemViewType(position: Int): Int {
            if (entries?.get(position)?.isCustom == true) {
                return VIEWHOLDER_TYPE_CUSTOM
            } else {
                return VIEWHOLDER_TYPE_NORMAL
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Adapter.ViewHolder {
            return when (viewType) {
                VIEWHOLDER_TYPE_NORMAL -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.choice_entry_list_element, parent, false)
                    NormalViewHolder(view)
                }
                VIEWHOLDER_TYPE_CUSTOM -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.choice_entry_list_element_custom, parent, false)
                    CustomViewHolder(view)
                }
                else -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.choice_entry_list_element, parent, false)
                    NormalViewHolder(view)
                }
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            entries?.let {
                holder.bind(it[position])
            }
        }

        override fun getItemCount(): Int = entries?.size ?: 0

        inner open class ViewHolder(view: View) : RecyclerView.ViewHolder(view), OnClickListener {

            protected val indicator: ImageView = view.findViewById(R.id.ui_checked)

            protected var entry: Entry? = null
                private set

            init {
                view.setOnClickListener(this)
            }

            open fun bind(entry: Entry) {
                this.entry = entry
                if (selectedIndices.contains(adapterPosition)) {
                    //checked or selected
                    indicator.setImageResource(if (allowMultipleSelection) {
                        R.drawable.checkbox_checked
                    } else {
                        R.drawable.radiobutton_selected
                    })

                    //textView.setTextColor(resources.getColor(R.color.textColorMid, null))
                } else {
                    indicator.setImageResource(
                            if (allowMultipleSelection) {
                                R.drawable.checkbox_empty
                            } else {
                                R.drawable.radiobutton_empty
                            })
                    //textView.setTextColor(resources.getColor(R.color.textColorLight, null))
                }
            }

            override fun onClick(view: View) {
                if (view === itemView) {
                    println("element clicked")
                    if (allowMultipleSelection) {
                        if (selectedIndices.contains(adapterPosition)) {
                            selectedIndices.remove(adapterPosition)
                        } else {
                            selectedIndices.add(adapterPosition)
                        }
                        notifyItemChanged(adapterPosition)

                    } else {
                        val removedIndices = selectedIndices.toTypedArray()
                        selectedIndices.clear()
                        for (i in removedIndices) {
                            notifyItemChanged(i)
                        }
                        selectedIndices.add(adapterPosition)
                        notifyItemChanged(adapterPosition)
                    }

                    valueChanged.invoke(this@ChoiceFormView, null)
                }
            }
        }

        inner class NormalViewHolder(view: View) : ViewHolder(view) {
            private val textView: TextView = view.findViewById(R.id.ui_text)

            override fun bind(entry: Entry) {
                super.bind(entry)
                this.textView.text = entry.text
            }
        }

        inner class CustomViewHolder(view: View) : ViewHolder(view), OnClickListener {
            private val customInputView: TextView = view.findViewById(R.id.ui_input)

            init {
                customInputView.setOnClickListener(this)
            }

            override fun bind(entry: Entry) {
                super.bind(entry)
                this.customInputView.text = entry.text
            }

            override fun onClick(view: View) {
                if (view === itemView && entry?.text?.isNullOrBlank() == true && !selectedIndices.contains(adapterPosition)) {
                    showCustomAnswerDialog(false)
                } else super.onClick(view)


                if (view === customInputView) {
                    showCustomAnswerDialog(false)
                }
            }

            private fun showCustomAnswerDialog(preserveSelection: Boolean) {
                MaterialDialog.Builder(context)
                        .title("Provide your own answer")
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .setSyncWithKeyboard(true)
                        .inputRangeRes(0, 200, R.color.colorRed)
                        .backgroundColorRes(R.color.frontalBackground)
                        .titleColorRes(R.color.textColorDark)
                        .contentColorRes(R.color.textColorMidDark)
                        .positiveColorRes(R.color.colorPointed)
                        .negativeColorRes(R.color.colorRed_Light)
                        .cancelable(true)
                        .negativeText(R.string.msg_cancel)
                        .input(null, entry?.text, true, {
                            dialog, text ->
                            val oldText = entry?.text
                            entry?.text = text.toString()
                            customInputView.text = text.toString()
                            if (oldText?.compareTo(text.toString()) != 0) {

                                if (!preserveSelection) {
                                    if (!selectedIndices.contains(adapterPosition) && text.isNotBlank()) {
                                        selectedIndices.add(adapterPosition)
                                        adapter.notifyItemChanged(adapterPosition)
                                    } else if (selectedIndices.contains(adapterPosition) && !oldText.isNullOrBlank() && text.isNullOrBlank()) {
                                        selectedIndices.remove(adapterPosition)
                                        adapter.notifyItemChanged(adapterPosition)
                                    }
                                }

                                valueChanged.invoke(this@ChoiceFormView, null)
                            }
                        }).show()
            }
        }
    }

}