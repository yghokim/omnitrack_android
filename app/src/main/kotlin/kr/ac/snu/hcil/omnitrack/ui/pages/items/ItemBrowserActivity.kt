package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.ToggleButton
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTTimeAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.ItemComparator
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseManager
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.services.OTTableExportService
import kr.ac.snu.hcil.omnitrack.ui.DragItemTouchHelperCallback
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.activities.OTTrackerAttachedActivity
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
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

class ItemBrowserActivity : OTTrackerAttachedActivity(R.layout.activity_item_browser), ExtendedSpinner.OnItemSelectedListener, View.OnClickListener, AttributeEditDialogFragment.Listener {

    companion object {

        const val REQUEST_CODE_NEW_ITEM = 151
        const val REQUEST_CODE_EDIT_ITEM = 150

        fun makeIntent(tracker: OTTracker, context: Context): Intent {
            val intent = Intent(context, ItemBrowserActivity::class.java)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            return intent
        }
    }

    private val items = ArrayList<OTItem>()

    private val itemListView: FallbackRecyclerView by bindView(R.id.ui_item_list)

    private lateinit var itemListViewAdapter: ItemListViewAdapter

    private val emptyListMessageView: TextView by bindView(R.id.ui_empty_list_message)

    private val sortSpinner: ExtendedSpinner by bindView(R.id.ui_spinner_sort_method)

    private val sortOrderButton: ToggleButton by bindView(R.id.ui_toggle_sort_order)

    private lateinit var removalSnackbar: Snackbar

    private var supportedItemComparators: List<ItemComparator>? = null

