package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.LoadingIndicatorBar
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.pages.ConnectionIndicatorStubProxy
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 24
 */
class ItemEditingActivity : MultiButtonActionBarActivity(R.layout.activity_new_item), OTItemBuilderWrapperBase.AttributeStateChangedListener {

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
            intent.action = "item_edit:${trackerId}"
            return intent
        }

        fun makeIntent(item: OTItem, tracker: OTTracker, context: Context): Intent {

            val intent = Intent(context, ItemEditingActivity::class.java)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, item.objectId)
            return intent
        }
    }

    private val attributeListAdapter = AttributeListAdapter()

    private lateinit var viewModel: ItemEditionViewModel

    private var builder: OTItemBuilder? = null

    private val attributeListView: RecyclerView by bindView(R.id.ui_attribute_list)
    private val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    private val attributeValueExtractors = Hashtable<String, () -> Any>()

    private var skipViewValueCaching = false

    private var mode: ItemEditionViewModel.ItemMode = ItemEditionViewModel.ItemMode.New

    private var activityResultAppliedAttributePosition = -1

    private lateinit var builderRestoredSnackbar: Snackbar

    private val loadingIndicatorBar: LoadingIndicatorBar by bindView(R.id.ui_loading_indicator)

    private val currentAttributeList = ArrayList<OTAttributeDAO>()

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

        viewModel = ViewModelProviders.of(this).get(ItemEditionViewModel::class.java)

        if (savedInstanceState == null) {
            val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
            if (trackerId != null) {
                viewModel.init(trackerId, intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM))
            }
        }

        attributeListView.layoutManager = layoutManager
        attributeListView.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, this))
        (attributeListView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        attributeListView.adapter = attributeListAdapter

        loadingIndicatorBar.setMessage(R.string.msg_indicator_message_item_autocomplete)

        builderRestoredSnackbar = Snackbar.make(findViewById(R.id.ui_snackbar_container), resources.getText(R.string.msg_builder_restored), Snackbar.LENGTH_INDEFINITE)
        builderRestoredSnackbar.setAction(resources.getText(R.string.msg_clear_form)) {
            view ->
            creationSubscriptions.add(
                    viewModel.builderWrapper.autoComplete(this).doOnSubscribe {
                        loadingIndicatorBar.show()
                    }.doOnCompleted { loadingIndicatorBar.dismiss() }
                            .subscribe {

                            }
            )
            /*
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
                                    if (builder != null) {
                                        builder!!.autoComplete(this).doOnSubscribe {
                                            loadingIndicatorBar.show()
                                        }.doOnCompleted { loadingIndicatorBar.dismiss() }
                                    } else {
                                        Observable.just(false)
                                    }
                                } else Observable.error(Exception("required permission not accepted."))
                            }.subscribe({
                        println("Finished builder autocomplete.")
                        if (builder != null) {
                            snapshot(builder!!)
                            snapshotInitialValue(builder!!)
                        }
                    }, {
                        //TODO handle permission not granted
                    })
            )

            builderRestoredSnackbar.dismiss()
            */
        }

        creationSubscriptions.add(
                viewModel.builderCreationModeObservable.subscribe { builderCreationMode ->
                    if (builderCreationMode == ItemEditionViewModel.BuilderCreationMode.Restored) {
                        builderRestoredSnackbar.show()
                        if (builder != null) {
                            snapshot(builder!!)
                        }
                    }
                }
        )

        creationSubscriptions.add(
                viewModel.modeObservable.subscribe { mode ->
                    when (mode) {
                        ItemEditionViewModel.ItemMode.Edit -> {
                            title = String.format(getString(R.string.title_activity_edit_item), viewModel.trackerDao?.name)
                        }
                        ItemEditionViewModel.ItemMode.New -> {
                            title = String.format(resources.getString(R.string.title_activity_new_item), viewModel.trackerDao?.name)
                        }
                    }
                }
        )

        creationSubscriptions.add(
                viewModel.unManagedAttributeListObservable.subscribe { list ->
                    currentAttributeList.clear()
                    currentAttributeList.addAll(list)
                    attributeListAdapter.notifyDataSetChanged()
                }
        )
    }
