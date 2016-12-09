package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.bindView
import com.afollestad.materialdialogs.MaterialDialog
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.*
import kr.ac.snu.hcil.omnitrack.ui.DragItemTouchHelperCallback
import kr.ac.snu.hcil.omnitrack.ui.components.common.FallbackRecyclerView
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.BooleanPropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ColorPalettePropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.ui.pages.ConnectionIndicatorStubProxy
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.AttributeDetailActivity
import kr.ac.snu.hcil.omnitrack.utils.startActivityOnDelay
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

/**
 * Created by Young-Ho Kim on 16. 7. 29
 */
class TrackerDetailStructureTabFragment : TrackerDetailActivity.ChildFragment() {

    companion object {
        val toastForAdded by lazy { Toast.makeText(OTApplication.app, R.string.msg_shortcut_added, Toast.LENGTH_SHORT) }
        val toastForRemoved by lazy { Toast.makeText(OTApplication.app, R.string.msg_shortcut_removed, Toast.LENGTH_SHORT) }
    }

    lateinit private var rootScrollView: NestedScrollView

    lateinit private var attributeListView: FallbackRecyclerView
    lateinit private var attributeListAdapter: AttributeListAdapter

    lateinit private var attributeListItemTouchHelper: ItemTouchHelper

    private lateinit var namePropertyView: ShortTextPropertyView
    private lateinit var colorPropertyView: ColorPalettePropertyView
    private lateinit var isOnShortcutPropertyView: BooleanPropertyView
    //private lateinit var fab: FloatingActionButton

    private lateinit var contentContainer: ViewGroup

    private lateinit var newAttributePanel: ViewGroup

    private lateinit var newAttributeGrid: RecyclerView

    private lateinit var removalSnackbar: Snackbar

    private var scrollToBottomReserved = false

    private var permissionWaitingTypeInfo: AttributePresetInfo? = null

    private lateinit var gridAdapter: GridAdapter

