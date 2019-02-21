package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import dagger.internal.Factory
import io.realm.Realm
import kotlinx.android.synthetic.main.input_choice.view.*
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTChoiceAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.ui.components.common.TintFancyButton
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList
import kr.ac.snu.hcil.omnitrack.utils.arrayEquals
import kr.ac.snu.hcil.omnitrack.utils.setPaddingLeft
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.dip
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashSet
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 8/14/2016
 */
class ChoiceInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<IntArray>(R.layout.input_choice, context, attrs) {

    private val listView: RecyclerView = findViewById(R.id.ui_recyclerview_with_fallback)
    private val adapter: Adapter

    private val selectedIds = ArrayList<Int>()

    private val idPivotedEntryIndexTable = SparseIntArray()

    private var appendNewRowButton: TintFancyButton? = null

    private val appendedEntryIds = HashSet<Int>()


    @field:[Inject Backend]
    lateinit var realmProvider: Factory<Realm>

    @Inject
    lateinit var syncManager: OTSyncManager

    private val newRowTextDialogBuilder: MaterialDialog.Builder by lazy {
        MaterialDialog.Builder(this.context)
                .title(R.string.msg_append_new_row)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .setSyncWithKeyboard(true)
                .inputRangeRes(1, 30, R.color.colorRed)
                .cancelable(true)
                .negativeText(R.string.msg_cancel)
                .input(R.string.msg_insert_new_choice_entry, 0, false) { dialog, text ->
                    if (entries.find { it.text == text } == null) {
                        appendNewRow(text)
                    } else {
                        Toast.makeText(context, R.string.msg_duplicate_choice_entry, Toast.LENGTH_LONG).show()
                    }
                }
    }

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.configuredContext().configuredAppComponent.inject(this)

        adapter = Adapter()

