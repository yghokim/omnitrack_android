package kr.ac.snu.hcil.omnitrack.activities

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView

/**
 * Created by younghokim on 16. 7. 24..
 */
class NewItemActivity : MultiButtonActionBarActivity(R.layout.activity_new_item) {

    private val attributeListAdapter = AttributeListAdapter()

    private var tracker: OTTracker? = null

    private var builder: OTItemBuilder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rightActionBarButton?.visibility = View.VISIBLE
        leftActionBarButton?.visibility = View.VISIBLE
        leftActionBarButton?.setImageResource(R.drawable.back_rhombus)
        rightActionBarButton?.setImageResource(R.drawable.done)

        val listView = findViewById(R.id.ui_attribute_list) as RecyclerView
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        listView.layoutManager = layoutManager
        listView.addItemDecoration(SpaceItemDecoration(LinearLayoutManager.VERTICAL, resources.getDimensionPixelOffset(R.dimen.attribute_list_element_vertical_space)));

        listView.adapter = attributeListAdapter
    }

    override fun onStart() {
        super.onStart()

        val trackerId = intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        if (trackerId != null) {
            tracker = OmniTrackApplication.app.currentUser.trackers.unObservedList.find { it.objectId == trackerId }
            if (tracker != null) {
                title = String.format(resources.getString(R.string.title_activity_new_item), tracker?.name)

                //TODO check pended builder and retreive it if exists.

            }
        }

    }

    override fun onLeftButtonClicked() {
        //back button
    }

    override fun onRightButtonClicked() {
        //push item to db
    }


    inner class AttributeListAdapter : RecyclerView.Adapter<AttributeListAdapter.ViewHolder>() {

        inner class ViewHolder(val inputView: AAttributeInputView<out Any>, val frame: View) : RecyclerView.ViewHolder(frame) {

            private lateinit var columnNameView: TextView
            private lateinit var attributeTypeView: TextView


            init {
                columnNameView = frame.findViewById(R.id.title) as TextView
                attributeTypeView = frame.findViewById(R.id.ui_attribute_type) as TextView
                val container = frame.findViewById(R.id.inputViewContainer) as ViewGroup
                container.addView(inputView)
            }

            fun bind(attribute: OTAttribute<out Any>) {
                //inputView.bindAttribute(attribute)
                attribute.refreshInputViewContents(inputView)
                columnNameView.text = attribute.name
                attributeTypeView.text = resources.getString(attribute.typeNameResourceId)
            }
        }

        fun getItem(position: Int): OTAttribute<out Any> {
            return tracker?.attributes?.get(position) ?: throw IllegalAccessException("Tracker is not attached to the activity.")
        }

        override fun getItemViewType(position: Int): Int {
            return getItem(position).getInputViewType(false)
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): AttributeListAdapter.ViewHolder {

            val frame = LayoutInflater.from(this@NewItemActivity).inflate(R.layout.attribute_input_frame, parent, false);

            return ViewHolder(AAttributeInputView.makeInstance(viewType, this@NewItemActivity), frame)
        }

        override fun onBindViewHolder(holder: AttributeListAdapter.ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun getItemCount(): Int {
            return tracker?.attributes?.size ?: 0
        }

    }


}