package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.DragItemTouchHelperCallback
import kr.ac.snu.hcil.omnitrack.utils.move
import java.util.*

/**
 * Created by younghokim on 16. 8. 13..
 */
class ChoiceEntryListEditor : LinearLayout, View.OnClickListener {

    private val newEntryButton: Button
    private val entryListView: RecyclerView

    private val entryList: ArrayList<String> = arrayListOf("")

    private val entryListAdapter: Adapter

    private val touchHelper: ItemTouchHelper

    var entries: Array<String>
        get() {
            val array = entryList.filter { !it.isNullOrEmpty() }.toTypedArray()
            println(array)
            return array
        }
        set(value) {
            entryList.clear()
            entryList.addAll(value)

            if (value.isEmpty()) {
                entryList.add("")
                entryList.add("")
            }

            entryListAdapter.notifyDataSetChanged()
        }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {

        orientation = VERTICAL

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_choice_entry_list_editor, this, true)

        newEntryButton = findViewById(R.id.ui_button_new_entry) as Button
        newEntryButton.setOnClickListener(this)

        entryListView = findViewById(R.id.ui_list) as RecyclerView

        entryListView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        entryListView.itemAnimator = null
        //entryListView.itemAnimator = SlideInLeftAnimator()

        entryListAdapter = Adapter()
        entryListView.adapter = entryListAdapter

        touchHelper = ItemTouchHelper(DragItemTouchHelperCallback(entryListAdapter, context, true, false, false))
        touchHelper.attachToRecyclerView(entryListView)
    }

    override fun onClick(p0: View?) {
        if (p0 === newEntryButton) {
            //add new entry
            entryList.add("")
            entryListAdapter.notifyItemInserted(entryList.size - 1)
        }
    }

    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>(), DragItemTouchHelperCallback.ItemDragHelperAdapter {

        override fun getItemCount(): Int {
            return entryList.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(entryList[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.choice_entry_list_element_editor, parent, false)
            return ViewHolder(view)
        }

        override fun onMoveItem(fromPosition: Int, toPosition: Int) {
            entryList.move(fromPosition, toPosition)
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onRemoveItem(position: Int) {

        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, TextWatcher, OnTouchListener {

            val entryInputView: EditText
            val removeEntryButton: View
            val dragHandle: View

            init {
                entryInputView = view.findViewById(R.id.ui_text) as EditText
                entryInputView.addTextChangedListener(this)

                removeEntryButton = view.findViewById(R.id.ui_button_remove)
                removeEntryButton.setOnClickListener(this)

                dragHandle = view.findViewById(R.id.ui_drag_handle)
                dragHandle.setOnTouchListener(this)
            }

            override fun onClick(p0: View?) {
                if (p0 === removeEntryButton) {
                    entryList.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                }
            }


            fun bind(entry: String) {
                entryInputView.setText(entry)
            }

            override fun onTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                entryList[adapterPosition] = s.toString()
            }

            override fun onTouch(view: View, mv: MotionEvent): Boolean {
                if (view === dragHandle) {
                    if (mv.action == MotionEvent.ACTION_DOWN) {
                        touchHelper.startDrag(this@ViewHolder)
                        return true
                    }
                }
                return false
            }
        }
    }

}