package kr.ac.snu.hcil.omnitrack.activities

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.ui.DragItemTouchHelperCallback
import kr.ac.snu.hcil.omnitrack.ui.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.AttributeFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.AttributeTypeListDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ColorPalettePropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import java.util.*

class TrackerDetailActivity : MultiButtonActionBarActivity(R.layout.activity_tracker_detail) {

    private var isEditMode = false

    private lateinit var tracker: OTTracker

    lateinit private var listView : RecyclerView
    lateinit private var attributeListAdapter : AttributeListAdapter

    private lateinit var namePropertyView : ShortTextPropertyView
    private lateinit var colorPropertyView: ColorPalettePropertyView

    private lateinit var attributeListGroupView: View
    private lateinit var fab: FloatingActionButton

    private lateinit var removalSnackbar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)
        fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { view ->
            removalSnackbar.dismiss()
            val dialogFragment = AttributeTypeListDialogFragment()
            dialogFragment.showDialog(supportFragmentManager) {
                entry ->
                tracker.attributes.add(OTAttribute.createAttribute(OmniTrackApplication.app.currentUser, "New Column", entry.typeId))
                attributeListAdapter.notifyItemInserted(tracker.attributes.size - 1)
                attributeListAdapter.clearTrashcan()
            }

        }

        namePropertyView = findViewById(R.id.nameProperty) as ShortTextPropertyView
        namePropertyView.title = resources.getString(R.string.msg_name)
        namePropertyView.addNewValidator("Name cannot be empty.", ShortTextPropertyView.NOT_EMPTY_VALIDATOR)

        colorPropertyView = findViewById(R.id.colorProperty) as ColorPalettePropertyView
        colorPropertyView.title = resources.getString(R.string.msg_color)

        attributeListGroupView = findViewById(R.id.ui_group_attribute_list) as View

        listView = findViewById(R.id.ui_attribute_list) as RecyclerView
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        listView.layoutManager = layoutManager
        listView.itemAnimator = SlideInRightAnimator()
        listView.addItemDecoration(SpaceItemDecoration(LinearLayoutManager.VERTICAL, resources.getDimensionPixelOffset(R.dimen.attribute_list_element_vertical_space)));

        attributeListAdapter = AttributeListAdapter()
        listView.adapter = attributeListAdapter
        val listItemTouchHelper = ItemTouchHelper(DragItemTouchHelperCallback(attributeListAdapter))
        listItemTouchHelper.attachToRecyclerView(listView)


        removalSnackbar = Snackbar.make(findViewById(R.id.root)!!, resources.getText(R.string.msg_attribute_removed_message), Snackbar.LENGTH_LONG)
        removalSnackbar.setAction(resources.getText(R.string.msg_undo)) {
            view ->
            attributeListAdapter.undoRemove()
        }
    }


    override fun onPause(){
        super.onPause()

        if(isEditMode) {
            if (namePropertyView.validate()) {
                tracker.name = namePropertyView.value
            }

            if(colorPropertyView.validate()){
                tracker.color = colorPropertyView.value
            }

            OmniTrackApplication.app.syncUserToDb()
        }
    }

    override fun onStart(){
        super.onStart()

        if (intent.getStringExtra("trackerId") != null) {
            //edit
            //instant update

            setActionBarButtonMode(Mode.Back)

            isEditMode = true
            title = resources.getString(R.string.title_activity_tracker_edit)
            tracker = OmniTrackApplication.app.currentUser.trackers.filter{ it.objectId == intent.getStringExtra("trackerId") }.first()

            attributeListGroupView.visibility = View.VISIBLE
            fab.visibility = View.VISIBLE

        } else {
            //new mode
            isEditMode = false

            namePropertyView.focus()

            setActionBarButtonMode(Mode.OKCancel)

            title = resources.getString(R.string.title_activity_tracker_new)

            tracker = OTTracker("New Tracker")

            attributeListGroupView.visibility = View.GONE
            fab.visibility = View.GONE
        }


        namePropertyView.value = tracker.name
        colorPropertyView.value = tracker.color

        attributeListAdapter.notifyDataSetChanged()
    }

    fun showRemovalSnackbar() {
        if (!removalSnackbar.isShown) {
            removalSnackbar.show()
        }
    }

    override fun onLeftButtonClicked() {
        if(isEditMode)
            finish()
        else{
            navigateUpTo(parentActivityIntent)
        }
    }

    override fun onRightButtonClicked() {
            //add
            if(namePropertyView.validate()) {
                //modify
                tracker.name = namePropertyView.value
                tracker.color = colorPropertyView.value

                if (!isEditMode) OmniTrackApplication.app.currentUser.trackers.add(tracker)
                finish()
            }
    }

    fun openAttributeDetailActivity(position: Int) {
        val intent = Intent(this, AttributeDetailActivity::class.java)
        intent.putExtra("attributeId", tracker.attributes[position].objectId)
        startActivity(intent)
    }

    inner class AttributeListAdapter() : RecyclerView.Adapter<AttributeListAdapter.ViewHolder>(), DragItemTouchHelperCallback.ItemDragHelperAdapter {

        private var removed: OTAttribute<out Any>? = null
        private var removedPosition: Int = -1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.attribute_list_element, parent, false) as AttributeFrameLayout
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            //holder.bindTracker(tracker.attributes[position])
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

            init{
                view.editButtonClicked += {
                    sender, args ->
                    openAttributeDetailActivity(adapterPosition)
                }
            }

            fun bindAttribute(attribute: OTAttribute<out Any>) {
                view.typeNameView.text = resources.getString(attribute.typeNameResourceId)
                view.columnNameView.text = attribute.name

                val preview = attribute.getInputView(this@TrackerDetailActivity, view.preview)
                preview.previewMode = true
                view.preview = preview
            }
        }
    }
}
