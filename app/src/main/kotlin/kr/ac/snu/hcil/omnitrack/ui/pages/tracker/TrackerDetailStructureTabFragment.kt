package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import butterknife.bindView
import com.afollestad.materialdialogs.MaterialDialog
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.AttributePresetInfo
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
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

    private lateinit var removalSnackbar: Snackbar

    private lateinit var newAttributeButton: FloatingActionButton

    private var scrollToBottomReserved = false

    private val subscriptions = CompositeSubscription()

    private var tracker: OTTracker? = null

    private val newAttributePanel: FieldPresetSelectionBottomSheetFragment by lazy {

        val newAttributePanel = FieldPresetSelectionBottomSheetFragment()
        newAttributePanel.callback = object : FieldPresetSelectionBottomSheetFragment.Callback {
            override fun onAttributePermittedToAdd(typeInfo: AttributePresetInfo) {
                addNewAttribute(typeInfo)
            }
        }

        newAttributePanel
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_tracker_detail_structure, container, false)

        rootScrollView = rootView.findViewById(R.id.scroll_root) as NestedScrollView

        contentContainer = rootView.findViewById(R.id.ui_content_container) as ViewGroup

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
            if (tracker?.isOnShortcut != isOnShortcut) {
                tracker?.isOnShortcut = isOnShortcut
                if (tracker?.isOnShortcut == true) {
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

        //val snackBarContainer: CoordinatorLayout = rootView.findViewById(R.id.ui_snackbar_container) as CoordinatorLayout
        removalSnackbar = Snackbar.make(rootView, resources.getText(R.string.msg_attribute_removed_message), Snackbar.LENGTH_LONG)
        removalSnackbar.setAction(resources.getText(R.string.msg_undo)) {
            view ->
            attributeListAdapter.undoRemove()
        }

        newAttributeButton = rootView.findViewById(R.id.ui_button_new_attribute) as FloatingActionButton

        newAttributeButton.setOnClickListener {
            newAttributePanel.show(this@TrackerDetailStructureTabFragment.fragmentManager, newAttributePanel.tag)
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
    }

    fun refresh() {
        if (isEditMode) {
            //edit
        } else {
            //new mode
            namePropertyView.focus()
        }

        namePropertyView.value = tracker?.name ?: ""
        colorPropertyView.value = tracker?.color ?: Color.BLACK
        isOnShortcutPropertyView.value = tracker?.isOnShortcut ?: false

        attributeListAdapter.notifyDataSetChanged()
    }


    override fun onStart() {
        super.onStart()

        subscriptions.add(
                OTApplication.app.currentUserObservable.subscribe {
                    user ->
                    if (trackerObjectId != null) {
                        tracker = user[trackerObjectId!!]
                    }

                    val tracker = tracker
                    tracker?.let {

                        if (activity.intent.hasExtra(TrackerDetailActivity.INTENT_KEY_FOCUS_ATTRIBUTE_ID)) {
                            val focusedAttributeId = activity.intent.getStringExtra(TrackerDetailActivity.INTENT_KEY_FOCUS_ATTRIBUTE_ID)

                            for (attr in tracker.attributes.unObservedList) {
                                if (attr.objectId == focusedAttributeId) {
                                    scrollToBottomReserved = true
                                    break
                                }
                            }
                        }
                    }

                    refresh()
                }
        )
    }

    override fun onStop() {
        super.onStop()
        subscriptions.clear()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onPause() {
        super.onPause()
        if (namePropertyView.validate()) {
            tracker?.name = namePropertyView.value
        }

        if (colorPropertyView.validate()) {
            tracker?.color = colorPropertyView.value
        }
    }

    fun showRemovalSnackbar() {
        if (!removalSnackbar.isShown) {
            removalSnackbar.show()
        }
    }


    fun openAttributeDetailActivity(position: Int) {
        val intent = Intent(activity, AttributeDetailActivity::class.java)
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE, tracker?.attributes?.get(position)?.objectId)
        startActivityOnDelay(intent)
    }

    fun scrollToBottom() {
        rootScrollView.smoothScrollTo(0, contentContainer.measuredHeight)
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
            holder.bindAttribute(tracker!!.attributes[position])
        }

        override fun getItemCount(): Int {
            return tracker?.attributes?.size ?: 0
        }

        override fun getItemId(position: Int): Long {
            return tracker?.attributes?.get(position)?.objectId?.toLong() ?: -1
        }

        fun undoRemove() {
            if (removed != null) {
                tracker?.attributes?.addAt(removed!!, removedPosition)
                notifyItemInserted(removedPosition)
            }
        }

        fun clearTrashcan() {
            removed = null
        }

        override fun onMoveItem(fromPosition: Int, toPosition: Int) {
            tracker?.attributes?.moveItem(fromPosition, toPosition)
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

                previewContainer.setOnClickListener(this)
            }

            override fun onClick(view: View) {
                if (view === editButton || view === previewContainer) {
                    openAttributeDetailActivity(adapterPosition)
                } else if (view === removeButton) {
                    val tracker = tracker
                    tracker?.let {
                        removed = tracker.attributes[adapterPosition]
                        removedPosition = adapterPosition
                        tracker.attributes.remove(tracker.attributes[adapterPosition])
                        notifyItemRemoved(adapterPosition)
                        showRemovalSnackbar()
                    }
                } else if (view === columnNameButton) {
                    val tracker = tracker
                    tracker?.let {
                        columnNameChangeDialog
                                .input(null, tracker?.attributes[adapterPosition].name, false) {
                                    dialog, input ->
                                    if (tracker.attributes[adapterPosition].name.compareTo(input.toString()) != 0) {
                                        tracker.attributes[adapterPosition].name = input.toString()
                                        attributeListAdapter.notifyItemChanged(adapterPosition)
                                    }
                                }.show()
                    }
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
        val tracker = tracker
        tracker?.let {
            val newAttribute = typeInfo.creater(OTApplication.app.currentUser, tracker.generateNewAttributeName(typeInfo.name, context))
            tracker.attributes.add(newAttribute)

            attributeListAdapter.notifyItemInserted(tracker.attributes.size - 1)
            scrollToBottomReserved = true
        }
    }
}