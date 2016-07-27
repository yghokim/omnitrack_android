package kr.ac.snu.hcil.omnitrack.activities

import android.content.Context
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
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import java.util.*

/**
 * Created by younghokim on 16. 7. 24..
 */
class NewItemActivity : MultiButtonActionBarActivity(R.layout.activity_new_item) {

    private val attributeListAdapter = AttributeListAdapter()

    private var tracker: OTTracker? = null

    private var builder: OTItemBuilder? = null

    private val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    private val attributeValueExtractors = Hashtable<String, () -> Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rightActionBarButton?.visibility = View.VISIBLE
        leftActionBarButton?.visibility = View.VISIBLE
        leftActionBarButton?.setImageResource(R.drawable.back_rhombus)
        rightActionBarButton?.setImageResource(R.drawable.done)

        val listView = findViewById(R.id.ui_attribute_list) as RecyclerView
        listView.layoutManager = layoutManager
        listView.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, this))

        listView.adapter = attributeListAdapter
    }

    override fun onStart() {
        super.onStart()

        attributeValueExtractors.clear()

        val trackerId = intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        if (trackerId != null) {
            tracker = OmniTrackApplication.app.currentUser.trackers.unObservedList.find { it.objectId == trackerId }
            if (tracker != null) {
                title = String.format(resources.getString(R.string.title_activity_new_item), tracker?.name)

                if (tryRestoreItemBuilderCache(tracker!!)) {

                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        tryRestoreItemBuilderCache(tracker!!)
    }

    override fun onPause() {
        super.onPause()
        storeItemBuilderCache()
    }

    override fun onLeftButtonClicked() {
        //back button
    }

    override fun onRightButtonClicked() {
        //push item to db
    }

    private fun makeTrackerPreferenceKey(tracker: OTTracker): String {
        return "tracker_${tracker.objectId}"
    }

    private fun storeItemBuilderCache() {
        if (tracker != null) {

            for (attribute in tracker!!.attributes.unObservedList) {
                builder?.setValueOf(attribute, attributeValueExtractors[attribute.objectId]?.invoke() ?: attribute.makeDefaultValue())
            }

            println(builder?.getSerializedString())

            val preferences = getSharedPreferences(OmniTrackApplication.PREFERENCE_KEY_FOREGROUND_ITEM_BUILDER_STORAGE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(makeTrackerPreferenceKey(tracker!!), builder?.getSerializedString())
            editor.apply()
        }
    }

    private fun tryRestoreItemBuilderCache(tracker: OTTracker): Boolean {
        val preferences = getSharedPreferences(OmniTrackApplication.PREFERENCE_KEY_FOREGROUND_ITEM_BUILDER_STORAGE, Context.MODE_PRIVATE)
        val serialized = preferences.getString(makeTrackerPreferenceKey(tracker), null)
        if (serialized != null) {
            builder = OTItemBuilder(serialized)
            return true
        } else {
            builder = OTItemBuilder(tracker, OTItemBuilder.MODE_FOREGROUND)
            return false
        }
    }

    private fun onAttributeValueChanged(attributeId: String, newVal: Any) {

    }

    inner class AttributeListAdapter : RecyclerView.Adapter<AttributeListAdapter.ViewHolder>() {

        inner class ViewHolder(val inputView: AAttributeInputView<out Any>, val frame: View) : RecyclerView.ViewHolder(frame) {

            private lateinit var columnNameView: TextView
            private lateinit var attributeTypeView: TextView

            private var attributeId: String? = null


            init {
                columnNameView = frame.findViewById(R.id.title) as TextView
                attributeTypeView = frame.findViewById(R.id.ui_attribute_type) as TextView
                val container = frame.findViewById(R.id.inputViewContainer) as ViewGroup
                container.addView(inputView)

                inputView.valueChanged += {
                    sender, newVal ->
                    if (attributeId != null)
                        onAttributeValueChanged(attributeId!!, newVal)
                }
            }

            fun bind(attribute: OTAttribute<out Any>) {
                attribute.refreshInputViewContents(inputView)
                attributeId = attribute.objectId
                columnNameView.text = attribute.name
                attributeTypeView.text = resources.getString(attribute.typeNameResourceId)

                if (builder?.hasValueOf(attribute) ?: false) {
                    inputView.valueChanged.suspend = true

                    if (builder != null) {
                        println("apply itembuilder value to inputView")
                        inputView.setAnyValue(builder!!.getValueOf(attribute)!!)
                    }
                    inputView.valueChanged.suspend = false

                }

                attributeValueExtractors[attributeId] = {
                    inputView.value
                }
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