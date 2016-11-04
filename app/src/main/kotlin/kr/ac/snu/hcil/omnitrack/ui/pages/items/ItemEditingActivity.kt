package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.pages.ConnectionIndicatorStubProxy
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 24
 */
class ItemEditingActivity : MultiButtonActionBarActivity(R.layout.activity_new_item), OTItemBuilder.AttributeStateChangedListener {

    companion object {

        const val INTENT_EXTRA_REMINDER_TIME = "reminderTime"

        fun makeIntent(trackerId: String, context: Context): Intent {
            val intent = Intent(context, ItemEditingActivity::class.java)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            return intent
        }

        fun makeIntent(trackerId: String, reminderTime: Long, context: Context): Intent {
            val intent = Intent(context, ItemEditingActivity::class.java)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            intent.putExtra(INTENT_EXTRA_REMINDER_TIME, reminderTime)
            return intent
        }

        fun makeIntent(item: OTItem, tracker: OTTracker, context: Context): Intent {

            val intent = Intent(context, ItemEditingActivity::class.java)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            intent.putExtra(OTApplication.INTENT_EXTRA_DB_ID_ITEM, item.dbId)
            return intent
        }
    }


    private enum class Mode {
        Edit, New
    }

    private val attributeListAdapter = AttributeListAdapter()

    private var tracker: OTTracker? = null

    private lateinit var builder: OTItemBuilder

    private lateinit var attributeListView: RecyclerView
    private val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    private val attributeValueExtractors = Hashtable<String, () -> Any>()

    private var skipViewValueCaching = false

    private var mode: Mode = Mode.New

    private var activityResultAppliedAttributePosition = -1

    private lateinit var builderRestoredSnackbar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rightActionBarButton?.visibility = View.VISIBLE
        leftActionBarButton?.visibility = View.VISIBLE
        leftActionBarButton?.setImageResource(R.drawable.back_rhombus)
        rightActionBarButton?.setImageResource(R.drawable.done)

