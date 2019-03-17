package kr.ac.snu.hcil.omnitrack.views.properties

import android.content.Context
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.snu.hcil.android.common.containers.UniqueStringEntryList
import kr.ac.snu.hcil.android.common.view.container.DragItemTouchHelperCallback
import java.util.*

/**
 * Created by younghokim on 16. 8. 13..
 */
class ChoiceEntryListEditor : LinearLayout, View.OnClickListener {

    interface IListEditedListener {
        fun onContentEdited(editor: ChoiceEntryListEditor)
    }

    private val newEntryButton: Button
    private val entryListView: RecyclerView

    private val entryList: UniqueStringEntryList


    private val entryListAdapter: Adapter

    private val touchHelper: ItemTouchHelper

    private val listeners = ArrayList<IListEditedListener>()


    /*
    var entries: Array<Entry>
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
        */

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {

        orientation = VERTICAL

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_choice_entry_list_editor, this, true)

        newEntryButton = findViewById(R.id.ui_button_new_entry)
        newEntryButton.setOnClickListener(this)

        entryListView = findViewById(R.id.ui_recyclerview_with_fallback)

        entryListView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        entryListView.itemAnimator = null
        //entryListView.itemAnimator = SlideInLeftAnimator()

        entryListAdapter = Adapter()
        entryListView.adapter = entryListAdapter

        touchHelper = ItemTouchHelper(DragItemTouchHelperCallback(entryListAdapter, context, true, false, false))
        touchHelper.attachToRecyclerView(entryListView)

        entryList = UniqueStringEntryList()
    }

    fun addListEditedListener(listener: IListEditedListener) {
        this.listeners.add(listener)
    }

    override fun onClick(p0: View?) {
        if (p0 === newEntryButton) {
            //add new entry
            entryList.appendNewEntry()
            entryListAdapter.notifyItemInserted(entryList.size - 1)
            notifyListEdited()
        }
    }

    fun getNotBlankEntryList(): UniqueStringEntryList {
        val clone = entryList.clone()
        clone.filterSelf { !it.text.isBlank() }
        return clone
    }

    fun setEntryList(newList: UniqueStringEntryList) {
        if (entryList !== newList) {
            entryList.set(newList)
            if (entryList.isEmpty()) {
                entryList.appendNewEntry()
                entryList.appendNewEntry()
            }

            entryListAdapter.notifyDataSetChanged()

        }
    }

    private fun notifyListEdited() {
        for (listener in listeners) {
            listener.onContentEdited(this)
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
            notifyListEdited()
        }

        override fun onRemoveItem(position: Int) {

        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), OnClickListener, TextWatcher, OnTouchListener {

            val entryInputView: EditText = view.findViewById(R.id.ui_text)
            val removeEntryButton: View = view.findViewById(R.id.ui_button_remove)
            val dragHandle: View = view.findViewById(R.id.ui_drag_handle)

            init {
                entryInputView.addTextChangedListener(this)
                removeEntryButton.setOnClickListener(this)
                dragHandle.setOnTouchListener(this)
            }

            override fun onClick(p0: View?) {
                if (p0 === removeEntryButton) {
                    entryList.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                    notifyListEdited()
                }
            }


            fun bind(entry: UniqueStringEntryList.Entry) {
                entryInputView.setText(entry.text)
            }

            override fun onTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                entryList[adapterPosition].text = s.toString()
                notifyListEdited()
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