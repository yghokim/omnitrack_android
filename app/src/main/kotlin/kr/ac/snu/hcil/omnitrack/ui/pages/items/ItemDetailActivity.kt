package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import butterknife.bindView
import com.airbnb.lottie.LottieAnimationView
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.LoadingIndicatorBar
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.pages.ConnectionIndicatorStubProxy
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.ValueWithTimestamp
import org.jetbrains.anko.notificationManager
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 16. 7. 24
 */
class ItemDetailActivity : MultiButtonActionBarActivity(R.layout.activity_new_item) {

    class RequiredFieldsNotCompleteException(val inCompleteFieldLocalIds: Array<String>) : Exception("Required fields are not completed.")

    companion object {

        const val INTENT_EXTRA_REMINDER_TIME = "reminderTime"

        const val INTENT_ACTION_NEW = "new_item"
        const val INTENT_ACTION_EDIT = "edit_item"

        fun makeNewItemPageIntent(trackerId: String, context: Context): Intent {
            val intent = Intent(context, ItemDetailActivity::class.java)
            intent.action = INTENT_ACTION_NEW
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            return intent
        }

        fun makeIntent(trackerId: String, reminderTime: Long, context: Context): Intent {
            val intent = Intent(context, ItemDetailActivity::class.java)
            intent.action = INTENT_ACTION_NEW
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            intent.putExtra(INTENT_EXTRA_REMINDER_TIME, reminderTime)
            intent.action = "item_edit:${trackerId}"
            return intent
        }

        fun makeItemEditPageIntent(itemId: String, trackerId: String, context: Context): Intent {
            val intent = Intent(context, ItemDetailActivity::class.java)
            intent.action = INTENT_ACTION_EDIT
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, itemId)
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            return intent
        }
    }

    private val attributeListAdapter = AttributeListAdapter()

    private lateinit var viewModel: ItemEditionViewModelBase

    private val attributeListView: RecyclerView by bindView(R.id.ui_attribute_list)
    private val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    private var mode: ItemEditionViewModelBase.ItemMode = ItemEditionViewModelBase.ItemMode.New

    private var activityResultAppliedAttributePosition = -1

    private lateinit var builderRestoredSnackbar: Snackbar

    private val loadingIndicatorBar: LoadingIndicatorBar by bindView(R.id.ui_loading_indicator)

    private val currentAttributeViewModelList = ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>()

    private var itemSaved: Boolean = false

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

        val action = intent.action
        viewModel = when (action) {
            INTENT_ACTION_NEW -> ViewModelProviders.of(this).get(NewItemCreationViewModel::class.java)
            INTENT_ACTION_EDIT -> ViewModelProviders.of(this).get(ItemEditingViewModel::class.java)
            else -> ViewModelProviders.of(this).get(NewItemCreationViewModel::class.java)
        }

        if (intent.hasExtra(OTApp.INTENT_EXTRA_NOTIFICATION_ID)) {
            val notificationID = intent.getIntExtra(OTApp.INTENT_EXTRA_NOTIFICATION_ID, -1)
            val tag = intent.getStringExtra(OTApp.INTENT_EXTRA_NOTIFICATON_TAG)
            if (tag != null) {
                notificationManager.cancel(tag, notificationID)
            } else notificationManager.cancel(notificationID)
        }

        attributeListView.layoutManager = layoutManager
        attributeListView.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, this))
        (attributeListView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        attributeListView.adapter = attributeListAdapter

        loadingIndicatorBar.setMessage(R.string.msg_indicator_message_item_autocomplete)

        builderRestoredSnackbar = Snackbar.make(findViewById(R.id.ui_snackbar_container), resources.getText(R.string.msg_builder_restored), Snackbar.LENGTH_INDEFINITE)
        builderRestoredSnackbar.setAction(resources.getText(R.string.msg_clear_form)) { view ->

            creationSubscriptions.add(
                    (viewModel.trackerDao?.makePermissionAssertObservable(this) ?: Observable.just(true))
                            .subscribe({ approved ->
                                if (approved) {
                                    viewModel.clearHistory()
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
                    this.mode = itemMode

                    if (builderCreationMode != null) {
                        if (builderCreationMode == ItemEditionViewModelBase.BuilderCreationMode.Restored) {
                            builderRestoredSnackbar.show()
                        } else if (builderCreationMode == ItemEditionViewModelBase.BuilderCreationMode.NewBuilder) {
                            viewModel.startAutoComplete()
                        }
                    } else {
                        viewModel.startAutoComplete()
                    }
                }
        )

        creationSubscriptions.add(
                viewModel.modeObservable.flatMap { mode -> viewModel.trackerNameObservable.map { name -> Pair(mode, name) } }.subscribe { (mode, name) ->
                    when (mode) {
                        ItemEditionViewModelBase.ItemMode.Edit -> {
                            title = String.format(getString(R.string.title_activity_edit_item), name)
                        }
                        ItemEditionViewModelBase.ItemMode.New -> {
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


        val trackerId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)
        viewModel.init(trackerId, intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM))

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
        println("onDestroy ItemDetailActivity")

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

        if (mode == ItemEditionViewModelBase.ItemMode.New) {
            val needToStoreBuilder = if (viewModel.isValid && viewModel.mode == ItemEditionViewModelBase.ItemMode.New) {

                if (viewModel.isViewModelsDirty()) {
                    true
                } else if (currentAttributeViewModelList.filter { it.attributeDAO.getHelper().isAttributeValueVolatile(it.attributeDAO) }.isNotEmpty()) {
                    true
                } else viewModel.builderCreationModeObservable.value == ItemEditionViewModelBase.BuilderCreationMode.Restored
            } else {
                false
            }

            if (needToStoreBuilder) {
                println("store Builder.")
                creationSubscriptions.add(
                        viewModel.cacheEditingInfo().subscribe { saved ->
                            finish()
                        }
                )
            } else {
                println("remove builder")
                viewModel.clearHistory()
                finish()
            }
        }

        finish()
    }

    override fun onBackPressed() {
        onToolbarLeftButtonClicked()
    }

    override fun onToolbarRightButtonClicked() {
        //push item to db
        //syncViewStateToBuilderAsync {viewModel.applyEditingToDatabase()

        creationSubscriptions.add(
                Single.zip(
                        attributeListAdapter.inputViews.map { it.forceApplyValueAsync() }
                ) { zipped -> zipped }.flatMapMaybe {
                    val incompleteFieldLocalIds = currentAttributeViewModelList.filter {
                        it.isRequired && it.value?.value == null
                    }.map { it.attributeLocalId }

                    if (incompleteFieldLocalIds.isNotEmpty()) {
                        throw RequiredFieldsNotCompleteException(incompleteFieldLocalIds.toTypedArray())
                    } else {
                        viewModel.applyEditingToDatabase()
                    }
                }.subscribe({ result ->
                        viewModel.clearHistory()
                    setResult(RESULT_OK, Intent().putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, result))
                        finish()
                }, { ex ->
                    if (ex is RequiredFieldsNotCompleteException) {
                        val incompleteFields = currentAttributeViewModelList.mapIndexed { index, attributeInputViewModel -> Pair(index, attributeInputViewModel) }.filter { ex.inCompleteFieldLocalIds.contains(it.second.attributeLocalId) }

                        val minPosition = incompleteFields.minBy { it.first }?.first
                        if (minPosition != null) {
                            val topFitScroller = object : LinearSmoothScroller(this@ItemDetailActivity) {
                                override fun getVerticalSnapPreference(): Int {
                                    return LinearSmoothScroller.SNAP_TO_START
                                }
                            }

                            topFitScroller.targetPosition = minPosition
                            attributeListView.layoutManager.startSmoothScroll(topFitScroller)
                        }

                        Toast.makeText(this@ItemDetailActivity, "${ex.inCompleteFieldLocalIds.size} required fields are not completed.", Toast.LENGTH_LONG).show()
                    }
                }, {
                    println("storing item was failed. Null item id returned")
                })
        )
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

        fun getItem(position: Int): ItemEditionViewModelBase.AttributeInputViewModel {
            return currentAttributeViewModelList[position]
        }

        override fun getItemViewType(position: Int): Int {
            return getItem(position).attributeDAO.getInputViewType(false)
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {

            val frame = LayoutInflater.from(this@ItemDetailActivity).inflate(R.layout.attribute_input_frame, parent, false)

            val inputView = AAttributeInputView.makeInstance(viewType, this@ItemDetailActivity)
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

            private val validationIndicator: LottieAnimationView by bindView(R.id.ui_validation_indicator)

            private val optionButton: View by bindView(R.id.ui_button_option)

            private val internalSubscriptions = CompositeDisposable()

            private var currentValidationState: Boolean by Delegates.observable(true) { property, old, new ->
                if (old != new) {
                    if (new == true) {

                        //validation
                        if (validationIndicator.progress != 1f || validationIndicator.progress != 0f) {
                            validationIndicator.playAnimation(0.5f, 1f)
                        }
                    } else {
                        //invalidated
                        if (validationIndicator.progress != 0.5f) {
                            validationIndicator.playAnimation(0.0f, 0.5f)
                        }
                    }
                }
            }


            init {
                validationIndicator.progress = 0.0f

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
                if (timestamp == null || timestamp == 0L) {
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

            fun bind(attributeViewModel: ItemEditionViewModelBase.AttributeInputViewModel) {
                validationIndicator.pauseAnimation()

                InterfaceHelper.alertBackground(this.itemView)

                if (attributeViewModel.isValidated) {
                    validationIndicator.progress = 0.0f
                } else {
                    validationIndicator.progress = 0.5f
                }

                internalSubscriptions.clear()

                attributeViewModel.attributeDAO.getHelper().refreshInputViewUI(inputView, attributeViewModel.attributeDAO)
                internalSubscriptions.add(
                        attributeViewModel.columnNameObservable.subscribe { name ->
                            columnNameView.text = name
                        }
                )

                internalSubscriptions.add(
                        attributeViewModel.isRequiredObservable.subscribe {
                            requiredMarker.visibility = if (it == true) {
                                View.VISIBLE
                            } else {
                                View.INVISIBLE
                            }
                        }
                )

                internalSubscriptions.add(
                        inputView.valueChanged.observable.subscribe { (sender, args) ->
                            val now = System.currentTimeMillis()
                            attributeViewModel.value = ValueWithTimestamp(args, now)
                            builderRestoredSnackbar.dismiss()
                        }
                )

                internalSubscriptions.add(
                        attributeViewModel.validationObservable.subscribe { isValid ->
                            currentValidationState = isValid
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