package kr.ac.snu.hcil.omnitrack.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.activities.AttributeDetailActivity
import kr.ac.snu.hcil.omnitrack.activities.TrackerDetailActivity
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.DragItemTouchHelperCallback
import kr.ac.snu.hcil.omnitrack.ui.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.AttributeFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.AttributeTypeListDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.components.TriggerPanel
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ColorPalettePropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.utils.startActivityOnDelay

/**
 * Created by younghokim on 16. 7. 29..
 */
class TrackerDetailStructureTabFragment : TrackerDetailActivity.ChildFragment() {

    companion object {
        const val IS_EDIT_MODE = "editMode"

        fun makeInstance(tracker: OTTracker, isEditMode: Boolean): TrackerDetailStructureTabFragment {
            val bundle = Bundle()
            bundle.putString(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            bundle.putBoolean(IS_EDIT_MODE, isEditMode)

            val fragment = TrackerDetailStructureTabFragment()
            fragment.arguments = bundle

            return fragment
        }
    }

    lateinit private var listView: RecyclerView
    lateinit private var attributeListAdapter: TrackerDetailStructureTabFragment.AttributeListAdapter

    private lateinit var namePropertyView: ShortTextPropertyView
    private lateinit var colorPropertyView: ColorPalettePropertyView

    private lateinit var attributeListGroupView: View
    //private lateinit var fab: FloatingActionButton
    private lateinit var newAttributeButton: View

    private lateinit var removalSnackbar: Snackbar


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
        }


        namePropertyView = rootView.findViewById(R.id.nameProperty) as ShortTextPropertyView
        namePropertyView.title = resources.getString(R.string.msg_name)
        namePropertyView.addNewValidator("Name cannot be empty.", ShortTextPropertyView.NOT_EMPTY_VALIDATOR)

        colorPropertyView = rootView.findViewById(R.id.colorProperty) as ColorPalettePropertyView
        colorPropertyView.title = resources.getString(R.string.msg_color)

        attributeListGroupView = rootView.findViewById(R.id.ui_group_attribute_list) as View


        listView = rootView.findViewById(R.id.ui_attribute_list) as RecyclerView
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listView.layoutManager = layoutManager
        listView.itemAnimator = SlideInRightAnimator()
        listView.addItemDecoration(SpaceItemDecoration(LinearLayoutManager.VERTICAL, resources.getDimensionPixelOffset(R.dimen.attribute_list_element_vertical_space)));

        attributeListAdapter = AttributeListAdapter()
        listView.adapter = attributeListAdapter
        val listItemTouchHelper = ItemTouchHelper(DragItemTouchHelperCallback(attributeListAdapter))
        listItemTouchHelper.attachToRecyclerView(listView)


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
/*
    override fun init(tracker: OTTracker, editMode: Boolean) {
        this.tracker = tracker
        this.isEditMode = editMode
    }

    override fun onClose() {
        if (namePropertyView.validate()) {
            tracker?.name = namePropertyView.value
        }

        if (colorPropertyView.validate()) {
            tracker?.color = colorPropertyView.value
        }
    }
*/

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


    inner class AttributeListAdapter() : RecyclerView.Adapter<AttributeListAdapter.ViewHolder>(), DragItemTouchHelperCallback.ItemDragHelperAdapter {

        private var removed: OTAttribute<out Any>? = null
        private var removedPosition: Int = -1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.attribute_list_element, parent, false) as AttributeFrameLayout
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindAttribute(tracker.attributes[position])
        }

        override fun getItemCount(): Int {
            return tracker.attributes.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
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

        override fun onItemMove(fromPosition: Int, toPosition: Int) {
            tracker.attributes.moveItem(fromPosition, toPosition)
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onItemDismiss(position: Int) {
            removed = tracker.attributes[position]
            removedPosition = position
            tracker.attributes.remove(tracker.attributes[position])
            notifyItemRemoved(position)
            showRemovalSnackbar()
        }

        inner class ViewHolder(val view: AttributeFrameLayout) : RecyclerView.ViewHolder(view) {

            init {
                view.editButtonClicked += {
                    sender, args ->
                    openAttributeDetailActivity(adapterPosition)
                }
            }

            fun bindAttribute(attribute: OTAttribute<out Any>) {
                view.typeNameView.text = resources.getString(attribute.typeNameResourceId)
                view.columnNameView.text = attribute.name

                view.previewContainer.alpha = 0.5f
                view.preview = attribute.getInputView(context, true, view.preview)
            }
        }

    }
}