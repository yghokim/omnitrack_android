package kr.ac.snu.hcil.omnitrack.activities

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
import kr.ac.snu.hcil.omnitrack.ui.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.properties.ColorPalettePropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.properties.ShortTextPropertyView

class TrackerDetailActivity : OkCancelActivity() {

    private var isEditMode = false

    private var tracker: OTTracker? = null

    lateinit private var listView : RecyclerView
    lateinit private var attributeListAdapter : AttributeListAdapter

    private lateinit var namePropertyView : ShortTextPropertyView
    private lateinit var colorPropertyView: ColorPalettePropertyView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker_detail)
        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)

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

            namePropertyView.value = tracker!!.name
            colorPropertyView.value = tracker!!.color

        } else {
            //new mode
            isEditMode = false
            title = resources.getString(R.string.title_activity_tracker_new)

            namePropertyView.focus()
        }

        attributeListAdapter = AttributeListAdapter()
        listView.adapter = attributeListAdapter
        attributeListAdapter.notifyDataSetChanged()
    }

    override fun onCanceled() {
        finish()
    }

    override fun onOk() {
        if(isEditMode)
        {
            //modify
            tracker?.name = namePropertyView.value
            finish()
        }
        else{
            //add
            if(namePropertyView.validate()) {
                val newTracker = OTTracker(namePropertyView.value)
                newTracker.color = colorPropertyView.value
                OmniTrackApplication.app.currentUser.trackers.add(newTracker)
                finish()
            }
        }
    }


    inner class AttributeListAdapter() : RecyclerView.Adapter<AttributeListAdapter.ViewHolder>(){

        private val attributes = arrayOf("Sleep Time", "Date", "Logged at", "Step Count", "Coffee Cups")

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.attribute_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            //holder.bindTracker(tracker.attributes[position])
            holder.bindAttribute(attributes[position])
        }

        override fun getItemCount(): Int {
            return attributes.size
        }

        override fun getItemId(position: Int): Long {
            return position as Long;
        }


        inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view){

            private lateinit var name: TextView

            init{
                name = view.findViewById(R.id.ui_column_name) as TextView

            }

            fun bindAttribute(value: String){
                name.text = value
            }

            fun bindAttribute(attribute: OTAttribute){
            }
        }
    }
}