/*
    override fun onTrackerLoaded(tracker: OTTracker) {
        super.onTrackerLoaded(tracker)

        println("tracker attribute count: ${tracker.attributes.size}")

        val requiredPermissions = tracker.getRequiredPermissions()
        val rxPermissionObservable = if (requiredPermissions.isNotEmpty()) {
            RxPermissions(this).request(*requiredPermissions)
        } else Observable.just(true)

        title = String.format(resources.getString(R.string.title_activity_new_item), tracker.name)

        fun onModeSet() {

            when (mode) {
                ItemEditionViewModel.ItemMode.Edit -> {
                    title = String.format(getString(R.string.title_activity_edit_item), tracker.name)
                }
                ItemEditionViewModel.ItemMode.New -> {
                    title = String.format(resources.getString(R.string.title_activity_new_item), tracker.name)
                }
            }

            if (mode == ItemEditionViewModel.ItemMode.New && activityResultAppliedAttributePosition == -1) {
                if (tryRestoreItemBuilderCache(tracker)) {

                    clearBuilderCache()

                    //Toast.makeText(this, "Past inputs were restored.", Toast.LENGTH_SHORT).show()
                    builderRestoredSnackbar.show()
                    if (builder != null) {
                        snapshot(builder!!)
                    }
                } else {
                    //new builder was created
                    println("Start builder autocomplete")
                    creationSubscriptions.add(
                            rxPermissionObservable.flatMap {
                                approved: Boolean ->
                                if (approved) {
                                    if (builder != null) {
                                        builder!!.autoComplete(this).doOnSubscribe {
                                            loadingIndicatorBar.show()
                                        }.doOnCompleted { loadingIndicatorBar.dismiss() }
                                    } else {
                                        Observable.just(false)
                                    }
                                } else Observable.error(Exception("required permission not accepted."))
                            }.subscribe({
                                println("Finished builder autocomplete.")
                                if (builder != null) {
                                    snapshot(builder!!)
                                    snapshotInitialValue(builder!!)
                                }
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
                    OTApplication.app.databaseManager.getItem(tracker, intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM)).subscribe {
                        item ->
                        if (item != null) {
                            mode = ItemEditionViewModel.ItemMode.Edit
                            println("started activity with edit mode")
                            builder = OTItemBuilder(item, tracker)
                            onModeSet()
                        } else {
                            mode = ItemEditionViewModel.ItemMode.New
                            onModeSet()
                        }
                    }
            )
        } else {
            mode = ItemEditionViewModel.ItemMode.New
            onModeSet()
        }

        //if(intent.hasExtra(INTENT_EXTRA_REMINDER_TIME))
        //{
        //TODO need more rich design for Notifications. Currently, dismiss all notifications for the tracker when opening the activity.
        //this is from the reminder.
        OTTrackingNotificationManager.notifyReminderChecked(tracker.objectId, intent.getLongExtra(INTENT_EXTRA_REMINDER_TIME, 0))
        //}

        //when the activity is NOT started by startActivityWithResult()
    }*/

    override fun onPause() {
        super.onPause()

        resumeSubscriptions.clear()

        for (inputView in attributeListAdapter.inputViews) {
            inputView.onPause()
        }


        if (mode == ItemEditionViewModel.ItemMode.New) {
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

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        for (inputView in attributeListAdapter.inputViews) {
            inputView.onSaveInstanceState(outState)
        }

        outState?.putString("mode", mode.toString())
        outState?.putString("builder", builder?.getSerializedString())

    }
/*
    override fun onRestoredInstanceStateWithTracker(savedInstanceState: Bundle, tracker: OTTracker) {
        super.onRestoredInstanceStateWithTracker(savedInstanceState, tracker)
        println("restore item editing activity instance state")
        mode = ItemEditionViewModel.ItemMode.valueOf(savedInstanceState.getString("mode"))
        when (mode) {
            ItemEditionViewModel.ItemMode.Edit -> {
                title = String.format(getString(R.string.title_activity_edit_item), tracker.name)
            }
            ItemEditionViewModel.ItemMode.New -> {
                title = String.format(resources.getString(R.string.title_activity_new_item), tracker.name)
            }
        }
        println("tracker exists. get builder cache.")
        if (savedInstanceState.containsKey("builder")) {
            val builder = OTItemBuilder.getDeserializedInstanceWithTracker(savedInstanceState.getString("builder"), tracker)
            if (builder != null) {
                this.builder = builder
                attributeListAdapter.notifyDataSetChanged()
            } else {
                tryRestoreItemBuilderCache(tracker)
                attributeListAdapter.notifyDataSetChanged()
            }
        }
    }*/

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

    override fun onResume() {
        super.onResume()
        for (inputView in attributeListAdapter.inputViews) {
            inputView.onResume()
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
        builder?.let {
            val item = it.makeItem(OTItem.LoggingSource.Manual)
            println("Will push $item")

            /*
            creationSubscriptions.add(

                    OTApplication.app.databaseManager.saveItem(item, tracker!!).subscribe { success ->
                        if (success) {
                            it.clear()
                            clearBuilderCache()
                            skipViewValueCaching = true
                            itemSaved = true
                            setResult(RESULT_OK, Intent().putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, item.objectId))
                            finish()
                        } else {
                            DialogHelper.makeSimpleAlertBuilder(this, "Item Logging Failed.")
                        }
                    }
            )*/


        }
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
        /*
        if (tracker?.attributes?.unObservedList?.find { (it.valueConnection != null && it.isConnectionValid(null)) || !it.isAutoCompleteValueStatic } != null) {
            return true
        } else {
            //there is no connection, and every field is static
            return tracker?.attributes?.unObservedList?.find {
                it.valueConnection == null && it.isAutoCompleteValueStatic &&
                        (builder?.getValueWithKey(it.objectId) != initialValueSnapshot[it.objectId])
            } != null
        }*/ return false
    }

    private fun clearBuilderCache() {
        //TODO clearBuilder
    }

    private fun storeItemBuilderCache() {
        /*
        if (tracker != null) {
//            syncViewStateToBuilderAsync {
            if (builder?.isEmpty == false) {
                val preferences = getSharedPreferences(OTApplication.PREFERENCE_KEY_FOREGROUND_ITEM_BUILDER_STORAGE, Context.MODE_PRIVATE)
                val editor = preferences.edit()
                editor.putString(makeTrackerPreferenceKey(tracker!!), builder?.getSerializedString())
                editor.apply()
            }
            //          }
        }
        */
        //TODO store builder
    }

    override fun onAttributeStateChanged(attributeLocalId: String, state: OTItemBuilderWrapperBase.EAttributeValueState) {
        println("attribute ${attributeLocalId} state was changed to $state, thread: ${Thread.currentThread().name}")
        runOnUiThread { attributeListAdapter.notifyItemChanged(currentAttributeList.indexOfFirst { it.localId == attributeLocalId }) }
    }


    private fun onAttributeValueChangedHandler(attributeId: String, newVal: Any) {
        println("Attribute $attributeId was changed to $newVal")
        /*
        val attribute = tracker?.attributes?.unObservedList?.find { it.objectId == attributeId }
        if (attribute != null) {
            println("set item builder ${attribute.typeId}, ${attributeId}")
            builder?.setValueOf(attribute, newVal)

            builderRestoredSnackbar.dismiss()
        }*/
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

        fun getItem(position: Int): OTAttributeDAO {
            return currentAttributeList[position]
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
            return currentAttributeList.size
        }

        inner class ViewHolder(val inputView: AAttributeInputView<out Any>, frame: View) : RecyclerView.ViewHolder(frame) {

            private val columnNameView: TextView by bindView(R.id.ui_column_name)
            private val requiredMarker: View by bindView(R.id.ui_required_marker)
            //private val attributeTypeView: TextView by bindView(R.id.ui_attribute_type)

            private val container: LockableFrameLayout by bindView(R.id.ui_input_view_container)

            private val timestampIndicator: TextView by bindView(R.id.ui_timestamp)

            private val connectionIndicatorStubProxy: ConnectionIndicatorStubProxy

            private val loadingIndicatorInContainer: View by bindView(R.id.ui_container_indicator)

            private val optionButton: View by bindView(R.id.ui_button_option)

            private var attributeId: String? = null


            init {

                container.addView(inputView, 0)

                inputView.valueChanged += {
                    sender, args ->
                    onInputViewValueChanged(args)
                }

                connectionIndicatorStubProxy = ConnectionIndicatorStubProxy(frame, R.id.ui_connection_indicator_stub)

                optionButton.setOnClickListener {
                    /*
                    val tracker = tracker
                    val attributeId = attributeId
                    if (tracker != null && attributeId != null) {
                        val historyDialog = RecentItemValuePickerBottomSheetFragment.getInstance(tracker.objectId, attributeId)
                        historyDialog.show(supportFragmentManager, RecentItemValuePickerBottomSheetFragment.TAG)
                    }*/
                }
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

            fun bind(attribute: OTAttributeDAO) {
                attribute.getHelper().refreshInputViewUI(inputView, attribute)

                attributeId = attribute.objectId
                columnNameView.text = attribute.name
                requiredMarker.visibility = if (attribute.isRequired) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
                //attributeTypeView.text = resources.getString(attribute.typeNameResourceId)

                connectionIndicatorStubProxy.onBind(attribute.getParsedConnection())

                viewModel.builderWrapper.let {
                    if (it.hasValueOf(attribute.localId)) {
                        val valueInfo = it.getValueInformationOf(attribute.localId)!!

                        inputView.valueChanged.suspend = true
                        inputView.setAnyValue(valueInfo.value)
                        inputView.valueChanged.suspend = false

                        setTimestampIndicatorText(valueInfo.timestamp)
                    }

                    val state = it.getAttributeValueState(attribute.localId)

                    if (state == OTItemBuilderWrapperBase.EAttributeValueState.Idle) {
                        container.locked = false
                        inputView.alpha = 1.0f
                    } else {
                        container.locked = true
                        inputView.alpha = 0.12f
                    }

                    println("current builder attribute state: ${it.getAttributeValueState(attribute.localId)}, localId: ${attribute.localId}")

                    when (it.getAttributeValueState(attribute.localId)) {
                        OTItemBuilderWrapperBase.EAttributeValueState.Idle -> {
                            loadingIndicatorInContainer.visibility = View.GONE
                        }
                        OTItemBuilderWrapperBase.EAttributeValueState.Processing -> {
                            loadingIndicatorInContainer.visibility = View.VISIBLE
                        }
                        OTItemBuilderWrapperBase.EAttributeValueState.GettingExternalValue -> {
                            loadingIndicatorInContainer.visibility = View.VISIBLE
                        }
                    }
                }

                attributeValueExtractors[attribute.localId] = {
                    inputView.value
                }

                inputView.boundAttributeObjectId = attribute.objectId


                inputView.onResume()
            }
        }
    }
}