        listView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        listView.adapter = adapter
        listView.itemAnimator?.changeDuration = 200
        listView.addItemDecoration(HorizontalDividerItemDecoration(ContextCompat.getColor(context, R.color.separator_Light), dip(0.8f), resources.getDimensionPixelSize(R.dimen.choice_indicator_size) + resources.getDimensionPixelSize(R.dimen.choice_indicator_spacing)))

    }

    var entries: Array<UniqueStringEntryList.Entry> = arrayOf(
            UniqueStringEntryList.Entry(0, "Entry 1"),
            UniqueStringEntryList.Entry(1, "Entry 2"),
            UniqueStringEntryList.Entry(2, "Entry 3"))
        set(value) {
            if (!arrayEquals(field, value)) {
                field = value
                idPivotedEntryIndexTable.clear()
                for (entry in value.withIndex()) {
                    idPivotedEntryIndexTable.put(entry.value.id, entry.index)
                }

                adapter.notifyDataSetChanged()
            }
        }

    override var value: IntArray? = null
        set(_value) {
            val value = if (_value?.isEmpty() == false) _value else null
            if (!arrayEquals(field, value)) {
                field = value
                if ((field == null && value?.size == 0) || (field?.size == 0 && value == null)) {

                } else {
                    selectedIds.clear()
                    selectedIds.addAll(value?.toTypedArray() ?: emptyArray())
                    adapter.notifyDataSetChanged()

                    println("Choice input value changed")
                    onValueChanged(value)
                }
            }
        }


    var multiSelectionMode: Boolean by Delegates.observable(false) { _, old, new ->
        if (old != new) {
            if (!new && selectedIds.size > 1) {
                val first = selectedIds.first()
                selectedIds.clear()
                selectedIds.add(first)
            }

            adapter.notifyDataSetChanged()
        }
    }

    var allowAppendingFromView: Boolean by Delegates.observable(false) { prop, old, new ->
        if (old != new) {
            this.setAppendNewEntryVisibility(new)
        }
    }


    override fun focus() {

    }

    override val typeId: Int = VIEW_TYPE_CHOICE


    private inline fun modifyEntryList(modifyFunc: (UniqueStringEntryList) -> Boolean) {
        if (boundAttributeObjectId != null) {
            this.realmProvider.get().use { realm ->
                val attribute = realm.where(OTAttributeDAO::class.java).equalTo(BackendDbManager.FIELD_OBJECT_ID, boundAttributeObjectId).findFirst()
                if (attribute != null) {
                    val configuredContext = (context.applicationContext as OTApp).applicationComponent.configuredContext()
                    val helper = attribute.getHelper(configuredContext) as OTChoiceAttributeHelper
                    val originalEntryList = helper.getChoiceEntries(attribute)
                    if (originalEntryList != null) {
                        if (modifyFunc(originalEntryList)) {
                            realm.executeTransaction {
                                helper.setPropertyValue(OTChoiceAttributeHelper.PROPERTY_ENTRIES, originalEntryList, attribute, realm)
                            }
                            attribute.trackerId?.let { _ ->
                                val tracker = realm.where(OTTrackerDAO::class.java).equalTo(BackendDbManager.FIELD_OBJECT_ID, attribute.trackerId).findFirst()
                                if (tracker != null) {
                                    realm.executeTransaction { _ ->
                                        tracker.synchronizedAt = null
                                    }
                                    syncManager.registerSyncQueue(ESyncDataType.TRACKER, SyncDirection.UPLOAD, ignoreDirtyFlags = false)
                                }
                            }
                            this.entries = originalEntryList.toArray()
                        }
                    }
                }

            }
        }
    }

    private fun appendNewRow(text: CharSequence) {
        modifyEntryList { list ->
            this.appendedEntryIds.add(list.appendNewEntry(text.toString()))
        }
    }

    private fun removeAppendedRow(id: Int) {
        modifyEntryList {
            val index = it.indexOf(id)
            if (index != -1) {
                it.removeAt(it.indexOf(id))
                this.appendedEntryIds.remove(id)
                this.selectedIds.remove(id)
                true
            } else false
        }
    }

    private fun setAppendNewEntryVisibility(visible: Boolean) {
        if (visible) {
            if (appendNewRowButton == null) {
                val newButton = TintFancyButton(context)
                newButton.text = context.getString(R.string.msg_append_new_row)
                newButton.backgroundColorResource = android.R.color.transparent
                newButton.setPaddingLeft(0)
                newButton.setFocusBackgroundColor(ContextCompat.getColor(context, R.color.colorSecondary))
                newButton.setIconResource(R.drawable.rounded_plus_light)
                newButton.tintColor = ContextCompat.getColor(context, R.color.textColorLightLight)
                newButton.setTextColor(ContextCompat.getColor(context, R.color.textColorLight))
                newButton.setOnClickListener {
                    newRowTextDialogBuilder.show()
                }

                newButton.gravity = Gravity.START or Gravity.CENTER_VERTICAL

                newButton.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

                appendNewRowButton = newButton
                ui_container.addView(newButton)
            }
        } else {
            appendNewRowButton?.visibility = View.GONE
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.choice_entry_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(entries[position])
        }

        override fun getItemCount(): Int = entries.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), OnClickListener {

            private val indicator: ImageView = view.findViewById(R.id.ui_checked)
            private val textView: TextView = view.findViewById(R.id.ui_text)
            private val removeAppendedEntryButton: AppCompatImageButton = view.findViewById(R.id.ui_button_delete)

            private var id: Int = -1

            init {
                view.setOnClickListener(this)
            }

            fun bind(entry: UniqueStringEntryList.Entry) {
                textView.text = entry.text
                id = entry.id
                if (selectedIds.contains(entry.id)) {
                    //checked or selected
                    indicator.setImageResource(if (multiSelectionMode) {
                        R.drawable.checkbox_checked
                    } else {
                        R.drawable.radiobutton_selected
                    })

                    //textView.setTextColor(resources.getColor(R.color.textColorMid, null))
                } else {
                    indicator.setImageResource(
                            if (multiSelectionMode) {
                                R.drawable.checkbox_empty
                            } else {
                                R.drawable.radiobutton_empty
                            })
                    //textView.setTextColor(resources.getColor(R.color.textColorLight, null))
                }

                if (appendedEntryIds.contains(id)) {
                    removeAppendedEntryButton.visibility = View.VISIBLE
                    removeAppendedEntryButton.setOnClickListener(this)
                } else {
                    removeAppendedEntryButton.visibility = View.GONE
                    removeAppendedEntryButton.setOnClickListener(null)
                }
            }

            val comparer = Comparator<Int> { a, b ->
                when {
                    idPivotedEntryIndexTable[a] > idPivotedEntryIndexTable[b] -> 1
                    idPivotedEntryIndexTable[a] == idPivotedEntryIndexTable[b] -> 0
                    else -> -1
                }
            }

            private fun sortSelectedIdsByEntryPosition() {
                Collections.sort(selectedIds, comparer)
            }


            override fun onClick(view: View?) {
                if (view === this.itemView) {
                    if (multiSelectionMode) {
                        if (selectedIds.contains(id)) {
                            selectedIds.remove(id)
                        } else {
                            selectedIds.add(id)
                            sortSelectedIdsByEntryPosition()
                        }

                    } else {

                        if (!selectedIds.contains(id)) {
                            if (selectedIds.size > 0) {
                                for (i in selectedIds.size - 1..0) {
                                    val selectedId = selectedIds[i]
                                    selectedIds.removeAt(i)

                                    for (entry in entries.withIndex()) {
                                        if (entry.value.id == selectedId) {
                                            notifyItemChanged(entry.index)
                                            break
                                        }
                                    }
                                }
                            }

                            selectedIds.add(id)
                            sortSelectedIdsByEntryPosition()
                        }
                    }

                    notifyItemChanged(adapterPosition)

                    value = selectedIds.toIntArray()

                } else if (view === this.removeAppendedEntryButton) {
                    DialogHelper.makeNegativePhrasedYesNoDialogBuilder(context, BuildConfig.APP_NAME, context.resources.getString(R.string.msg_confirm_remove_appended_entry), R.string.msg_remove, R.string.msg_cancel, {
                        removeAppendedRow(id)
                    }).show()
                }
            }

        }
    }
}