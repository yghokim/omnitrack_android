package kr.ac.snu.hcil.omnitrack.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.activities.AttributeDetailActivity
import kr.ac.snu.hcil.omnitrack.activities.TrackerDetailActivity
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.DragItemTouchHelperCallback
import kr.ac.snu.hcil.omnitrack.ui.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ColorPalettePropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.utils.startActivityOnDelay

/**
 * Created by younghokim on 16. 7. 29..
 */
class TrackerDetailStructureTabFragment : TrackerDetailActivity.ChildFragment() {

    lateinit private var rootScrollView: NestedScrollView

    lateinit private var attributeListView: RecyclerView
    lateinit private var attributeListAdapter: TrackerDetailStructureTabFragment.AttributeListAdapter

    lateinit private var attributeListItemTouchHelper: ItemTouchHelper

    private lateinit var namePropertyView: ShortTextPropertyView
    private lateinit var colorPropertyView: ColorPalettePropertyView
    //private lateinit var fab: FloatingActionButton

    private lateinit var contentContainer: ViewGroup

    private lateinit var newAttributePanel: ViewGroup

    private lateinit var newAttributeGrid: RecyclerView

    private lateinit var removalSnackbar: Snackbar

    private var scrollToBottomReserved = false


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_tracker_detail_structure, container, false)

        /*
        fab = rootView.findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { view ->
            removalSnackbar.dismiss()
            val dialogFragment = AttributeTypeListDialogFragment()
            dialogFragment.showDialog(fragmentManager) {
                entry ->
                tracker.attributes.add(OTAttribute.createAttribute(OmniTrackApplication.app.currentUser, "New Column", entry.typeId))
                attributeListAdapter.notifyItemInserted(tracker.attributes.size - 1)
                attributeListAdapter.clearTrashcan()
            }

        }*/
        /*
        newAttributeButton = rootView.findViewById(R.id.ui_button_new_attribute)
        newAttributeButton.setOnClickListener { view ->
            removalSnackbar.dismiss()
            val dialogFragment = AttributeTypeListDialogFragment()
            dialogFragment.showDialog(fragmentManager) {
                entry ->
                val tracker = this.tracker!!
                tracker.attributes.add(OTAttribute.createAttribute(OmniTrackApplication.app.currentUser, "New Column", entry.typeId))
                attributeListAdapter.notifyItemInserted(tracker.attributes.size - 1)
                attributeListAdapter.clearTrashcan()
            }
        }*/
        rootScrollView = rootView.findViewById(R.id.scroll_root) as NestedScrollView

        contentContainer = rootView.findViewById(R.id.ui_content_container) as ViewGroup
        newAttributePanel = rootView.findViewById(R.id.ui_new_attribute_panel) as ViewGroup

        namePropertyView = rootView.findViewById(R.id.nameProperty) as ShortTextPropertyView
        namePropertyView.title = resources.getString(R.string.msg_name)
        namePropertyView.addNewValidator("Name cannot be empty.", ShortTextPropertyView.NOT_EMPTY_VALIDATOR)

        colorPropertyView = rootView.findViewById(R.id.colorProperty) as ColorPalettePropertyView
        colorPropertyView.title = resources.getString(R.string.msg_color)

        attributeListView = rootView.findViewById(R.id.ui_attribute_list) as RecyclerView
        val layoutManager = object : LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
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


        /*
        val mRecyclerViewDragDropManager = RecyclerViewDragDropManager()
        attributeListView.adapter = mRecyclerViewDragDropManager.createWrappedAdapter(attributeListAdapter)
        mRecyclerViewDragDropManager.attachRecyclerView(attributeListView)
           */


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

        attributeListAdapter.notifyDataSetChanged()
    }


    override fun onStart() {
        super.onStart()

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
        intent.putExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE, tracker!!.attributes[position].objectId)
        startActivityOnDelay(intent)
    }

    fun scrollToBottom() {
        rootScrollView.scrollTo(0, contentContainer.measuredHeight)
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


        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

            lateinit var previewContainer: LockableFrameLayout
            lateinit var columnNameView: TextView
            lateinit var typeNameView: TextView

            lateinit var editButton: ImageButton
            lateinit var removeButton: ImageButton

            lateinit var draggableZone: View

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
                typeNameView = view.findViewById(R.id.ui_attribute_type) as TextView
                editButton = view.findViewById(R.id.ui_button_edit) as ImageButton
                removeButton = view.findViewById(R.id.ui_button_remove) as ImageButton
                draggableZone = view.findViewById(R.id.ui_drag_handle)

                editButton.setOnClickListener {
                    openAttributeDetailActivity(adapterPosition)
                }

                removeButton.setOnClickListener {
                    removed = tracker.attributes[adapterPosition]
                    removedPosition = adapterPosition
                    tracker.attributes.remove(tracker.attributes[adapterPosition])
                    notifyItemRemoved(adapterPosition)
                    showRemovalSnackbar()
                }

                draggableZone.setOnTouchListener { view, motionEvent ->
                    if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                        attributeListItemTouchHelper.startDrag(this@ViewHolder)
                    }
                    true
                }
            }

            fun bindAttribute(attribute: OTAttribute<out Any>) {
                typeNameView.text = resources.getString(attribute.typeNameResourceId)
                columnNameView.text = attribute.name

                previewContainer.alpha = 0.5f
                preview = attribute.getInputView(context, true, preview)
            }
        }

    }


    inner class GridAdapter() : RecyclerView.Adapter<GridAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.attribute_type_grid_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(OmniTrackApplication.app.supportedAttributeTypes[position])
        }

        override fun getItemCount(): Int {
            return OmniTrackApplication.app.supportedAttributeTypes.size
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
                    val typeInfo = OmniTrackApplication.app.supportedAttributeTypes[adapterPosition]
                    tracker.attributes.add(OTAttribute.createAttribute(OmniTrackApplication.app.currentUser, tracker.generateNewAttributeName(typeInfo.name, context), typeInfo.typeId))

                    attributeListAdapter.notifyItemInserted(tracker.attributes.size - 1)
                    scrollToBottomReserved = true
                }
            }

            fun bind(entry: OTAttribute.AttributeTypeInfo) {
                name.text = entry.name
                typeIcon.setImageResource(entry.iconId)
            }
        }
    }
}