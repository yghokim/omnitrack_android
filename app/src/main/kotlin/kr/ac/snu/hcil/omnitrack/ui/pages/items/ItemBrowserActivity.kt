package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.ToggleButton
import butterknife.bindView
import io.reactivex.disposables.Disposable
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.OTTimeAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.ItemComparator
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.services.OTTableExportService
import kr.ac.snu.hcil.omnitrack.ui.DragItemTouchHelperCallback
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.DismissingBottomSheetDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.components.common.ExtendedSpinner
import kr.ac.snu.hcil.omnitrack.ui.components.common.FallbackRecyclerView
import kr.ac.snu.hcil.omnitrack.ui.components.common.viewholders.RecyclerViewMenuAdapter
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.DrawableListBottomSpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.AttributeEditDialogFragment
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.dipRound
import kr.ac.snu.hcil.omnitrack.utils.getDayOfMonth
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.subscriptions.CompositeSubscription
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

class ItemBrowserActivity : MultiButtonActionBarActivity(R.layout.activity_item_browser), ExtendedSpinner.OnItemSelectedListener, View.OnClickListener, AttributeEditDialogFragment.Listener {

    companion object {

        const val REQUEST_CODE_NEW_ITEM = 151
        const val REQUEST_CODE_EDIT_ITEM = 150

        fun makeIntent(trackerId: String, context: Context): Intent {
            val intent = Intent(context, ItemBrowserActivity::class.java)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            return intent
        }
    }

    private lateinit var viewModel: ItemListViewModel

    private val items = ArrayList<ItemListViewModel.ItemViewModel>()

    private val itemListView: FallbackRecyclerView by bindView(R.id.ui_item_list)

    private lateinit var itemListViewAdapter: ItemListViewAdapter

    private val emptyListMessageView: TextView by bindView(R.id.ui_empty_list_message)

    private val sortSpinner: ExtendedSpinner by bindView(R.id.ui_spinner_sort_method)

    private val sortOrderButton: ToggleButton by bindView(R.id.ui_toggle_sort_order)

    private lateinit var removalSnackbar: Snackbar

    private val settingsFragment: BottomSheetDialogFragment?
        get() {
            return SettingsDialogFragment.getInstance()
        }

    private val startSubscriptions = CompositeSubscription()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sortSpinner.onItemSelectedListener = this

        rightActionBarButton?.visibility = View.VISIBLE
        rightActionBarButton?.setImageResource(R.drawable.ic_add_box)
        leftActionBarButton?.visibility = View.VISIBLE
        leftActionBarButton?.setImageResource(R.drawable.back_rhombus)

        setRightSubButtonImage(R.drawable.settings_dark)
        showRightSubButton()

        itemListView.emptyView = emptyListMessageView
        itemListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        itemListView.addItemDecoration(HorizontalImageDividerItemDecoration(context = this))
        itemListView.addItemDecoration(DrawableListBottomSpaceItemDecoration(R.drawable.expanded_view_inner_shadow_top, 0))

        itemListViewAdapter = ItemListViewAdapter()

        itemListView.adapter = itemListViewAdapter

        itemListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (removalSnackbar.isShown) {
                    removalSnackbar.dismiss()
                }
            }

        })


        /*ItemTouchHelper(DragItemTouchHelperCallback(itemListViewAdapter, this, false, true))
                .attachToRecyclerView(itemListView)*/


        sortOrderButton.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                sortOrderButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ascending, 0)
            } else {
                sortOrderButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.descending, 0)
            }
        }
        InterfaceHelper.removeButtonTextDecoration(sortOrderButton)

        sortOrderButton.setOnClickListener(this)

        val snackBarContainer: CoordinatorLayout = findViewById(R.id.ui_snackbar_container)
        removalSnackbar = Snackbar.make(snackBarContainer, resources.getText(R.string.msg_item_removed_message), Snackbar.LENGTH_INDEFINITE)
        removalSnackbar.setAction(resources.getText(R.string.msg_undo)) { view ->
            itemListViewAdapter.undoRemoval()
        }.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                itemListViewAdapter.clearTrashcan()
            }

        })

        viewModel = ViewModelProviders.of(this).get(ItemListViewModel::class.java)

        creationSubscriptions.add(
                viewModel.trackerNameObservable.subscribe { name ->
                    title = String.format(resources.getString(R.string.title_activity_item_browser, name))
                }
        )

        creationSubscriptions.add(
                viewModel.sorterSetObservable.subscribe { set ->
                    val adapter = ArrayAdapter<ItemComparator>(this, R.layout.simple_list_element_text_light, R.id.textView, set)
                    adapter.setDropDownViewResource(R.layout.simple_list_element_text_dropdown)

                    sortSpinner.adapter = adapter
                }
        )

        creationSubscriptions.add(
                viewModel.sortedItemsObservable.subscribe {
                    val diffResult = DiffUtil.calculateDiff(
                            ItemListViewModel.ItemViewModelListDiffUtilCallback(items, it)
                    )
                    items.clear()
                    items.addAll(it)
                    diffResult.dispatchUpdatesTo(itemListViewAdapter)
                }
        )

        creationSubscriptions.add(
                viewModel.onSchemaChanged.subscribe {
                    itemListViewAdapter.notifyDataSetChanged()
                }
        )

        creationSubscriptions.add(
                viewModel.currentSorterObservable.subscribe { newSorter ->
                    itemListViewAdapter.notifyDataSetChanged()
                }
        )

        creationSubscriptions.add(
                viewModel.onItemContentChanged.subscribe { changeRanges ->
                    println("changeRanges: ${changeRanges}")
                    changeRanges.forEach {
                        itemListViewAdapter.notifyItemRangeChanged(it.startIndex, it.length)
                    }
                }
        )

        val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        if (trackerId != null) {
            viewModel.init(trackerId)
        }

    }

    override fun onOkAttributeEditDialog(changed: Boolean, value: Any?, trackerId: String, attributeLocalId: String, itemId: String?) {
        println("dismiss handler")
        Log.d(AttributeEditDialogFragment.TAG, "changed: ${changed}, value: ${value}")


        if (this.viewModel.trackerId == trackerId) {
            if (itemId != null) {
                val item = items.find { item -> item.itemId == itemId }
                if (item != null) {
                    item.setValueOf(attributeLocalId, value?.let { TypeStringSerializationHelper.serialize(it) })

                    creationSubscriptions.add(
                            item.save(*(item.itemDao.fieldValueEntries.map { it.key } - attributeLocalId).toTypedArray()).subscribe { (resultCode, itemId) ->
                                if (resultCode != RealmDatabaseManager.SAVE_RESULT_FAIL) {
                                    println("item was modified successfully.")
                                }
                            }
                    )
                    itemListViewAdapter.notifyItemChanged(items.indexOf(item))
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        startSubscriptions.clear()
    }

    override fun onPause() {
        super.onPause()
        itemListViewAdapter.clearTrashcan()
        removalSnackbar.dismiss()
    }

    override fun onClick(view: View) {
        reSort()
    }


    override fun onItemSelected(spinner: ExtendedSpinner, position: Int) {
        reSort()
    }

    private fun onItemRemoved(position: Int) {
        itemListViewAdapter.notifyItemRemoved(position)
        //onListChanged()
    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {
        val intent = ItemDetailActivity.makeNewItemPageIntent(viewModel.trackerId, this)
        intent.putExtra(OTApplication.INTENT_EXTRA_FROM, this@ItemBrowserActivity.javaClass.simpleName)
        startActivityForResult(intent, REQUEST_CODE_NEW_ITEM)
    }

    override fun onToolbarRightSubButtonClicked() {
        settingsFragment?.show(supportFragmentManager, "TRACKER_ITEMS_SETTINGS")
    }

    override fun onDestroy() {
        super.onDestroy()

        itemListViewAdapter.dispose()
    }

    private fun reSort() {
        val comparator = getCurrentSort()
        if (comparator != null) {
            println("sort items by ${comparator}")
            viewModel.setSorter(comparator)
        }
    }

    private fun getCurrentSort(): ItemComparator? {
        val sorter = viewModel.sorterSetObservable.value?.get(sortSpinner.selectedItemPosition)
        sorter?.isDecreasing = !sortOrderButton.isChecked
        return sorter
    }

    fun deleteItemPermanently(position: Int): String? {
        val removedItem = items[position]
        removedItem.itemId?.let {
            viewModel.removeItem(it)
        }
        return removedItem.itemId
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_EDIT_ITEM -> {
                    println("activity result OK with edit item:")
                }
                REQUEST_CODE_NEW_ITEM -> {
                    println("activity result OK with new item:")
                }
            }
        }
    }

    inner class ItemListViewAdapter : RecyclerView.Adapter<ItemListViewAdapter.ItemElementViewHolder>(), DragItemTouchHelperCallback.ItemDragHelperAdapter {

        private val removedItems = HashSet<String>()

        private val viewHolders = ArrayList<ItemElementViewHolder>()

        fun clearTrashcan() {
            for (item in removedItems) {
                //TODO remove
                //OTApplication.app.databaseManager.removeItem(item)
            }
        }

        fun dispose() {
            viewHolders.forEach {
                it.itemLevelSubscriptions.clear()
            }
        }

        override fun onMoveItem(fromPosition: Int, toPosition: Int) {

        }

        override fun onRemoveItem(position: Int) {
            items.removeAt(position).itemId?.let { removedItems += it }
            removalSnackbar.show()
        }

        fun undoRemoval() {
            /*
            if (!removedItems.isEmpty()) {
                val restored = removedItems.removeAt(removedItems.size - 1)
                items.add(restored)
                reSort(false)
                val newPosition = items.indexOf(restored)
                if (newPosition != -1) {
                    itemListViewAdapter.notifyItemInserted(newPosition)
                } else {
                    itemListViewAdapter.notifyDataSetChanged()
                }
            }*/
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: ItemElementViewHolder, position: Int) {
            holder.bindItem(items[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemElementViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_element, parent, false)
            val viewHolder = ItemElementViewHolder(view)
            viewHolders.add(viewHolder)
            return viewHolder
        }

        inner class ItemElementViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, PopupMenu.OnMenuItemClickListener {

            val itemLevelSubscriptions = CompositeSubscription()
            val colorBar: View by bindView(R.id.color_bar)

            val monthView: TextView by bindView(R.id.ui_text_month)
            val dayView: TextView by bindView(R.id.ui_text_day)

            val valueListView: RecyclerView by bindView(R.id.ui_list)

            val moreButton: View by bindView(R.id.ui_button_more)

            val sourceView: TextView by bindView(R.id.ui_logging_source)
            val loggingTimeView: TextView by bindView(R.id.ui_logging_time)

            val valueListAdapter: TableRowAdapter

            val itemMenu: PopupMenu


            init {

                val leftBar: View = view.findViewById(R.id.ui_left_bar)

                leftBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                view.minimumHeight = leftBar.measuredHeight

                /*
                if(tracker!=null)
                    colorBar.setBackgroundColor(tracker!!.color)
*/
                moreButton.setOnClickListener(this)

                itemMenu = PopupMenu(this@ItemBrowserActivity, moreButton, Gravity.TOP or Gravity.START)
                itemMenu.inflate(R.menu.menu_item_list_element)
                itemMenu.setOnMenuItemClickListener(this)

                valueListView.layoutManager = LinearLayoutManager(this@ItemBrowserActivity, LinearLayoutManager.VERTICAL, false)

                valueListView.addItemDecoration(HorizontalDividerItemDecoration(ContextCompat.getColor(this@ItemBrowserActivity, R.color.separator_Light), dipRound(1)))

                valueListAdapter = TableRowAdapter()
                valueListView.adapter = valueListAdapter
            }

            override fun onClick(p0: View?) {
                if (p0 === moreButton) {
                    itemMenu.show()
                    removalSnackbar.dismiss()
                }
            }

            override fun onMenuItemClick(p0: MenuItem): Boolean {
                when (p0.itemId) {
                    R.id.action_edit -> {
                        val intent = ItemDetailActivity.makeItemEditPageIntent(items[adapterPosition].itemId!!, viewModel.trackerId, this@ItemBrowserActivity)
                        intent.putExtra(OTApplication.INTENT_EXTRA_FROM, this@ItemBrowserActivity.javaClass.simpleName)
                        startActivityForResult(intent, REQUEST_CODE_EDIT_ITEM)
                        return true
                    }
                    R.id.action_remove -> {
                        DialogHelper.makeNegativePhrasedYesNoDialogBuilder(this@ItemBrowserActivity, "OmniTrack", resources.getString(R.string.msg_item_remove_confirm), R.string.msg_remove, onYes = {
                            deleteItemPermanently(adapterPosition)
                        }).show()
                        return true
                    }
                }
                return false
            }


            fun bindItem(itemVM: ItemListViewModel.ItemViewModel) {
                itemLevelSubscriptions.clear()

                val cal = Calendar.getInstance()
                //val currentSorter = getCurrentSort()
                cal.timeInMillis = itemVM.timestamp
                monthView.text = String.format(Locale.US, "%tb", cal)
                dayView.text = cal.getDayOfMonth().toString()

                sourceView.text = itemVM.loggingSource.sourceText
                loggingTimeView.text = OTTimeAttribute.formats[OTTimeAttribute.GRANULARITY_MINUTE]!!.format(Date(itemVM.timestamp))

                itemLevelSubscriptions.add(
                        viewModel.currentSorterObservable.subscribe { sort ->
                            if (sort is AFieldValueSorter) {
                                val rowIndex = viewModel.attributes.indexOfFirst { it.localId == sort.attributeLocalId }
                                if (rowIndex != -1) {
                                    valueListAdapter.notifyItemChanged(rowIndex)
                                }
                            }
                        }
                )

                valueListAdapter.notifyDataSetChanged()

                /*
                cal.timeInMillis =
                        if (currentSorter is AFieldValueSorter && currentSorter.attribute is OTTimeAttribute) {
                            if (item.hasValueOf(currentSorter.attribute)) {
                                (item.getValueOf(currentSorter.attribute) as TimePoint).timestamp
                            } else item.timestamp
                        } else item.timestamp
*/
            }

            inner class TableRowAdapter : RecyclerView.Adapter<TableRowAdapter.TableRowViewHolder>() {

                fun getParent(): ItemListViewModel.ItemViewModel {
                    return items[this@ItemElementViewHolder.adapterPosition]
                }

                override fun getItemViewType(position: Int): Int {
                    if (this@ItemElementViewHolder.adapterPosition != -1 && getParent().getItemValueOf(viewModel.attributes[position].localId) != null)
                        return viewModel.attributes[position].getHelper().getViewForItemListContainerType()
                    else return 5
                }

                override fun onBindViewHolder(holder: TableRowViewHolder, position: Int) {
                    holder.bind(viewModel.attributes[position])
                }

                override fun getItemCount(): Int {
                    return viewModel.attributes.size
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableRowViewHolder {

                    val view = LayoutInflater.from(parent.context).inflate(when (viewType) {
                        OTAttributeManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_SINGLELINE -> R.layout.item_attribute_row_singleline
                        OTAttributeManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE -> R.layout.item_attribute_row_multiline
                        else -> R.layout.item_attribute_row_singleline
                    }, parent, false)

                    return if (viewType == 5) {
                        NoValueTableRowViewHolder(view as ViewGroup)
                    } else TableRowViewHolder(view as ViewGroup)
                }

                inner open class TableRowViewHolder(val view: ViewGroup) : RecyclerView.ViewHolder(view), View.OnClickListener {

                    val attributeNameView: TextView by bindView(R.id.ui_attribute_name)
                    var valueView: View

                    var valueApplySubscription: Disposable? = null

                    var attributeLocalId: String? = null

                    init {
                        valueView = view.findViewById(R.id.ui_value_view_replace)
                        view.setOnClickListener(this)
                    }

                    override fun onClick(v: View?) {
                        try {

                            val item = getParent()
                            AttributeEditDialogFragment.makeInstance(item.itemId!!, attributeLocalId!!, viewModel.trackerId, this@ItemBrowserActivity)
                                    .show(this@ItemBrowserActivity.supportFragmentManager, "ValueModifyDialog")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    open fun bindRawValue(name: String, value: String) {
                        attributeNameView.text = name
                        val newValueView: TextView = if (valueView is TextView) {
                            valueView as TextView
                        } else {
                            TextView(this@ItemBrowserActivity)
                        }

                        InterfaceHelper.setTextAppearance(newValueView, R.style.viewForItemListTextAppearance)

                        changeNewValueView(newValueView)

                        newValueView.text = value
                    }

                    open fun bind(attribute: OTAttributeDAO) {
                        attributeNameView.text = attribute.name
                        attributeLocalId = attribute.localId

                        val sort = viewModel.currentSorter
                        attributeNameView.setTextColor(
                                if (sort is AFieldValueSorter && sort.attributeLocalId === attribute.localId) {
                                    ContextCompat.getColor(this@ItemBrowserActivity, R.color.colorAccent)
                                } else ContextCompat.getColor(this@ItemBrowserActivity, R.color.textColorLight)
                        )

                        val itemValue = getParent().getItemValueOf(attribute.localId)
                        if (itemValue != null) {
                            val newValueView = attribute.getHelper().getViewForItemList(attribute, this@ItemBrowserActivity, valueView)
                            changeNewValueView(newValueView)

                            valueApplySubscription?.dispose()
                            valueApplySubscription = attribute.getHelper().applyValueToViewForItemList(attribute, itemValue, valueView).subscribe({
                                valueApplySubscription = null
                            }, {
                                valueApplySubscription = null
                            })
                            startSubscriptions.add(valueApplySubscription)
                        } else {
                            //empty value
                            val emptyValueView = if (valueView is TextView) {
                                valueView as TextView
                            } else {
                                TextView(this@ItemBrowserActivity)
                            }
                            emptyValueView.setTextColor(ContextCompat.getColor(this@ItemBrowserActivity, R.color.colorRed_Light))
                            emptyValueView.text = getString(R.string.msg_empty_value)
                            changeNewValueView(emptyValueView)
                        }
                    }

                    private fun changeNewValueView(newValueView: View) {
                        if (newValueView !== valueView) {
                            val lp = valueView.layoutParams

                            val container = valueView.parent as ViewGroup

                            val index = container.indexOfChild(valueView)
                            container.removeViewInLayout(valueView)
                            newValueView.layoutParams = lp
                            container.addView(newValueView, index)
                            valueView = newValueView
                        }
                    }
                }

                inner class NoValueTableRowViewHolder(view: ViewGroup) : TableRowViewHolder(view) {
                    init {
                        if (valueView is TextView) {
                            (valueView as TextView).text = getString(R.string.msg_empty_value)
                            (valueView as TextView).setTextColor(ContextCompat.getColor(this@ItemBrowserActivity, R.color.colorRed_Light))
                        }
                    }

                    override fun bind(attributeDao: OTAttributeDAO) {
                        attributeLocalId = attributeDao.localId
                        attributeNameView.text = attributeDao.name

                        val sort = viewModel.currentSorter
                        attributeNameView.setTextColor(
                                if (sort is AFieldValueSorter && sort.attributeLocalId === attributeDao.localId) {
                                    ContextCompat.getColor(this@ItemBrowserActivity, R.color.colorAccent)
                                } else ContextCompat.getColor(this@ItemBrowserActivity, R.color.textColorLight)
                        )
                    }
                }
            }
        }
    }

    class SettingsDialogFragment : DismissingBottomSheetDialogFragment(R.layout.fragment_items_settings) {

        companion object {

            const val REQUEST_CODE_FILE_LOCATION_PICK = 300

            fun getInstance(): BottomSheetDialogFragment {
                val fragment = SettingsDialogFragment()
                return fragment
            }
        }

        private lateinit var viewModel: ItemListViewModel

        private var listView: RecyclerView by Delegates.notNull()

        private var dialogSubscriptions = CompositeSubscription()

        private val menuAdapter = Adapter()

        private lateinit var purgeMenuItem: RecyclerViewMenuAdapter.MenuItem

        private lateinit var deletionMenuItem: RecyclerViewMenuAdapter.MenuItem

        private lateinit var exportMenuItem: RecyclerViewMenuAdapter.MenuItem

        private var exportConfigIncludeFile: Boolean = false
        private var exportConfigTableFileType: OTTableExportService.TableFileType = OTTableExportService.TableFileType.CSV

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)
            viewModel = ViewModelProviders.of(activity).get(ItemListViewModel::class.java)

            dialogSubscriptions.add(
                    viewModel.sortedItemsObservable.subscribe { items ->
                        val count = items.count()
                        println("item count: ${count}")
                        if (count > 0) {
                            deletionMenuItem.description = "Item count: ${count}"
                            deletionMenuItem.isEnabled = true

                            exportMenuItem.isEnabled = true
                        } else {
                            deletionMenuItem.description = "No items"
                            deletionMenuItem.isEnabled = false

                            exportMenuItem.isEnabled = false
                        }
                        menuAdapter.notifyItemChanged(1)
                        menuAdapter.notifyItemChanged(2)
                    }
            )

            refreshPurgeButton()
        }

        override fun onViewStateRestored(savedInstanceState: Bundle?) {
            super.onViewStateRestored(savedInstanceState)
            if (savedInstanceState != null) {
                try {
                    exportConfigIncludeFile = savedInstanceState.getBoolean(OTTableExportService.EXTRA_EXPORT_CONFIG_INCLUDE_FILE, false)
                    exportConfigTableFileType = OTTableExportService.TableFileType.valueOf(savedInstanceState.getString(OTTableExportService.EXTRA_EXPORT_CONFIG_TABLE_FILE_TYPE))
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putBoolean(OTTableExportService.EXTRA_EXPORT_CONFIG_INCLUDE_FILE, exportConfigIncludeFile)
            outState.putString(OTTableExportService.EXTRA_EXPORT_CONFIG_TABLE_FILE_TYPE, exportConfigTableFileType.toString())
        }

        override fun onDismiss(dialog: DialogInterface?) {
            super.onDismiss(dialog)

            dialogSubscriptions.clear()
        }

        private fun refreshPurgeButton() {
            val cacheSize = this.viewModel.trackerDao.getTotalCacheFileSize(context)
            if (cacheSize > 0L) {
                purgeMenuItem.isEnabled = true
                purgeMenuItem.description = "${(cacheSize / (1024 * 102.4f) + .5f).toInt() / 10f} Mb"
            } else {
                purgeMenuItem.isEnabled = false
                purgeMenuItem.description = getString(R.string.msg_no_cache)
            }
            menuAdapter.notifyItemChanged(0)
        }

        override fun setupDialogAndContentView(dialog: Dialog, contentView: View) {

            purgeMenuItem = RecyclerViewMenuAdapter.MenuItem(R.drawable.clear_cache, context.getString(R.string.msg_purge_cache), null, isEnabled = false, onClick = {
                val cacheDir = viewModel.trackerDao.getItemCacheDir(context, false)
                if (cacheDir != null) {
                    println("purge cache dir files")
                    /*
                    RxProgressDialog.Builder(FileHelper.removeAllFilesIn(cacheDir).toObservable()).create(this@SettingsDialogFragment.activity).show().subscribe {
                        refreshPurgeButton()
                    }*/
                    FileHelper.deleteDirectory(cacheDir)
                    refreshPurgeButton()
                }
            })

            deletionMenuItem = RecyclerViewMenuAdapter.MenuItem(R.drawable.trashcan, context.getString(R.string.msg_remove_all_the_items_permanently), null, isEnabled = false, onClick = {
                //TODO remove all items
            })

            exportMenuItem = RecyclerViewMenuAdapter.MenuItem(R.drawable.icon_cloud_download, getString(R.string.msg_export_to_file_tracker),
                    description = getString(R.string.msg_desc_export_to_file_tracker), isEnabled = false, onClick = {
                println("export item clicked.")


                viewModel.trackerDao.let {

                    val configDialog = OTTableExportService.makeConfigurationDialog(context, it) { includeFile, tableFileType ->
                        exportConfigIncludeFile = includeFile
                        exportConfigTableFileType = tableFileType

                        val extension = if (includeFile) {
                            "zip"
                        } else tableFileType.extension
                        val intent = FileHelper.makeSaveLocationPickIntent("omnitrack_export_${it.name}_${SimpleDateFormat("yyyyMMddHHmmss").format(Date())}.$extension")

                        if (includeFile) {
                            val currentNetworkConnectionInfo = NetworkHelper.getCurrentNetworkConnectionInfo()
                            if (currentNetworkConnectionInfo.mobileConnected && !currentNetworkConnectionInfo.wifiConnected) {
                                DialogHelper.makeYesNoDialogBuilder(this@SettingsDialogFragment.context, "OmniTrack", getString(R.string.msg_export_warning_mobile_network), R.string.msg_export, onYes = {
                                    this@SettingsDialogFragment.startActivityForResult(intent, ItemBrowserActivity.SettingsDialogFragment.REQUEST_CODE_FILE_LOCATION_PICK)
                                })
                                        .show()
                            } else {
                                this@SettingsDialogFragment.startActivityForResult(intent, ItemBrowserActivity.SettingsDialogFragment.REQUEST_CODE_FILE_LOCATION_PICK)
                            }
                        } else {
                            this@SettingsDialogFragment.startActivityForResult(intent, ItemBrowserActivity.SettingsDialogFragment.REQUEST_CODE_FILE_LOCATION_PICK)
                        }
                    }
                    configDialog.show()

                    /*
                    val extension = if (it.attributes.unObservedList.find { attr -> attr.isExternalFile } != null) {
                        "zip"
                    } else "csv"

                    val intent = FileHelper.makeSaveLocationPickIntent("omnitrack_export_${it.name}_${SimpleDateFormat("yyyyMMddHHmmss").format(Date())}.$extension")

                    if (it.attributes.unObservedList.find { it.isExternalFile == true } != null) {
                        val currentNetworkConnectionInfo = NetworkHelper.getCurrentNetworkConnectionInfo()
                        if (currentNetworkConnectionInfo.mobileConnected && !currentNetworkConnectionInfo.wifiConnected) {
                            DialogHelper.makeYesNoDialogBuilder(this@SettingsDialogFragment.context, "OmniTrack", getString(R.string.msg_export_warning_mobile_network), R.string.msg_export, onYes= {
                                this@SettingsDialogFragment.startActivityForResult(intent, ItemBrowserActivity.SettingsDialogFragment.REQUEST_CODE_FILE_LOCATION_PICK)
                            })
                                    .show()
                        } else {
                            this@SettingsDialogFragment.startActivityForResult(intent, ItemBrowserActivity.SettingsDialogFragment.REQUEST_CODE_FILE_LOCATION_PICK)
                        }
                    } else {
                        this@SettingsDialogFragment.startActivityForResult(intent, ItemBrowserActivity.SettingsDialogFragment.REQUEST_CODE_FILE_LOCATION_PICK)
                    }*/
                }
            })

            listView = contentView.findViewById(R.id.ui_list)

            listView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            listView.adapter = menuAdapter
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUEST_CODE_FILE_LOCATION_PICK) {
                if (resultCode == RESULT_OK && data != null) {
                    val exportUri = data.data
                    if (exportUri != null) {
                        println(exportUri.toString())
                        viewModel.trackerDao.objectId?.let {
                            val serviceIntent = OTTableExportService.makeIntent(this@SettingsDialogFragment.context, it, exportUri.toString(), exportConfigIncludeFile, exportConfigTableFileType)
                            this@SettingsDialogFragment.dismiss()
                            activity?.startService(serviceIntent)
                        }
                    }
                }
            }
        }

        inner class Adapter : RecyclerViewMenuAdapter() {

            //TODO hid deletion
            override fun getMenuItemAt(index: Int): MenuItem {
                return when (index) {
                    0 -> purgeMenuItem
                //1 -> deletionMenuItem
                    1 -> exportMenuItem
                    else -> purgeMenuItem
                }
            }

            override fun getItemCount(): Int {
                return 2
            }

        }
    }

}
