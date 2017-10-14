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
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.LoadingIndicatorBar
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.pages.ConnectionIndicatorStubProxy
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 24
 */
class ItemEditingActivity : MultiButtonActionBarActivity(R.layout.activity_new_item) {

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

    private val attributeListView: RecyclerView by bindView(R.id.ui_attribute_list)
    private val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    private var mode: ItemEditionViewModel.ItemMode = ItemEditionViewModel.ItemMode.New

    private var activityResultAppliedAttributePosition = -1

    private lateinit var builderRestoredSnackbar: Snackbar

    private val loadingIndicatorBar: LoadingIndicatorBar by bindView(R.id.ui_loading_indicator)

    private val currentAttributeViewModelList = ArrayList<ItemEditionViewModel.AttributeInputViewModel>()

    private var itemSaved: Boolean = false

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
        val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)

        attributeListView.layoutManager = layoutManager
        attributeListView.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, this))
        (attributeListView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        attributeListView.adapter = attributeListAdapter

        loadingIndicatorBar.setMessage(R.string.msg_indicator_message_item_autocomplete)

        builderRestoredSnackbar = Snackbar.make(findViewById(R.id.ui_snackbar_container), resources.getText(R.string.msg_builder_restored), Snackbar.LENGTH_INDEFINITE)
        builderRestoredSnackbar.setAction(resources.getText(R.string.msg_clear_form)) {
            view ->

            creationSubscriptions.add(
                    (viewModel.trackerDao?.makePermissionAssertObservable(this) ?: Observable.just(true))
                            .subscribe({ approved ->
                                if (approved) {
                                    viewModel.removeItemBuilder()
                                    viewModel.reset()
                                } else {

                                }
                            }, {
                                //TODO handle permission not granted
                            }, {
                                println("Finished builder autocomplete.")

                            })
            )

            builderRestoredSnackbar.dismiss()
        }

        creationSubscriptions.add(
                viewModel.isBusyObservable.subscribe { isBusy ->
                    if (isBusy)
                        loadingIndicatorBar.show()
                    else loadingIndicatorBar.dismiss()
                }
        )

        creationSubscriptions.add(
                viewModel.onInitialized.observeOn(AndroidSchedulers.mainThread()).subscribe { (itemMode, builderCreationMode) ->
                    println("ItemEditingViewModel initialized: itemMode - ${itemMode}, builderMode - ${builderCreationMode}")
                    if (itemMode == ItemEditionViewModel.ItemMode.New) {
                        if (builderCreationMode == ItemEditionViewModel.BuilderCreationMode.Restored) {
                            builderRestoredSnackbar.show()
                        } else if (builderCreationMode == ItemEditionViewModel.BuilderCreationMode.NewBuilder) {
                            viewModel.startAutoComplete()
                        }
                    }
                }
        )

        creationSubscriptions.add(
                viewModel.modeObservable.flatMap { mode -> viewModel.trackerNameObservable.map { name -> Pair(mode, name) } }.subscribe { (mode, name) ->
                    when (mode) {
                        ItemEditionViewModel.ItemMode.Edit -> {
                            title = String.format(getString(R.string.title_activity_edit_item), name)
                        }
                        ItemEditionViewModel.ItemMode.New -> {
                            title = String.format(resources.getString(R.string.title_activity_new_item), name)
                        }
                    }
                }
        )

        creationSubscriptions.add(
                viewModel.attributeViewModelListObservable.subscribe { list ->
                    currentAttributeViewModelList.clear()
                    currentAttributeViewModelList.addAll(list)
                    attributeListAdapter.notifyDataSetChanged()
                }
        )

        if (trackerId != null) {
            viewModel.init(trackerId, intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM))
        }
    }


    override fun onPause() {
        super.onPause()

        for (inputView in attributeListAdapter.inputViews) {
            inputView.onPause()
        }

        for (inputView in attributeListAdapter.inputViews) {
            inputView.clearFocus()
        }

        this.activityResultAppliedAttributePosition = -1
    }

    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy ItemEditingActivity")

        for (inputView in attributeListAdapter.inputViews) {
            inputView.onDestroy()
        }

        /*
        ItemBuilder Caching Policy:
        1. if user edited the result: save
        2. if user did not edited the result:
            - if field values are volatile: cache result
            - if field values are not volatile:
                - creationMode was new: discard builder (it will yield the same result on later visit)
                - creationMode was restored: store builder (it is naturally dirty after autoComplete.)
         */

        val needToStoreBuilder = if (viewModel.isValid && viewModel.mode == ItemEditionViewModel.ItemMode.New) {

            if (viewModel.isViewModelsDirty()) {
                true
            } else if (currentAttributeViewModelList.filter { it.attributeDAO.getHelper().isAttributeValueVolatile(it.attributeDAO) }.isNotEmpty()) {
                true
            } else viewModel.builderCreationModeObservable.value == ItemEditionViewModel.BuilderCreationMode.Restored
        } else {
            false
        }

        if (needToStoreBuilder) {
            println("store Builder.")
            viewModel.saveItemBuilder()
        } else {
            println("remove builder")
            viewModel.removeItemBuilder()
        }
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
        //syncViewStateToBuilderAsync {viewModel.applyBuilderToItem()
        viewModel.applyBuilderToItem().subscribe { result ->
            if (result.datum != null) {
                viewModel.removeItemBuilder()
                setResult(RESULT_OK, Intent().putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, result.datum))
                finish()
            }
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

        fun getItem(position: Int): ItemEditionViewModel.AttributeInputViewModel {
            return currentAttributeViewModelList[position]
        }

        override fun getItemViewType(position: Int): Int {
            return getItem(position).attributeDAO.getInputViewType(false)
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
            return currentAttributeViewModelList.size
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

            private val internalSubscriptions = CompositeSubscription()


            init {

                container.addView(inputView, 0)

                connectionIndicatorStubProxy = ConnectionIndicatorStubProxy(frame, R.id.ui_connection_indicator_stub)

                optionButton.setOnClickListener {
                    /*
                    val tracker = tracker
                    val attributeLocalId = attributeLocalId
                    if (tracker != null && attributeLocalId != null) {
                        val historyDialog = RecentItemValuePickerBottomSheetFragment.getInstance(tracker.objectId, attributeLocalId)
                        historyDialog.show(supportFragmentManager, RecentItemValuePickerBottomSheetFragment.TAG)
                    }*/
                }
            }

            private fun setTimestampIndicatorText(timestamp: Long?) {
                println("timestamp: ${timestamp}")
                if (timestamp == null) {
                    timestampIndicator.text = ""
                } else {
                    val now = System.currentTimeMillis()
                    timestampIndicator.text = if (now - timestamp < 2 * DateUtils.SECOND_IN_MILLIS) {
                        resources.getString(R.string.time_just_now)
                    } else {
                        DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL)
                    }
                }
            }

            fun bind(attributeViewModel: ItemEditionViewModel.AttributeInputViewModel) {
                println("bind attributeViewModel: ${attributeViewModel.attributeLocalId}")

                internalSubscriptions.clear()

                attributeViewModel.attributeDAO.getHelper().refreshInputViewUI(inputView, attributeViewModel.attributeDAO)
                internalSubscriptions.add(
                        attributeViewModel.columnNameObservable.subscribe { name ->
                            columnNameView.text = name
                        }
                )
                requiredMarker.visibility = View.INVISIBLE
                /*
                requiredMarker.visibility = if (attributeViewModel.isRequired) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }*/
                //attributeTypeView.text = resources.getString(attribute.typeNameResourceId)


                internalSubscriptions.add(
                        inputView.valueChanged.observable.subscribe { (sender, args) ->
                            val now = System.currentTimeMillis()
                            attributeViewModel.value = OTItemBuilderWrapperBase.ValueWithTimestamp(args, now)
                            builderRestoredSnackbar.dismiss()
                    }
                )


                connectionIndicatorStubProxy.onBind(attributeViewModel.attributeDAO.getParsedConnection())

                internalSubscriptions.add(
                        attributeViewModel.stateObservable.observeOn(AndroidSchedulers.mainThread()).subscribe { state ->
                            if (state == OTItemBuilderWrapperBase.EAttributeValueState.Idle) {
                                container.locked = false
                                inputView.alpha = 1.0f
                            } else {
                                container.locked = true
                                inputView.alpha = 0.12f
                            }
                            when (state) {
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
                )

                internalSubscriptions.add(
                        attributeViewModel.valueObservable.observeOn(AndroidSchedulers.mainThread()).subscribe { valueNullable ->
                            println("viewModel value changed - ${valueNullable.datum}, ${attributeViewModel.attributeLocalId}")
                            if (inputView.value != valueNullable.datum?.value) {
                                inputView.valueChanged.suspend = true
                                inputView.setAnyValue(valueNullable.datum?.value)
                                inputView.valueChanged.suspend = false
                            }
                            setTimestampIndicatorText(valueNullable.datum?.timestamp)
                        }
                )

                creationSubscriptions.addAll(internalSubscriptions)

                inputView.boundAttributeObjectId = attributeViewModel.attributeDAO.objectId


                inputView.onResume()
            }
        }
    }
}