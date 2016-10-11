package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTTimeAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AttributeSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.ItemComparator
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.DrawableListBottomSpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.getDayOfMonth
import java.util.*

class ItemBrowserActivity : MultiButtonActionBarActivity(R.layout.activity_item_browser), AdapterView.OnItemSelectedListener, View.OnClickListener {

    companion object {
        fun makeIntent(tracker: OTTracker, context: Context): Intent {
            val intent = Intent(context, ItemBrowserActivity::class.java)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            return intent
        }
    }

    private var tracker: OTTracker? = null

    private val items = ArrayList<OTItem>()

    private lateinit var itemListView: RecyclerView

    private lateinit var itemListViewAdapter: ItemListViewAdapter

    private lateinit var emptyListMessageView: TextView

    private lateinit var sortSpinner: Spinner

    private lateinit var sortOrderButton: ToggleButton

    private var supportedItemComparators: List<ItemComparator>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sortSpinner = findViewById(R.id.ui_spinner_sort_method) as Spinner
        sortSpinner.onItemSelectedListener = this

        rightActionBarButton?.visibility = View.VISIBLE
        rightActionBarButton?.setImageResource(R.drawable.ic_add_box)
        leftActionBarButton?.visibility = View.VISIBLE
        leftActionBarButton?.setImageResource(R.drawable.back_rhombus)

        itemListView = findViewById(R.id.ui_item_list) as RecyclerView

        itemListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        itemListView.addItemDecoration(HorizontalImageDividerItemDecoration(context = this))
        itemListView.addItemDecoration(DrawableListBottomSpaceItemDecoration(R.drawable.expanded_view_inner_shadow_top, 0))

        emptyListMessageView = findViewById(R.id.ui_empty_list_message) as TextView

        itemListViewAdapter = ItemListViewAdapter()

        itemListView.adapter = itemListViewAdapter

