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
import kotlin.collections.HashMap

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

    private val uniqueValueCountDict = HashMap<String, Int>()

    private val entryListAdapter: Adapter

    private val touchHelper: ItemTouchHelper

    private val listeners = ArrayList<IListEditedListener>()

    var suspendListeners = false

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

        entryListView = findViewById(R.id.ui_recyclerview)

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
/*
            MaterialDialog.Builder(context)
                    .title(R.string.choice_add_new_entry)
                    .input(resources.getString(R.string.choice_add_new_entry_hint), null, false) { dialog, input ->
                        val inputString = input.toString()
                        if (entryList.checkNewTextAddable(inputString)) {
                            entryList.appendNewEntry(inputString)
                            entryListAdapter.notifyItemInserted(entryList.size - 1)
                            checkDuplicatesOnEntryAddition(entryList.size - 1)
                            notifyListeners()
                            dialog.dismiss()
                        } else {
                            dialog.inputEditText?.error = resources.getString(R.string.choice_add_new_entry_error_duplicate)
                        }
                    }
                    .inputRangeRes(1, 30, R.color.colorRed)
                    .autoDismiss(false).cancelable(true)
                    .positiveColorRes(R.color.colorPointed)
                    .positiveText(R.string.msg_add)
                    .show()*/



            entryList.appendNewEntry()
            entryListAdapter.notifyItemInserted(entryList.size - 1)
            //checkDuplicatesOnEntryAddition(entryList.size - 1)
            notifyListeners()
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

            notifyListeners()

            uniqueValueCountDict.clear()
            for (entry in entryList) {
                val value = uniqueValueCountDict[entry.text]
                if (value != null) {
                    uniqueValueCountDict.set(entry.text, value + 1)
                } else {
                    uniqueValueCountDict.set(entry.text, 1)
                }
            }

            entryListAdapter.notifyDataSetChanged()
        }
    }

    private fun notifyListeners() {
        if (!suspendListeners) {
            for (listener in listeners) {
                listener.onContentEdited(this)
            }
        }
    }

    private fun notifyChangedWithValue(entryValue: String) {
        for (entry in entryList) {
            if (entry.text.equals(entryValue)) {
                entryListAdapter.notifyItemChanged(entryList.indexOf(entry.id))
            }
        }
    }

    private fun checkDuplicatesOnEntryRemoval(removedEntryValue: String) {
        val count = uniqueValueCountDict[removedEntryValue]
        if (count != null) {
            if (count >= 2) {
                uniqueValueCountDict[removedEntryValue] = count - 1
                notifyChangedWithValue(removedEntryValue)
            } else if (count == 1) {
                uniqueValueCountDict.remove(removedEntryValue)
            }
        }
    }

    private fun checkDuplicatesOnEntryAddition(addedIndex: Int, excludeThis: Boolean) {
        val addedEntryValue = entryList[addedIndex].text
        if (addedEntryValue.length > 0) {
            val count = uniqueValueCountDict[addedEntryValue]
            if (count == null || count == 0) {
                uniqueValueCountDict[addedEntryValue] = 1
            } else {
                uniqueValueCountDict.set(addedEntryValue, count + 1)
                for (entry in entryList) {
                    if (entry.text.equals(addedEntryValue)) {
                        val index = entryList.indexOf(entry.id)
                        if (!excludeThis || index != addedIndex) {
                            entryListAdapter.notifyItemChanged(index)
                        }
                    }
                }
            }
        }
    }

    private fun checkDuplicatesOnEntryEditing(modifiedIndex: Int, oldValue: String) {
        checkDuplicatesOnEntryRemoval(oldValue)
        checkDuplicatesOnEntryAddition(modifiedIndex, false)
        entryListAdapter.notifyItemChanged(modifiedIndex)
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
            notifyListeners()
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
                    val removedEntry = entryList.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                    checkDuplicatesOnEntryRemoval(removedEntry.text)
                    notifyListeners()
                }
            }


            fun bind(entry: UniqueStringEntryList.Entry) {
                if (!entryInputView.text.toString().equals(entry.text)) {
                    entryInputView.setText(entry.text)
                }

                if (uniqueValueCountDict[entry.text] ?: 1 > 1) {
                    //duplicate
                    entryInputView.background?.level = 1
                } else {
                    //unique
                    entryInputView.background?.level = 0
                }
            }

            override fun onTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                val oldValue = entryList[adapterPosition].text
                val newValue = s.toString().trim()
                if (oldValue != newValue) {
                    entryList[adapterPosition].text = newValue
                    checkDuplicatesOnEntryEditing(adapterPosition, oldValue)
                    notifyListeners()
                }
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