package kr.ac.snu.hcil.omnitrack.activities

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
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
import kr.ac.snu.hcil.omnitrack.ui.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.AttributeFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.properties.ColorPalettePropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.properties.ShortTextPropertyView

class TrackerDetailActivity : OkCancelActivity() {

    private var isEditMode = false

    private lateinit var tracker: OTTracker

    lateinit private var listView : RecyclerView
    lateinit private var attributeListAdapter : AttributeListAdapter

    private lateinit var namePropertyView : ShortTextPropertyView
    private lateinit var colorPropertyView: ColorPalettePropertyView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker_detail)
        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)

        val fab = findViewById(R.id.fab) as FloatingActionButton?
        fab!!.setOnClickListener { view ->
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            //(application as OmniTrackApplication).syncUserToDb()
            //user.trackers.add(OTTracker("Hihi"))
            tracker.attributes.add(OTNumberAttribute("Step Count"))
            attributeListAdapter.notifyItemInserted(tracker.attributes.size - 1)
        }

        namePropertyView = findViewById(R.id.nameProperty) as ShortTextPropertyView
        namePropertyView.title = resources.getString(R.string.msg_name)
        namePropertyView.addNewValidator("Name cannot be empty.", ShortTextPropertyView.NOT_EMPTY_VALIDATOR)

        colorPropertyView = findViewById(R.id.colorProperty) as ColorPalettePropertyView
        colorPropertyView.title = resources.getString(R.string.msg_color)

        listView = findViewById(R.id.ui_attribute_list) as RecyclerView
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        listView.layoutManager = layoutManager
        listView.itemAnimator = SlideInRightAnimator()
        listView.addItemDecoration(SpaceItemDecoration(LinearLayoutManager.VERTICAL, resources.getDimensionPixelOffset(R.dimen.attribute_list_element_vertical_space)));
    }


    override fun onStart(){
        super.onStart()


        if (intent.getStringExtra("trackerId") != null) {
            //edit
            isEditMode = true
            title = resources.getString(R.string.title_activity_tracker_edit)
            tracker = OmniTrackApplication.app.currentUser.trackers.filter{ it.objectId == intent.getStringExtra("trackerId") }.first()


        } else {
            //new mode
            isEditMode = false
            title = resources.getString(R.string.title_activity_tracker_new)

            tracker = OTTracker("New Tracker")
        }


        namePropertyView.value = tracker.name
        colorPropertyView.value = tracker.color

        attributeListAdapter = AttributeListAdapter()
        listView.adapter = attributeListAdapter
        attributeListAdapter.notifyDataSetChanged()
    }

    override fun onCanceled() {
        finish()
    }

    override fun onOk() {
            //add
            if(namePropertyView.validate()) {
                //modify
                tracker.name = namePropertyView.value

                if (!isEditMode) OmniTrackApplication.app.currentUser.trackers.add(tracker)
                finish()
            }
    }


    inner class AttributeListAdapter() : RecyclerView.Adapter<AttributeListAdapter.ViewHolder>(){

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


        inner class ViewHolder(val view: AttributeFrameLayout) : RecyclerView.ViewHolder(view) {

            init{

            }

            fun bindAttribute(attribute: OTAttribute<out Any>) {
                view.typeNameView.text = attribute.typeName
                view.columnNameView.text = attribute.name
            }
        }
    }
}
