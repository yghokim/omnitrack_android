package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.AttributePresetInfo
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.DragItemTouchHelperCallback
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.BooleanPropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ColorPalettePropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.AttributeDetailActivity
import kr.ac.snu.hcil.omnitrack.utils.startActivityOnDelay

/**
 * Created by younghokim on 16. 7. 29..
 */
class TrackerDetailStructureTabFragment : TrackerDetailActivity.ChildFragment() {

    lateinit private var rootScrollView: NestedScrollView

    lateinit private var attributeListView: RecyclerView
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
            sender, isOnShortcut->
                tracker.isOnShortcut = isOnShortcut
        }

        attributeListView = rootView.findViewById(R.id.ui_attribute_list) as RecyclerView
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
        attributeListView.addItemDecoration(SpaceItemDecoration(LinearLayoutManager.VERTICAL, resources.getDimensionPixelOffset(R.dimen.attribute_list_element_vertical_space)));

        attributeListAdapter = AttributeListAdapter()

        attributeListView.adapter = attributeListAdapter

        attributeListItemTouchHelper = ItemTouchHelper(DragItemTouchHelperCallback(attributeListAdapter, context, true, false))
        attributeListItemTouchHelper.attachToRecyclerView(attributeListView)

        newAttributeGrid = rootView.findViewById(R.id.ui_new_attribute_grid) as RecyclerView
        newAttributeGrid.layoutManager = GridLayoutManager(context, 5)
        newAttributeGrid.adapter = GridAdapter()


        removalSnackbar = Snackbar.make(rootView, resources.getText(R.string.msg_attribute_removed_message), Snackbar.LENGTH_LONG)
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
        refresh()
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
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE, tracker!!.attributes[position].objectId)
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

            private val previewContainer: LockableFrameLayout
            private val columnNameView: TextView
            private val typeIconView: AppCompatImageView

            private val editButton: ImageButton
            private val removeButton: ImageButton

            private val requiredMarker: View

            private val draggableZone: View

            private val connectionIndicatorStub: ViewStub

            private var connectionIndicator: View? = null
            private var connectionSourceNameView: TextView? = null

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

                previewContainer = view.findViewById(R.id.ui_preview_container) as LockableFrameLayout
                previewContainer.locked = true

                columnNameView = view.findViewById(R.id.ui_column_name) as TextView
                typeIconView = view.findViewById(R.id.ui_attribute_type) as AppCompatImageView
                editButton = view.findViewById(R.id.ui_button_edit) as ImageButton
                removeButton = view.findViewById(R.id.ui_button_remove) as ImageButton
                draggableZone = view.findViewById(R.id.ui_drag_handle)
                connectionIndicatorStub = view.findViewById(R.id.ui_connection_indicator_stub) as ViewStub

                requiredMarker = view.findViewById(R.id.ui_required_marker)

                editButton.setOnClickListener(this)
                removeButton.setOnClickListener(this)
                draggableZone.setOnTouchListener(this)
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

                val connectionSource = attribute.valueConnection?.source
                if (connectionSource != null) {
                    if (connectionIndicator == null) {
                        val inflatedIndicator = connectionIndicatorStub.inflate()
                        connectionSourceNameView = inflatedIndicator.findViewById(R.id.ui_connection_source_name) as TextView
                        this.connectionIndicator = inflatedIndicator
                    } else {
                        connectionIndicator?.visibility = View.VISIBLE
                    }

                    connectionSourceNameView?.text = connectionSource.factory.getFormattedName()
                } else {
                    connectionIndicator?.visibility = View.GONE
                }
            }
        }

    }


    inner class GridAdapter() : RecyclerView.Adapter<GridAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.attribute_type_grid_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(OTApplication.app.supportedAttributePresets[position])
        }

        override fun getItemCount(): Int {
            return OTApplication.app.supportedAttributePresets.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong();
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            lateinit var name: TextView
            lateinit var typeIcon: ImageView

            init {
                name = view.findViewById(R.id.name) as TextView
                typeIcon = view.findViewById(R.id.type_icon) as ImageView

                view.setOnClickListener {
                    val typeInfo = OTApplication.app.supportedAttributePresets[adapterPosition]
                    val newAttribute = typeInfo.creater(OTApplication.app.currentUser, tracker.generateNewAttributeName(typeInfo.name, context))
                    tracker.attributes.add(newAttribute)

                    attributeListAdapter.notifyItemInserted(tracker.attributes.size - 1)
                    scrollToBottomReserved = true
                }
            }

            fun bind(entry: AttributePresetInfo) {
                name.text = entry.name
                typeIcon.setImageResource(entry.iconId)
            }
        }
    }
}