        sortOrderButton = findViewById(R.id.ui_toggle_sort_order) as ToggleButton
        sortOrderButton.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                sortOrderButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ascending, 0)
            } else {
                sortOrderButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.descending, 0)
            }
        }
        InterfaceHelper.removeButtonTextDecoration(sortOrderButton)

        sortOrderButton.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()

        items.clear()
        if (intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER) != null) {
            tracker = OTApplication.app.currentUser.trackers.filter { it.objectId == intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER) }.first()

            setTitle(String.format(resources.getString(R.string.title_activity_item_browser, tracker?.name)))

            supportedItemComparators = tracker?.getSupportedComparators()

            if (supportedItemComparators != null) {
                val adapter = ArrayAdapter<ItemComparator>(this, R.layout.simple_list_element_text_light, R.id.textView, supportedItemComparators)
                adapter.setDropDownViewResource(R.layout.simple_list_element_text_dropdown)

                sortSpinner.adapter = adapter
            }

            OTApplication.app.dbHelper.getItems(tracker!!, items)

            onItemListChanged()
        }


    }

    override fun onClick(view: View) {
        reSort()
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, id: Long) {
        reSort()
    }

    private fun reSort() {
        val comparator = supportedItemComparators?.get(sortSpinner.selectedItemPosition)
        if (comparator != null) {
            println("sort items by ${comparator}")
            comparator.isDecreasing = !sortOrderButton.isChecked
            items.sortWith(comparator)
            onItemListChanged()
        }
    }

    private fun getCurrentSort(): ItemComparator? {
        return supportedItemComparators?.get(sortSpinner.selectedItemPosition)
    }

    private fun onItemListChanged() {
        itemListViewAdapter.notifyDataSetChanged()
        onListChanged()
    }

    private fun onItemChanged(position: Int) {
        itemListViewAdapter.notifyItemChanged(position)
        onListChanged()
    }

    private fun onItemRemoved(position: Int) {
        itemListViewAdapter.notifyItemRemoved(position)
        onListChanged()
    }

    private fun onListChanged() {
        emptyListMessageView.visibility = if (items.size != 0) View.GONE else View.VISIBLE

    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {
        if (tracker != null) {
            startActivity(ItemEditingActivity.makeIntent(tracker!!.objectId, this))
        }
    }

    inner class ItemListViewAdapter : RecyclerView.Adapter<ItemListViewAdapter.ItemElementViewHolder>() {
        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: ItemElementViewHolder, position: Int) {
            holder.bindItem(items[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemElementViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_element, parent, false)
            return ItemElementViewHolder(view)
        }

        inner class ItemElementViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, PopupMenu.OnMenuItemClickListener {

            val colorBar: View

            val monthView: TextView
            val dayView: TextView

            val valueListView: RecyclerView

            val moreButton: View

            val valueListAdapter: TableRowAdapter

            val itemMenu: PopupMenu


            init {

                val leftBar = view.findViewById(R.id.ui_left_bar)

                leftBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                view.minimumHeight = leftBar.measuredHeight

                colorBar = view.findViewById(R.id.color_bar)
                /*
                if(tracker!=null)
                    colorBar.setBackgroundColor(tracker!!.color)
*/
                monthView = view.findViewById(R.id.ui_text_month) as TextView
                dayView = view.findViewById(R.id.ui_text_day) as TextView

                moreButton = view.findViewById(R.id.ui_button_more)
                moreButton.setOnClickListener(this)

                itemMenu = PopupMenu(this@ItemBrowserActivity, moreButton, Gravity.TOP or Gravity.LEFT)
                itemMenu.inflate(R.menu.menu_item_list_element)
                itemMenu.setOnMenuItemClickListener(this)


                valueListView = view.findViewById(R.id.ui_list) as RecyclerView
                valueListView.layoutManager = LinearLayoutManager(this@ItemBrowserActivity, LinearLayoutManager.VERTICAL, false)

                valueListView.addItemDecoration(HorizontalDividerItemDecoration(ContextCompat.getColor(this@ItemBrowserActivity, R.color.separator_Light), (1 * resources.displayMetrics.density).toInt()))

                valueListAdapter = TableRowAdapter()
                valueListView.adapter = valueListAdapter
            }

            override fun onClick(p0: View?) {
                if (p0 === moreButton) {
                    itemMenu.show()
                }
            }

            override fun onMenuItemClick(p0: MenuItem): Boolean {
                println(p0.itemId)
                when (p0.itemId) {
                    R.id.action_edit -> {
                        startActivity(ItemEditingActivity.makeIntent(items[adapterPosition], tracker!!, this@ItemBrowserActivity))
                        return true
                    }
                    R.id.action_remove -> {
                        DialogHelper.makeYesNoDialogBuilder(this@ItemBrowserActivity, "OmniTrack", resources.getString(R.string.msg_item_remove_confirm), {
                            OTApplication.app.dbHelper.removeItem(items[adapterPosition])
                            items.removeAt(adapterPosition)
                            onItemRemoved(adapterPosition)
                        }).show()
                        return true
                    }
                }
                return false
            }


            fun bindItem(item: OTItem) {
                val cal = Calendar.getInstance()
                val currentSorter = getCurrentSort()

                cal.timeInMillis =
                        if (currentSorter is AttributeSorter && currentSorter.attribute is OTTimeAttribute) {
                            if (item.hasValueOf(currentSorter.attribute)) {
                                (item.getValueOf(currentSorter.attribute) as TimePoint).timestamp
                            } else item.timestamp
                        } else item.timestamp


                monthView.text = String.format(Locale.US, "%tb", cal);
                dayView.text = cal.getDayOfMonth().toString()

                valueListAdapter.notifyDataSetChanged()
            }

            inner class TableRowAdapter : RecyclerView.Adapter<TableRowAdapter.TableRowViewHolder>() {

                fun getParentItem(): OTItem {
                    return items[this@ItemElementViewHolder.adapterPosition]
                }

                override fun getItemViewType(position: Int): Int {
                    if (getParentItem().hasValueOf(tracker!!.attributes[position]))
                        return tracker?.attributes?.get(position)?.getViewForItemListContainerType() ?: 0
                    else return 5
                }

                override fun onBindViewHolder(holder: TableRowViewHolder, position: Int) {
                    holder.bindAttribute(tracker!!.attributes[position])
                }

                override fun getItemCount(): Int {
                    return tracker?.attributes?.size ?: 0
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableRowViewHolder {

                    val view = LayoutInflater.from(parent.context).inflate(when (viewType) {
                        OTAttribute.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_SINGLELINE -> R.layout.item_attribute_row_singleline
                        OTAttribute.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE -> R.layout.item_attribute_row_multiline
                        else -> R.layout.item_attribute_row_singleline
                    }, parent, false)

                    return if (viewType == 5) {
                        NoValueTableRowViewHolder(view as ViewGroup)
                    } else TableRowViewHolder(view as ViewGroup)
                }

                inner open class TableRowViewHolder(val view: ViewGroup) : RecyclerView.ViewHolder(view) {

                    val attributeNameView: TextView
                    var valueView: View

                    init {
                        attributeNameView = view.findViewById(R.id.ui_attribute_name) as TextView
                        valueView = view.findViewById(R.id.ui_value_view_replace)
                    }

                    open fun bindAttribute(attribute: OTAttribute<out Any>) {
                        attributeNameView.text = attribute.name
                        val sort = getCurrentSort()
                        attributeNameView.setTextColor(
                                if (sort is AttributeSorter && sort.attribute === attribute) {
                                    ContextCompat.getColor(this@ItemBrowserActivity, R.color.colorAccent)
                                } else ContextCompat.getColor(this@ItemBrowserActivity, R.color.textColorLight)
                        )

                        val newValueView = attribute.getViewForItemList(this@ItemBrowserActivity, valueView)
                        if (newValueView !== valueView) {
                            val lp = valueView.layoutParams

                            val container = valueView.parent as ViewGroup

                            val index = container.indexOfChild(valueView)
                            container.removeView(valueView)
                            newValueView.layoutParams = lp
                            container.addView(newValueView, index)
                            valueView = newValueView
                        }

                        if (getParentItem().hasValueOf(attribute)) {
                            attribute.applyValueToViewForItemList(getParentItem().getValueOf(attribute), valueView)
                        }
                    }
                }

                inner class NoValueTableRowViewHolder(view: ViewGroup) : TableRowViewHolder(view) {
                    init {
                        if (valueView is TextView) {
                            (valueView as TextView).text = "No value"
                            (valueView as TextView).setTextColor(ContextCompat.getColor(this@ItemBrowserActivity, R.color.colorRed_Light))
                        }
                    }

                    override fun bindAttribute(attribute: OTAttribute<out Any>) {
                        attributeNameView.text = attribute.name
                    }
                }
            }
        }
    }

}