    private val subscriptions = CompositeSubscription()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_tracker_detail_structure, container, false)

        rootScrollView = rootView.findViewById(R.id.scroll_root) as NestedScrollView

        contentContainer = rootView.findViewById(R.id.ui_content_container) as ViewGroup
        newAttributePanel = rootView.findViewById(R.id.ui_new_attribute_panel) as ViewGroup

        namePropertyView = rootView.findViewById(R.id.nameProperty) as ShortTextPropertyView
        namePropertyView.addNewValidator("Name cannot be empty.", ShortTextPropertyView.NOT_EMPTY_VALIDATOR)

        colorPropertyView = rootView.findViewById(R.id.colorProperty) as ColorPalettePropertyView

        colorPropertyView.valueChanged += {
            sender, colorIndex ->
            if (activity is TrackerDetailActivity) {
                (activity as TrackerDetailActivity).transitionToColor(colorPropertyView.value)
            }
        }

        isOnShortcutPropertyView = rootView.findViewById(R.id.isOnShortcutProperty) as BooleanPropertyView
        isOnShortcutPropertyView.valueChanged += {
            sender, isOnShortcut ->
            if (tracker.isOnShortcut != isOnShortcut) {
                tracker.isOnShortcut = isOnShortcut
                if (tracker.isOnShortcut) {
                    toastForAdded.show()
                } else {
                    toastForRemoved.show()
                }
            }
        }

        attributeListView = rootView.findViewById(R.id.ui_attribute_list) as FallbackRecyclerView
        attributeListView.emptyView = rootView.findViewById(R.id.ui_empty_list_message)
        val layoutManager = object : LinearLayoutManager(context, VERTICAL, false) {
            override fun canScrollVertically(): Boolean {
                return false
            }

            override fun onItemsAdded(recyclerView: RecyclerView?, positionStart: Int, itemCount: Int) {
                super.onItemsAdded(recyclerView, positionStart, itemCount)
                scrollToBottom()
            }
        }

        attributeListView.addOnLayoutChangeListener { view, a, b, c, d, e, f, g, h ->
            if (scrollToBottomReserved) {
                scrollToBottom()
                scrollToBottomReserved = false
            }
        }

        attributeListView.layoutManager = layoutManager
        attributeListView.itemAnimator = SlideInRightAnimator()
        attributeListView.addItemDecoration(SpaceItemDecoration(LinearLayoutManager.VERTICAL, resources.getDimensionPixelOffset(R.dimen.attribute_list_element_vertical_space)))


        newAttributeGrid = rootView.findViewById(R.id.ui_new_attribute_grid) as RecyclerView
        newAttributeGrid.layoutManager = GridLayoutManager(context, resources.getInteger(R.integer.new_attribute_panel_horizontal_count))
        gridAdapter = GridAdapter()
        newAttributeGrid.adapter = gridAdapter


        val snackBarContainer: CoordinatorLayout = rootView.findViewById(R.id.ui_snackbar_container) as CoordinatorLayout
        removalSnackbar = Snackbar.make(snackBarContainer, resources.getText(R.string.msg_attribute_removed_message), Snackbar.LENGTH_LONG)
        removalSnackbar.setAction(resources.getText(R.string.msg_undo)) {
            view ->
            attributeListAdapter.undoRemove()
        }

        return rootView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attributeListAdapter = AttributeListAdapter()

        attributeListView.adapter = attributeListAdapter

        attributeListItemTouchHelper = ItemTouchHelper(DragItemTouchHelperCallback(attributeListAdapter, context, true, false))
        attributeListItemTouchHelper.attachToRecyclerView(attributeListView)

        subscriptions.add(
                Observable.defer { Observable.just(makeAttributePresets()) }
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            presets ->
                            gridAdapter.presets = presets
                            gridAdapter.notifyDataSetChanged()
                        }
        )

        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        subscriptions.clear()
    }

    fun refresh() {
        if (isEditMode) {
            //edit
        } else {
            //new mode
            namePropertyView.focus()
        }

        namePropertyView.value = tracker.name
        colorPropertyView.value = tracker.color
        isOnShortcutPropertyView.value = tracker.isOnShortcut

        attributeListAdapter.notifyDataSetChanged()
    }


    override fun onStart() {
        super.onStart()
        if (activity.intent.hasExtra(TrackerDetailActivity.INTENT_KEY_FOCUS_ATTRIBUTE_ID)) {
            val attrId = activity.intent.getStringExtra(TrackerDetailActivity.INTENT_KEY_FOCUS_ATTRIBUTE_ID)
            for (attr in tracker.attributes.unObservedList.withIndex()) {
                if (attr.value.objectId == attrId) {
                    scrollToBottomReserved = true
                    break
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onPause() {
        super.onPause()
        if (namePropertyView.validate()) {
            tracker.name = namePropertyView.value
        }

        if (colorPropertyView.validate()) {
            tracker.color = colorPropertyView.value
        }
    }

    fun showRemovalSnackbar() {
        if (!removalSnackbar.isShown) {
            removalSnackbar.show()
        }
    }


    fun openAttributeDetailActivity(position: Int) {
        val intent = Intent(activity, AttributeDetailActivity::class.java)
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE, tracker.attributes[position].objectId)
        startActivityOnDelay(intent)
    }

    fun scrollToBottom() {
        newAttributePanel.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        println(newAttributePanel.measuredHeight)
        rootScrollView.scrollTo(0, contentContainer.measuredHeight - newAttributePanel.measuredHeight)
    }


    inner class AttributeListAdapter() : RecyclerView.Adapter<AttributeListAdapter.ViewHolder>(), DragItemTouchHelperCallback.ItemDragHelperAdapter {

        private var removed: OTAttribute<out Any>? = null
        private var removedPosition: Int = -1

        init {
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.attribute_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindAttribute(tracker.attributes[position])
        }

        override fun getItemCount(): Int {
            return tracker.attributes.size
        }

        override fun getItemId(position: Int): Long {
            return tracker.attributes[position].objectId.toLong()
        }

        fun undoRemove() {
            if (removed != null) {
                tracker.attributes.addAt(removed!!, removedPosition)
                notifyItemInserted(removedPosition)
            }
        }

        fun clearTrashcan() {
            removed = null
        }

        override fun onMoveItem(fromPosition: Int, toPosition: Int) {
            tracker.attributes.moveItem(fromPosition, toPosition)
            notifyItemMoved(fromPosition, toPosition)
        }

        /*
        override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean {
            println("check can drop $draggingPosition $dropPosition")
            return true
        }

        override fun onCheckCanStartDrag(holder: AttributeListAdapter.ViewHolder?, position: Int, x: Int, y: Int): Boolean {
            println("check can start drag $position $x, $y")
            return true
        }

        override fun onGetItemDraggableRange(holder: AttributeListAdapter.ViewHolder?, position: Int): ItemDraggableRange? {
            return null
        }*/

        override fun onRemoveItem(position: Int) {
        }


        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnTouchListener {

            private val previewContainer: LockableFrameLayout by bindView(R.id.ui_preview_container)
            private val columnNameView: TextView by bindView(R.id.ui_column_name)
            private val typeIconView: AppCompatImageView by bindView(R.id.ui_attribute_type)

            private val editButton: ImageButton by bindView(R.id.ui_button_edit)
            private val removeButton: ImageButton by bindView(R.id.ui_button_remove)

            private val requiredMarker: View by bindView(R.id.ui_required_marker)

            private val draggableZone: View by bindView(R.id.ui_drag_handle)

            private val columnNameButton: View by bindView(R.id.ui_column_name_button)

            private val connectionIndicatorStubProxy: ConnectionIndicatorStubProxy

            private var connectionIndicator: View? = null
            private var connectionSourceNameView: TextView? = null

            private val columnNameChangeDialog: MaterialDialog.Builder

            var preview: AAttributeInputView<out Any>? = null
                get
                set(value) {
                    if (field !== value) {
                        previewContainer.removeAllViews()
                        if (value != null) {
                            previewContainer.addView(value, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                        }

                        field = value
                    }
                }

            init {

                previewContainer.locked = true

                connectionIndicatorStubProxy = ConnectionIndicatorStubProxy(view, R.id.ui_connection_indicator_stub)

                editButton.setOnClickListener(this)
                removeButton.setOnClickListener(this)
                draggableZone.setOnTouchListener(this)
                columnNameButton.setOnClickListener(this)

                columnNameChangeDialog = MaterialDialog.Builder(this.view.context)
                        .title(R.string.msg_change_field_name)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .inputRangeRes(1, 20, R.color.colorRed)
                        .cancelable(true)
                        .negativeText(R.string.msg_cancel)
            }

            override fun onClick(view: View) {
                if (view === editButton) {
                    openAttributeDetailActivity(adapterPosition)
                } else if (view === removeButton) {
                    removed = tracker.attributes[adapterPosition]
                    removedPosition = adapterPosition
                    tracker.attributes.remove(tracker.attributes[adapterPosition])
                    notifyItemRemoved(adapterPosition)
                    showRemovalSnackbar()
                } else if (view === columnNameButton) {
                    columnNameChangeDialog
                            .input(null, tracker.attributes[adapterPosition].name, false) {
                                dialog, input ->
                                if (tracker.attributes[adapterPosition].name.compareTo(input.toString()) != 0) {
                                    tracker.attributes[adapterPosition].name = input.toString()
                                    attributeListAdapter.notifyItemChanged(adapterPosition)
                                }
                            }.show()
                }
            }

            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {

                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    attributeListItemTouchHelper.startDrag(this@ViewHolder)
                    return true
                } else return false
            }

            fun bindAttribute(attribute: OTAttribute<out Any>) {
                typeIconView.setImageResource(attribute.typeSmallIconResourceId)
                columnNameView.text = attribute.name
                requiredMarker.visibility = if (attribute.isRequired) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }

                previewContainer.alpha = 0.5f
                preview = attribute.getInputView(context, true, preview)

                connectionIndicatorStubProxy.onBind(attribute)
            }
        }

    }

    protected fun addNewAttribute(typeInfo: AttributePresetInfo) {
        val newAttribute = typeInfo.creater(OTApplication.app.currentUser, tracker.generateNewAttributeName(typeInfo.name, context))
        tracker.attributes.add(newAttribute)

        attributeListAdapter.notifyItemInserted(tracker.attributes.size - 1)
        scrollToBottomReserved = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.filter { it != PackageManager.PERMISSION_GRANTED }.isEmpty()) {
            //granted
            if (permissionWaitingTypeInfo != null) {
                addNewAttribute(permissionWaitingTypeInfo!!)
                permissionWaitingTypeInfo = null
            }
        }
    }

    private fun makeAttributePresets(): Array<AttributePresetInfo> {
        return arrayOf(
                SimpleAttributePresetInfo(OTAttribute.TYPE_SHORT_TEXT, R.drawable.field_icon_shorttext, this.getString(R.string.type_shorttext_name), this.getString(R.string.type_shorttext_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_LONG_TEXT, R.drawable.field_icon_longtext, this.getString(R.string.type_longtext_name), this.getString(R.string.type_longtext_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_NUMBER, R.drawable.field_icon_number, this.getString(R.string.type_number_name), this.getString(R.string.type_number_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_RATING, R.drawable.field_icon_rating, this.getString(R.string.type_rating_name), this.getString(R.string.type_rating_desc)),
                //                SimpleAttributePresetInfo(OTAttribute.TYPE_TIME, R.drawable.field_icon_time, this.getString(R.string.type_timepoint_name), this.getString(R.string.type_timepoint_desc)),

                AttributePresetInfo(OTAttribute.TYPE_TIME, R.drawable.field_icon_time_hour, this.getString(R.string.type_timepoint_time_name), this.getString(R.string.type_timepoint_time_desc),
                        { user, columnName ->
                            val attr = OTAttribute.createAttribute(user, columnName, OTAttribute.TYPE_TIME) as OTTimeAttribute
                            attr.granularity = OTTimeAttribute.GRANULARITY_MINUTE
                            attr
                        }),

                AttributePresetInfo(OTAttribute.TYPE_TIME, R.drawable.field_icon_time_date, this.getString(R.string.type_timepoint_date_name), this.getString(R.string.type_timepoint_date_desc),
                        { user, columnName ->
                            val attr = OTAttribute.createAttribute(user, columnName, OTAttribute.TYPE_TIME) as OTTimeAttribute
                            attr.granularity = OTTimeAttribute.GRANULARITY_DAY
                            attr
                        }),


                SimpleAttributePresetInfo(OTAttribute.TYPE_TIMESPAN, R.drawable.field_icon_timer, this.getString(R.string.type_timespan_name), this.getString(R.string.type_timespan_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_LOCATION, R.drawable.field_icon_location, this.getString(R.string.type_location_name), this.getString(R.string.type_location_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_IMAGE, R.drawable.field_icon_image, this.getString(R.string.type_image_name), this.getString(R.string.type_image_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_AUDIO, R.drawable.field_icon_audio, this.getString(R.string.type_audio_record_name), this.getString(R.string.type_audio_record_desc)),

                AttributePresetInfo(OTAttribute.TYPE_CHOICE, R.drawable.field_icon_singlechoice, this.getString(R.string.type_single_choice_name), this.getString(R.string.type_single_choice_desc),
                        { user, columnName ->
                            val attr = OTAttribute.createAttribute(user, columnName, OTAttribute.TYPE_CHOICE) as OTChoiceAttribute
                            attr.allowedMultiSelection = false
                            attr
                        }),

                AttributePresetInfo(OTAttribute.TYPE_CHOICE, R.drawable.field_icon_multiplechoice, this.getString(R.string.type_multiple_choices_name), this.getString(R.string.type_multiple_choices_desc),
                        { user, columnName ->
                            val attr = OTAttribute.createAttribute(user, columnName, OTAttribute.TYPE_CHOICE) as OTChoiceAttribute
                            attr.allowedMultiSelection = true
                            attr
                        })

        )
    }


    inner class GridAdapter() : RecyclerView.Adapter<GridAdapter.ViewHolder>() {

        var presets: Array<AttributePresetInfo>? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.attribute_type_grid_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (presets != null) {
                holder.bind(presets!![position])
            }
        }

        override fun getItemCount(): Int {
            return presets?.size ?: 0
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView
            val typeIcon: ImageView

            init {
                name = view.findViewById(R.id.name) as TextView
                typeIcon = view.findViewById(R.id.type_icon) as ImageView

                view.setOnClickListener {
                    val typeInfo = presets?.get(adapterPosition)
                    if (typeInfo != null) {
                        val requiredPermissions = OTAttribute.getPermissionsForAttribute(typeInfo.typeId)
                        if (requiredPermissions != null) {
                            permissionWaitingTypeInfo = typeInfo
                            requestPermissions(requiredPermissions, 10)
                        } else {
                            addNewAttribute(typeInfo)
                        }
                    }
                }
            }

            fun bind(entry: AttributePresetInfo) {
                name.text = entry.name
                typeIcon.setImageResource(entry.iconId)
            }
        }
    }
}