        attributeListView = findViewById(R.id.ui_attribute_list) as RecyclerView
        attributeListView.layoutManager = layoutManager
        attributeListView.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, this))

        attributeListView.adapter = attributeListAdapter

        builderRestoredSnackbar = Snackbar.make(findViewById(R.id.ui_root), resources.getText(R.string.msg_builder_restored), Snackbar.LENGTH_INDEFINITE)
        builderRestoredSnackbar.setAction(resources.getText(R.string.msg_discard)) {
            view ->
            builder = OTItemBuilder(tracker!!, OTItemBuilder.MODE_FOREGROUND)
            builder.autoCompleteAsync(this) {
                //attributeListAdapter.notifyDataSetChanged()
            }
            builderRestoredSnackbar.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()

        val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        if (trackerId != null) {
            tracker = OTApplication.app.currentUser.trackers.unObservedList.find { it.objectId == trackerId }

            if (tracker != null) {
                title = String.format(resources.getString(R.string.title_activity_new_item), tracker?.name)

                if (intent.hasExtra(OTApplication.INTENT_EXTRA_DB_ID_ITEM)) {
                    //contains item. Edit mode
                    val item = OTApplication.app.dbHelper.getItem(intent.getLongExtra(OTApplication.INTENT_EXTRA_DB_ID_ITEM, -1), tracker!!)
                    if (item != null) {
                        mode = Mode.Edit
                        println("started activity with edit mode")
                        builder = OTItemBuilder(item, tracker!!)
                    } else mode = Mode.New
                } else {
                    mode = Mode.New
                }

                //if(intent.hasExtra(INTENT_EXTRA_REMINDER_TIME))
                //{
                //TODO need more rich design for Notifications. Currently, dismiss all notifications for the tracker when opening the activity.
                //this is from the reminder.
                OTNotificationManager.notifyReminderChecked(trackerId, intent.getLongExtra(INTENT_EXTRA_REMINDER_TIME, 0))
                //}

                //when the activity is NOT started by startActivityWithResult()
                if (mode == Mode.New && activityResultAppliedAttributePosition == -1) {
                    if (tryRestoreItemBuilderCache(tracker!!)) {

                        //Toast.makeText(this, "Past inputs were restored.", Toast.LENGTH_SHORT).show()
                        builderRestoredSnackbar.show()
                    } else {
                        //new builder was created
                        //TODO make it as a AcyncTask and update each attribute immediately
                        builder.autoCompleteAsync(this) {
                            //attributeListAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (mode == Mode.New) {
            if (tracker == null) {
                tryRestoreItemBuilderCache(tracker!!)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        for (inputView in attributeListAdapter.inputViews) {
            inputView.onPause()
        }

        if (mode == Mode.New) {
            if (!skipViewValueCaching) {
                storeItemBuilderCache()
                Toast.makeText(this, "Filled form content was stored.", Toast.LENGTH_SHORT).show()

            } else {
                skipViewValueCaching = false
            }
        }

        this.activityResultAppliedAttributePosition = -1
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        for (inputView in attributeListAdapter.inputViews) {
            inputView.onSaveInstanceState(outState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        for (inputView in attributeListAdapter.inputViews) {
            inputView.onDestroy()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        for (inputView in attributeListAdapter.inputViews) {
            inputView.onLowMemory()
        }
    }

    override fun onToolbarLeftButtonClicked() {
        //back button
        finish()
    }

    override fun onToolbarRightButtonClicked() {
        //push item to db
        syncViewStateToBuilderAsync {
            val item = builder.makeItem(OTItem.LoggingSource.Manual)
            println("Will push $item")

            OTApplication.app.dbHelper.save(item, tracker!!)
            builder.clear()
            clearBuilderCache()
            skipViewValueCaching = true
            finish()
        }
    }

    private fun makeTrackerPreferenceKey(tracker: OTTracker): String {
        return "tracker_${tracker.objectId}"
    }

    private fun syncViewStateToBuilderAsync(finished: (() -> Unit)?) {
        println("grab inputView's values to ItemBuilder.")
        val waitingAttributes = ArrayList<OTAttribute<out Any>>()
        for (attribute in tracker!!.attributes.unObservedList) {
            val valueExtractor = attributeValueExtractors[attribute.objectId]
            if (valueExtractor != null) {
                val value = valueExtractor()
                println("assigning value to builder.${attribute.name} : ${value}")
                builder.setValueOf(attribute, value)
            } else {
                println("value extract is null.")
                waitingAttributes.add(attribute)
            }
        }

        var remain = waitingAttributes.size
        if (remain == 0) {
            finished?.invoke()
            return
        }

        for (attribute in waitingAttributes) {
            attribute.getAutoCompleteValueAsync {
                result ->
                remain--
                builder.setValueOf(attribute, result)

                if (remain == 0) {
                    //finish
                    finished?.invoke()
                }
            }
        }
    }

    private fun clearBuilderCache() {
        if (tracker != null) {
            val preferences = getSharedPreferences(OTApplication.PREFERENCE_KEY_FOREGROUND_ITEM_BUILDER_STORAGE, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.remove(makeTrackerPreferenceKey(tracker!!))
            editor.apply()
        }
    }

    private fun storeItemBuilderCache() {
        if (tracker != null) {


//            syncViewStateToBuilderAsync {
                if (!builder.isEmpty) {

                    val preferences = getSharedPreferences(OTApplication.PREFERENCE_KEY_FOREGROUND_ITEM_BUILDER_STORAGE, Context.MODE_PRIVATE)
                    val editor = preferences.edit()
                    editor.putString(makeTrackerPreferenceKey(tracker!!), builder.getSerializedString())
                    editor.apply()
                }
            //          }

        }
    }

    private fun tryRestoreItemBuilderCache(tracker: OTTracker): Boolean {
        val preferences = getSharedPreferences(OTApplication.PREFERENCE_KEY_FOREGROUND_ITEM_BUILDER_STORAGE, Context.MODE_PRIVATE)
        val serialized = preferences.getString(makeTrackerPreferenceKey(tracker), null)
        try {
            val storedBuilder = OTItemBuilder(serialized)
            /*
            if (activityResultAppliedAttributePosition != -1) {
                storedBuilder.setValueOf(tracker.attributes[activityResultAppliedAttributePosition],
                        builder.getValueInformationOf(tracker.attributes[activityResultAppliedAttributePosition])!!.value)
            }*/
            builder = storedBuilder
            return true
        } catch(e: Exception) {
            e.printStackTrace()
            println("deserialization failed. make new itemBuilder.")

            println("new ItemBuilder created.")
            builder = OTItemBuilder(tracker, OTItemBuilder.MODE_FOREGROUND)
            return false
        }
    }

    override fun onAttributeStateChanged(attribute: OTAttribute<*>, position: Int, state: OTItemBuilder.EAttributeValueState) {
        println("attribute ${attribute.name} state was changed to $state")
        attributeListAdapter.notifyItemChanged(position)
    }


    private fun onAttributeValueChangedHandler(attributeId: String, newVal: Any) {
        println("Attribute $attributeId was changed to $newVal")
        val attribute = tracker?.attributes?.unObservedList?.find { it.objectId == attributeId }
        if (attribute != null) {
            println("set item builder ${attribute.typeId}, ${attributeId}")
            builder.setValueOf(attribute, newVal)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        println("activityResult")
        if (resultCode == Activity.RESULT_OK && data != null) {
            val attributePosition = AAttributeInputView.getPositionFromRequestCode(requestCode)
            val inputView = attributeListAdapter.inputViews.find { it.position == attributePosition }
            inputView?.setValueFromActivityResult(data, AAttributeInputView.getRequestTypeFromRequestCode(requestCode))
            activityResultAppliedAttributePosition = attributePosition
        }
    }

    inner class AttributeListAdapter : RecyclerView.Adapter<AttributeListAdapter.ViewHolder>() {

        val inputViews = HashSet<AAttributeInputView<*>>()

        fun getItem(position: Int): OTAttribute<out Any> {
            return tracker?.attributes?.get(position) ?: throw IllegalAccessException("Tracker is not attached to the activity.")
        }

        override fun getItemViewType(position: Int): Int {
            return getItem(position).getInputViewType(false)
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {

            val frame = LayoutInflater.from(this@ItemEditingActivity).inflate(R.layout.attribute_input_frame, parent, false)

            val inputView = AAttributeInputView.makeInstance(viewType, this@ItemEditingActivity)
            inputViews.add(inputView)

            inputView.onCreate(null)

            return ViewHolder(inputView, frame)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
            holder.inputView.position = position
        }

        override fun getItemCount(): Int {
            return tracker?.attributes?.size ?: 0
        }

        inner class ViewHolder(val inputView: AAttributeInputView<out Any>, frame: View) : RecyclerView.ViewHolder(frame) {

            private val columnNameView: TextView
            private val requiredMarker: View
            private val attributeTypeView: TextView

            private val container: LockableFrameLayout


            private var timestampIndicator: TextView

            private val connectionIndicatorStubProxy: ConnectionIndicatorStubProxy

            private val loadingIndicatorInContainer: View

            private var attributeId: String? = null


            init {
                columnNameView = frame.findViewById(R.id.ui_column_name) as TextView
                requiredMarker = frame.findViewById(R.id.ui_required_marker)
                attributeTypeView = frame.findViewById(R.id.ui_attribute_type) as TextView

                timestampIndicator = frame.findViewById(R.id.ui_timestamp) as TextView

                container = frame.findViewById(R.id.ui_input_view_container) as LockableFrameLayout
                container.addView(inputView, 0)

                loadingIndicatorInContainer = frame.findViewById(R.id.ui_container_indicator)

                inputView.valueChanged += {
                    sender, args ->
                    onInputViewValueChanged(args)
                }

                connectionIndicatorStubProxy = ConnectionIndicatorStubProxy(frame, R.id.ui_connection_indicator_stub)
            }

            private fun onInputViewValueChanged(newVal: Any) {
                setTimestampIndicatorText(System.currentTimeMillis())
                if (attributeId != null) {
                    onAttributeValueChangedHandler(attributeId!!, newVal)
                }
            }

            private fun setTimestampIndicatorText(timestamp: Long) {
                val now = System.currentTimeMillis()
                timestampIndicator.text = if (now - timestamp < DateUtils.SECOND_IN_MILLIS) {
                    resources.getString(R.string.time_just_now)
                } else {
                    DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL)
                }
            }

            fun bind(attribute: OTAttribute<out Any>) {
                attribute.refreshInputViewUI(inputView)

                attributeId = attribute.objectId
                columnNameView.text = attribute.name
                requiredMarker.visibility = if (attribute.isRequired) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
                attributeTypeView.text = resources.getString(attribute.typeNameResourceId)

                connectionIndicatorStubProxy.onBind(attribute)


                if (builder.hasValueOf(attribute)) {
                    val valueInfo = builder.getValueInformationOf(attribute)!!

                    inputView.valueChanged.suspend = true
                    inputView.setAnyValue(valueInfo.value)
                    inputView.valueChanged.suspend = false

                    setTimestampIndicatorText(valueInfo.timestamp)
                }

                attributeValueExtractors[attributeId] = {
                    inputView.value
                }

                inputView.boundAttributeId = attribute.objectId

                val state = builder.getAttributeValueState(adapterPosition)

                if (state == OTItemBuilder.EAttributeValueState.Idle) {
                    container.locked = false
                    inputView.alpha = 1.0f
                } else {
                    container.locked = true
                    inputView.alpha = 0.12f
                }

                when (builder.getAttributeValueState(adapterPosition)) {
                    OTItemBuilder.EAttributeValueState.Idle -> {
                        loadingIndicatorInContainer.visibility = View.GONE
                    }
                    OTItemBuilder.EAttributeValueState.Processing -> {
                        loadingIndicatorInContainer.visibility = View.VISIBLE
                    }
                    OTItemBuilder.EAttributeValueState.GettingExternalValue -> {
                        loadingIndicatorInContainer.visibility = View.VISIBLE
                    }
                }

                inputView.onResume()
            }
        }
    }
}