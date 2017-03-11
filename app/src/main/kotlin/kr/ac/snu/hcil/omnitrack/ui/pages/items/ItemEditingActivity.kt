package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.bindView
import com.tbruyelle.rxpermissions.RxPermissions
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper
import kr.ac.snu.hcil.omnitrack.core.system.OTTrackingNotificationManager
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.activities.OTTrackerAttachedActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.LoadingIndicatorBar
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.pages.ConnectionIndicatorStubProxy
import rx.Observable
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 24
 */
class ItemEditingActivity : OTTrackerAttachedActivity(R.layout.activity_new_item), OTItemBuilder.AttributeStateChangedListener {

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
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, item.objectId)
            return intent
        }
    }


    private enum class Mode {
        Edit, New
    }

    private val attributeListAdapter = AttributeListAdapter()

    private lateinit var builder: OTItemBuilder

    private val attributeListView: RecyclerView by bindView(R.id.ui_attribute_list)
    private val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    private val attributeValueExtractors = Hashtable<String, () -> Any>()

    private var skipViewValueCaching = false

    private var mode: Mode = Mode.New

    private var activityResultAppliedAttributePosition = -1

    private lateinit var builderRestoredSnackbar: Snackbar

    private val loadingIndicatorBar : LoadingIndicatorBar by bindView(R.id.ui_loading_indicator)

    private var itemSaved: Boolean = false

    private val startSubscriptions = CompositeSubscription()
    private val resumeSubscriptions = CompositeSubscription()

    private val initialValueSnapshot = Hashtable<String, Any>()
    private val snapshot = Hashtable<String, Any>()

    override fun onSessionLogContent(contentObject: Bundle) {
        super.onSessionLogContent(contentObject)
        contentObject.putString("mode", mode.name)
        if (isFinishing) {
            contentObject.putBoolean("item_saved", itemSaved)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(MultiButtonActionBarActivity.Mode.SaveCancel)

        attributeListView.layoutManager = layoutManager
        attributeListView.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, this))
        (attributeListView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        loadingIndicatorBar.setMessage(R.string.msg_indicator_message_item_autocomplete)

        builderRestoredSnackbar = Snackbar.make(findViewById(R.id.ui_snackbar_container), resources.getText(R.string.msg_builder_restored), Snackbar.LENGTH_INDEFINITE)
        builderRestoredSnackbar.setAction(resources.getText(R.string.msg_clear_form)) {
            view ->
            builder = OTItemBuilder(tracker!!, OTItemBuilder.MODE_FOREGROUND)

            val requiredPermissions = tracker!!.getRequiredPermissions()
            val rxPermissionObservable = if (requiredPermissions.isNotEmpty()) {
                RxPermissions(this).request(*requiredPermissions)
            } else {
                Observable.just(true)
            }
            creationSubscriptions.add(
                    rxPermissionObservable
                            .flatMap {
                                approved ->
                                if (approved) {
                                    builder.autoComplete(this).doOnSubscribe {
                                        loadingIndicatorBar.show()
                                    }.doOnCompleted { loadingIndicatorBar.dismiss() }
                                } else Observable.error(Exception("required permission not accepted."))
                            }.subscribe({
                        println("Finished builder autocomplete.")
                        snapshot(builder)
                        snapshotInitialValue(builder)
                    }, {
                        //TODO handle permission not granted
                    })
            )

            builderRestoredSnackbar.dismiss()
        }
    }

    override fun onTrackerLoaded(tracker: OTTracker) {
        super.onTrackerLoaded(tracker)

        val requiredPermissions = tracker.getRequiredPermissions()
        val rxPermissionObservable = if (requiredPermissions.isNotEmpty()) {
            RxPermissions(this).request(*requiredPermissions)
        } else Observable.just(true)

        title = String.format(resources.getString(R.string.title_activity_new_item), tracker.name)

        fun onModeSet() {
            if (mode == Mode.New && activityResultAppliedAttributePosition == -1) {
                if (tryRestoreItemBuilderCache(tracker)) {

                    clearBuilderCache()

                    //Toast.makeText(this, "Past inputs were restored.", Toast.LENGTH_SHORT).show()
                    builderRestoredSnackbar.show()
                    snapshot(builder)
                } else {
                    //new builder was created
                    println("Start builder autocomplete")
                    creationSubscriptions.add(
                            rxPermissionObservable.flatMap {
                                approved: Boolean ->
                                if (approved) {
                                    builder.autoComplete(this).doOnSubscribe {
                                        loadingIndicatorBar.show()
                                    }.doOnCompleted { loadingIndicatorBar.dismiss() }
                                } else Observable.error(Exception("required permission not accepted."))
                            }.subscribe({
                                println("Finished builder autocomplete.");
                                snapshot(builder)
                                snapshotInitialValue(builder)
                            }, {
                                //TODO handle when permission not accepted.
                            })
                    )
                }
            }

            attributeListView.adapter = attributeListAdapter
        }

        if (intent.hasExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM)) {
            //contains item. Edit mode
            creationSubscriptions.add(
                    FirebaseDbHelper.getItem(tracker, intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM)).subscribe {
                        item ->
                        if (item != null) {
                            mode = Mode.Edit
                            println("started activity with edit mode")
                            builder = OTItemBuilder(item, tracker)
                            onModeSet()
                        } else {
                            mode = Mode.New
                            onModeSet()
                        }
                    }
            )
        } else {
            mode = Mode.New
            onModeSet()
        }

        //if(intent.hasExtra(INTENT_EXTRA_REMINDER_TIME))
        //{
        //TODO need more rich design for Notifications. Currently, dismiss all notifications for the tracker when opening the activity.
        //this is from the reminder.
        OTTrackingNotificationManager.notifyReminderChecked(tracker.objectId, intent.getLongExtra(INTENT_EXTRA_REMINDER_TIME, 0))
        //}

        //when the activity is NOT started by startActivityWithResult()
    }

    override fun onPause() {
        super.onPause()

        resumeSubscriptions.clear()

        for (inputView in attributeListAdapter.inputViews) {
            inputView.onPause()
        }


        if (mode == Mode.New) {
            if (!skipViewValueCaching) {
                if (needsToCacheBuilder())
                    storeItemBuilderCache()
                //Toast.makeText(this, "Filled form content was stored.", Toast.LENGTH_SHORT).show()

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

    override fun onStop() {
        super.onStop()
        println("startSubscriptions has subscriptoin: ${startSubscriptions.hasSubscriptions()}")
        startSubscriptions.clear()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        for (inputView in attributeListAdapter.inputViews) {
            inputView.onLowMemory()
        }
    }

    override fun onToolbarLeftButtonClicked() {
        //back button
        itemSaved = false
        finish()
    }

    override fun onToolbarRightButtonClicked() {
        //push item to db
        //syncViewStateToBuilderAsync {
            val item = builder.makeItem(OTItem.LoggingSource.Manual)
            println("Will push $item")

            FirebaseDbHelper.saveItem(item, tracker!!)
            builder.clear()
            clearBuilderCache()
            skipViewValueCaching = true
            itemSaved = true
        setResult(RESULT_OK, Intent().putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, item.objectId))
            finish()
        //}
    }

    private fun makeTrackerPreferenceKey(tracker: OTTracker): String {
        return "tracker_${tracker.objectId}"
    }

    private fun snapshot(builder: OTItemBuilder) {
        println("snapshot now")
        snapshot.clear()
        for (key in builder.keys) {
            snapshot[key] = builder.getValueWithKey(key)
        }
    }

    private fun snapshotInitialValue(builder: OTItemBuilder) {
        println("snapshot now")
        initialValueSnapshot.clear()
        for (key in builder.keys) {
            initialValueSnapshot[key] = builder.getValueWithKey(key)
        }
    }

    private fun needsToCacheBuilder(): Boolean {
        if (tracker?.attributes?.unObservedList?.find { (it.valueConnection != null && it.isConnectionValid(null)) || !it.isAutoCompleteValueStatic } != null) {
            return true
        } else {
            //there is no connection, and every field is static
            return tracker?.attributes?.unObservedList?.find {
                it.valueConnection == null && it.isAutoCompleteValueStatic &&
                        (builder.getValueWithKey(it.objectId) != initialValueSnapshot[it.objectId])
            } != null
        }
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

        Observable.merge(waitingAttributes.map {
            it.getAutoCompleteValue().map {
                value ->
                Pair<OTAttribute<out Any>, Any>(it, value!!)
            }
        }).subscribe({
            result ->
            builder.setValueOf(result.first, result.second)
        },
                Throwable::printStackTrace,
                {
                    finished?.invoke()
                })
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
            val storedBuilder = OTItemBuilder.getDeserializedInstanceWithTracker(serialized, tracker)
            /*
            if (activityResultAppliedAttributePosition != -1) {
                storedBuilder.setValueOf(tracker.attributes[activityResultAppliedAttributePosition],
                        builder.getValueInformationOf(tracker.attributes[activityResultAppliedAttributePosition])!!.value)
            }*/
            if (storedBuilder == null) {
                println("new ItemBuilder created.")
                builder = OTItemBuilder(tracker, OTItemBuilder.MODE_FOREGROUND)
                return false
            } else {
                builder = storedBuilder
                return true
            }
        } catch(e: Exception) {
            e.printStackTrace()
            println("deserialization failed. make new itemBuilder.")

            println("new ItemBuilder created.")
            builder = OTItemBuilder(tracker, OTItemBuilder.MODE_FOREGROUND)
            return false
        }
    }

    override fun onAttributeStateChanged(attribute: OTAttribute<*>, position: Int, state: OTItemBuilder.EAttributeValueState) {
        println("attribute ${attribute.name} state was changed to $state, thread: ${Thread.currentThread().name}")
        runOnUiThread { attributeListAdapter.notifyItemChanged(position) }
    }


    private fun onAttributeValueChangedHandler(attributeId: String, newVal: Any) {
        println("Attribute $attributeId was changed to $newVal")
        val attribute = tracker?.attributes?.unObservedList?.find { it.objectId == attributeId }
        if (attribute != null) {
            println("set item builder ${attribute.typeId}, ${attributeId}")
            builder.setValueOf(attribute, newVal)

            builderRestoredSnackbar.dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        println("activityResult")
        super.onActivityResult(requestCode, resultCode, data)
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

            private val columnNameView: TextView by bindView(R.id.ui_column_name)
            private val requiredMarker: View by bindView(R.id.ui_required_marker)
            private val attributeTypeView: TextView by bindView(R.id.ui_attribute_type)

            private val container: LockableFrameLayout by bindView(R.id.ui_input_view_container)

            private val timestampIndicator: TextView by bindView(R.id.ui_timestamp)

            private val connectionIndicatorStubProxy: ConnectionIndicatorStubProxy

            private val loadingIndicatorInContainer: View by bindView(R.id.ui_container_indicator)

            private var attributeId: String? = null


            init {

                container.addView(inputView, 0)

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

                println("current builder attribute state: ${builder.getAttributeValueState(adapterPosition)}")

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