    private val settingsFragment: BottomSheetDialogFragment? get() {
        if (tracker != null)
            return SettingsDialogFragment.getInstance(tracker!!)
        else return null
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
        removalSnackbar.setAction(resources.getText(R.string.msg_undo)) {
            view ->
            itemListViewAdapter.undoRemoval()
        }.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                itemListViewAdapter.clearTrashcan()
            }

        })
    }

    override fun onTrackerLoaded(tracker: OTTracker) {
        super.onTrackerLoaded(tracker)

        println("Cache size: ${tracker.getTotalCacheFileSize(this)}")

        items.clear()
        title = String.format(resources.getString(R.string.title_activity_item_browser, tracker.name))

        supportedItemComparators = tracker.getSupportedComparators()

        if (supportedItemComparators != null) {
            val adapter = ArrayAdapter<ItemComparator>(this, R.layout.simple_list_element_text_light, R.id.textView, supportedItemComparators)
            adapter.setDropDownViewResource(R.layout.simple_list_element_text_dropdown)

            sortSpinner.adapter = adapter
        }

        creationSubscriptions.add(
                OTApplication.app.databaseManager.loadItems(tracker).subscribe { items ->
                    this.items.clear()
                    this.items.addAll(items)
                    reSort()
                }
        )
    }

    override fun onOkAttributeEditDialog(changed: Boolean, value: Any, tracker: OTTracker, attribute: OTAttribute<out Any>, itemId: String?) {
        println("dismiss handler")
        Log.d(AttributeEditDialogFragment.TAG, "changed: ${changed}, value: ${value}")
        if (this.tracker?.objectId == tracker.objectId) {
            if (itemId != null) {
                val item = items.find { item -> item.objectId == itemId }
                if (item != null) {
                    item.setValueOf(attribute, value)
                    creationSubscriptions.add(
                            DatabaseManager.saveItem(item, tracker, false).observeOn(AndroidSchedulers.mainThread()).subscribe { success ->
                                if (success)
                                    itemListViewAdapter.notifyItemChanged(items.indexOf(item))
                            }
                    )
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

    private fun reSort(refresh: Boolean = true) {
        val comparator = supportedItemComparators?.get(sortSpinner.selectedItemPosition)
        if (comparator != null) {
            println("sort items by ${comparator}")
            comparator.isDecreasing = !sortOrderButton.isChecked
            items.sortWith(comparator)
            if (refresh)
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
    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {
        if (tracker != null) {
            val intent = ItemEditingActivity.makeIntent(tracker!!.objectId, this)
            intent.putExtra(OTApplication.INTENT_EXTRA_FROM, this@ItemBrowserActivity.javaClass.simpleName)
            startActivityForResult(intent, REQUEST_CODE_NEW_ITEM)
        }
    }

    override fun onToolbarRightSubButtonClicked() {
        settingsFragment?.show(supportFragmentManager, "TRACKER_ITEMS_SETTINGS")
    }

    fun deleteItemPermanently(position: Int): OTItem {
        val removedItem = items[position]
        OTApplication.app.databaseManager.removeItem(removedItem)
        //items.removeAt(position)
        onItemRemoved(position)

        return removedItem
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

        private val removedItems = ArrayList<OTItem>()

        fun clearTrashcan() {
            for (item in removedItems) {
                OTApplication.app.databaseManager.removeItem(item)
            }
        }

        override fun onMoveItem(fromPosition: Int, toPosition: Int) {

        }

        override fun onRemoveItem(position: Int) {
            removedItems += items.removeAt(position)
            notifyItemRemoved(position)
            removalSnackbar.show()
        }

        fun undoRemoval() {
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
            }
        }

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

                itemMenu = PopupMenu(this@ItemBrowserActivity, moreButton, Gravity.TOP or Gravity.LEFT)
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
                        val intent = ItemEditingActivity.makeIntent(items[adapterPosition], tracker!!, this@ItemBrowserActivity)
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


            fun bindItem(item: OTItem) {
                val cal = Calendar.getInstance()
                val currentSorter = getCurrentSort()

                cal.timeInMillis =
                        if (currentSorter is AFieldValueSorter && currentSorter.attribute is OTTimeAttribute) {
                            if (item.hasValueOf(currentSorter.attribute)) {
                                (item.getValueOf(currentSorter.attribute) as TimePoint).timestamp
                            } else item.timestamp
                        } else item.timestamp


                monthView.text = String.format(Locale.US, "%tb", cal)
                dayView.text = cal.getDayOfMonth().toString()

                sourceView.text = item.source.sourceText
                loggingTimeView.text = OTTimeAttribute.formats[OTTimeAttribute.GRANULARITY_MINUTE]!!.format(Date(item.timestamp))

                valueListAdapter.notifyDataSetChanged()
            }

            inner class TableRowAdapter : RecyclerView.Adapter<TableRowAdapter.TableRowViewHolder>() {

                fun getParentItem(): OTItem {
                    return items[this@ItemElementViewHolder.adapterPosition]
                }

                override fun getItemViewType(position: Int): Int {
                    if (this@ItemElementViewHolder.adapterPosition != -1 && getParentItem().hasValueOf(tracker!!.attributes[position]))
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

                inner open class TableRowViewHolder(val view: ViewGroup) : RecyclerView.ViewHolder(view), View.OnClickListener {

                    val attributeNameView: TextView by bindView(R.id.ui_attribute_name)
                    var valueView: View

                    var valueApplySubscription: Subscription? = null

                    var attributeId: String? = null

                    init {
                        valueView = view.findViewById(R.id.ui_value_view_replace)
                        view.setOnClickListener(this)
                    }

                    override fun onClick(v: View?) {
                        try {
                            val item = getParentItem()
                            AttributeEditDialogFragment.makeInstance(item.objectId!!, attributeId!!, item.trackerId, this@ItemBrowserActivity)
                                    .show(this@ItemBrowserActivity.supportFragmentManager, "ValueModifyDialog")

                        } catch(e: Exception) {
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

                    open fun bindAttribute(attribute: OTAttribute<out Any>) {
                        attributeNameView.text = attribute.name
                        attributeId = attribute.objectId
                        val sort = getCurrentSort()
                        attributeNameView.setTextColor(
                                if (sort is AFieldValueSorter && sort.attribute === attribute) {
                                    ContextCompat.getColor(this@ItemBrowserActivity, R.color.colorAccent)
                                } else ContextCompat.getColor(this@ItemBrowserActivity, R.color.textColorLight)
                        )

                        val newValueView = attribute.getViewForItemList(this@ItemBrowserActivity, valueView)
                        changeNewValueView(newValueView)

                        valueApplySubscription?.unsubscribe()
                        if (getParentItem().hasValueOf(attribute)) {
                            valueApplySubscription = attribute.applyValueToViewForItemList(getParentItem().getValueOf(attribute), valueView).subscribe({
                                valueApplySubscription = null
                            }, {
                                valueApplySubscription = null
                            })
                            startSubscriptions.add(valueApplySubscription)
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

                    override fun bindAttribute(attribute: OTAttribute<out Any>) {
                        attributeId = attribute.objectId
                        attributeNameView.text = attribute.name
                    }
                }
            }
        }
    }

    class SettingsDialogFragment : DismissingBottomSheetDialogFragment(R.layout.fragment_items_settings) {

        companion object {

            const val REQUEST_CODE_FILE_LOCATION_PICK = 300

            fun getInstance(tracker: OTTracker): BottomSheetDialogFragment {
                val arguments = Bundle()
                arguments.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)

                val fragment = SettingsDialogFragment()
                fragment.arguments = arguments

                return fragment
            }
        }

        private var listView: RecyclerView by Delegates.notNull()

        private var user: OTUser? = null
        private var tracker: OTTracker? = null

        private var dialogSubscriptions = CompositeSubscription()

        private val menuAdapter = Adapter()

        private lateinit var purgeMenuItem: RecyclerViewMenuAdapter.MenuItem

        private lateinit var deletionMenuItem: RecyclerViewMenuAdapter.MenuItem

        private lateinit var exportMenuItem: RecyclerViewMenuAdapter.MenuItem

        private var exportConfigIncludeFile: Boolean = false
        private var exportConfigTableFileType: OTTableExportService.TableFileType = OTTableExportService.TableFileType.CSV

        override fun onViewStateRestored(savedInstanceState: Bundle?) {
            super.onViewStateRestored(savedInstanceState)
            if (savedInstanceState != null) {
                try {
                    exportConfigIncludeFile = savedInstanceState.getBoolean(OTTableExportService.EXTRA_EXPORT_CONFIG_INCLUDE_FILE, false)
                    exportConfigTableFileType = OTTableExportService.TableFileType.valueOf(savedInstanceState.getString(OTTableExportService.EXTRA_EXPORT_CONFIG_TABLE_FILE_TYPE))
                } catch(ex: Exception) {
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
            user = null
            tracker = null
        }

        private fun refreshPurgeButton() {
            val cacheSize = this.tracker?.getTotalCacheFileSize(context) ?: 0L
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
                val cacheDir = tracker?.getItemCacheDir(context, false)
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


                tracker?.let {

                    val configDialog = OTTableExportService.makeConfigurationDialog(context, it) {
                        includeFile, tableFileType ->
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

            if (arguments != null) {
                val activity = activity
                if (activity is OTActivity) {
                    dialogSubscriptions.add(
                            activity.signedInUserObservable.subscribe {
                                user ->
                                this.user = user
                                this.tracker = user[arguments.getString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)]

                                refreshPurgeButton()

                                if (tracker != null) {
                                    dialogSubscriptions.add(
                                            OTApplication.app.databaseManager.getTotalItemCount(tracker!!).subscribe {
                                                count ->
                                                if (count.first > 0) {
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
                                }
                            })
                }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUEST_CODE_FILE_LOCATION_PICK) {
                if (resultCode == RESULT_OK && data != null) {
                    val exportUri = data.data
                    if (exportUri != null) {
                        println(exportUri.toString())
                        tracker?.let {
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
