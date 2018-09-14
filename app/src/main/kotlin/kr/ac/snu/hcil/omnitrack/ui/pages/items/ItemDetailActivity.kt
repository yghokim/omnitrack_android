package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.afollestad.materialdialogs.MaterialDialog
import com.airbnb.lottie.LottieAnimationView
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.internal.Factory
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_new_item.*
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTTriggerReminderEntry
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.di.global.ForGeneric
import kr.ac.snu.hcil.omnitrack.services.OTReminderService
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.LoadingIndicatorBar
import kr.ac.snu.hcil.omnitrack.ui.components.common.activity.WebServiceLoginActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.container.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.pages.ConnectionIndicatorStubProxy
import kr.ac.snu.hcil.omnitrack.utils.AnyValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import org.jetbrains.anko.notificationManager
import rx_activity_result2.RxActivityResult
import java.util.*
import javax.inject.Inject
import javax.inject.Provider
import kotlin.properties.Delegates

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
/**
 * Created by Young-Ho Kim on 16. 7. 24
 */
class ItemDetailActivity : MultiButtonActionBarActivity(R.layout.activity_new_item), View.OnClickListener {

    class RequiredFieldsNotCompleteException(val inCompleteFieldLocalIds: Array<String>) : Exception("Required fields are not completed.")
    class SurveyNotCompleteException : Exception("You did not complete the survey.")

    companion object {

        const val INTENT_EXTRA_REMINDER_TIME = "reminderTime"

        const val INTENT_EXTRA_IGNORE_CACHED_INPUT = "ignoreCachedInput"

        const val INTENT_ACTION_NEW = "new_item"
        const val INTENT_ACTION_EDIT = "edit_item"

        fun makeNewItemPageIntent(trackerId: String, context: Context): Intent {
            val intent = Intent(context, ItemDetailActivity::class.java)
            intent.action = INTENT_ACTION_NEW
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            return intent
        }

        fun makeReminderOpenIntent(trackerId: String, reminderTime: Long, metadata: JsonObject, context: Context): Intent {
            val intent = Intent(context, ItemDetailActivity::class.java)
            intent.action = INTENT_ACTION_NEW
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            intent.putExtra(INTENT_EXTRA_REMINDER_TIME, reminderTime)
            intent.putExtra(OTApp.INTENT_EXTRA_METADATA, metadata.toString())
            intent.putExtra(INTENT_EXTRA_IGNORE_CACHED_INPUT, true)
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

    @field:[Inject ForGeneric]
    protected lateinit var genericGson: Provider<Gson>

    @field:[Inject Backend]
    protected lateinit var backendRealmFactory: Factory<Realm>

    private val attributeListAdapter = AttributeListAdapter()

    private lateinit var viewModel: ItemEditionViewModelBase

    private val attributeListView: RecyclerView by bindView(R.id.ui_attribute_list)
    private val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    private var mode: ItemEditionViewModelBase.ItemMode = ItemEditionViewModelBase.ItemMode.New

    private var activityResultAppliedAttributePosition = -1

    private lateinit var builderRestoredSnackbar: Snackbar
    private lateinit var redirectedPageCanceledSnackbar: Snackbar

    private val loadingIndicatorBar: LoadingIndicatorBar by bindView(R.id.ui_loading_indicator)

    private val currentAttributeViewModelList = ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>()

    private var itemSaved: Boolean = false

    private val invalidOutsideDialogBuilder: MaterialDialog.Builder by lazy {
        DialogHelper.makeSimpleAlertBuilder(this, "", null) {
            finish()
        }
    }

    override fun onSessionLogContent(contentObject: JsonObject) {
        super.onSessionLogContent(contentObject)
        contentObject["mode"] = mode.name
        if (isFinishing) {
            contentObject["item_saved"] = itemSaved
        }
    }

    override fun onInject(app: OTAndroidApp) {
        app.currentConfiguredContext.configuredAppComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(MultiButtonActionBarActivity.Mode.SaveCancel)
        rightActionBarTextButton?.visibility = View.GONE
        rightActionBarButton?.visibility = View.GONE
        rightActionBarSubButton?.visibility = View.GONE

        ui_button_next.setOnClickListener(this)

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

        builderRestoredSnackbar = Snackbar.make(ui_root, resources.getText(R.string.msg_builder_restored), Snackbar.LENGTH_INDEFINITE)
        builderRestoredSnackbar.setAction(resources.getText(R.string.msg_clear_form)) { view ->
            creationSubscriptions.add(
                    (viewModel.trackerDao?.makePermissionAssertObservable(this, configuredContext) ?: Observable.just(true))
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

        redirectedPageCanceledSnackbar = Snackbar.make(ui_root, resources.getText(R.string.msg_redirected_page_canceled), Snackbar.LENGTH_INDEFINITE)
        redirectedPageCanceledSnackbar.setAction(resources.getText(R.string.msg_reopen_redirected_page)) { view ->
            redirectedPageCanceledSnackbar.dismiss()
            tryOpenRedirectPage()
        }


        creationSubscriptions.add(
                viewModel.isBusyObservable.subscribe { isBusy ->
                    if (isBusy)
                        loadingIndicatorBar.show()
                    else loadingIndicatorBar.dismiss()
                }
        )

        creationSubscriptions.add(
                viewModel.lastInitializedState.observeOn(AndroidSchedulers.mainThread()).subscribe { (itemMode, builderCreationMode) ->

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

        creationSubscriptions.add(
                viewModel.hasTrackerRemovedOutside.subscribe {
                    invalidOutsideDialogBuilder.content(R.string.msg_format_removed_outside_return_home, getString(R.string.msg_text_tracker))
                    invalidOutsideDialogBuilder.show()
                }
        )

        creationSubscriptions.add(
                viewModel.hasItemRemovedOutside.subscribe {
                    invalidOutsideDialogBuilder.content(R.string.msg_format_removed_outside_return_home, getString(R.string.msg_text_item))
                    invalidOutsideDialogBuilder.show()
                }
        )

        (viewModel as? NewItemCreationViewModel)?.let { newItemCreationViewModel ->
            creationSubscriptions.add(
                    newItemCreationViewModel.redirectedPageVisitStatusObservable.subscribe {
                        ui_button_next.text = resources.getString(
                                when (it) {
                                    NewItemCreationViewModel.RedirectedPageStatus.NotVisited -> R.string.msg_visit_survey_website
                                    else -> R.string.msg_save_current_input
                                }
                        )
                    }
            )
        }

        val trackerId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)

        val payloadMetadataJson = intent.getStringExtra(OTApp.INTENT_EXTRA_METADATA)
        val metadata = if (payloadMetadataJson != null) {
            val m = genericGson.get().fromJson(payloadMetadataJson, JsonObject::class.java)
            m.addProperty("accessedDirectlyFromReminder", true)
            m.addProperty("pairedToReminder", true)
            m
        } else {
            //if the tracker was manually opened, get metadata from current reminder.
            val realm = backendRealmFactory.get()
            val entries = realm.where(OTTriggerReminderEntry::class.java)
                    .equalTo("dismissed", false)
                    .equalTo("trackerId", trackerId)
                    .findAll()
            if (entries.size > 0) {
                val m = entries.last()?.serializedMetadata?.let { genericGson.get().fromJson(it, JsonObject::class.java) }
                        ?: JsonObject()
                m.addProperty("accessedDirectlyFromReminder", false)
                m.addProperty("pairedToReminder", true)
                m
            } else jsonObject("pairedToReminder" to false)
        }

        viewModel.init(trackerId, intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM), metadata)

    }

    override fun onPause() {
        super.onPause()

        for (inputView in attributeListAdapter.inputViews) {
            inputView.clearFocus()
        }

        this.activityResultAppliedAttributePosition = -1
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
        itemSaved = false

        if (mode == ItemEditionViewModelBase.ItemMode.New) {
            val needToStoreBuilder = if (viewModel.isValid) {
                if (viewModel.isViewModelsDirty()) {
                    true
                } else if (currentAttributeViewModelList.filter { it.attributeDAO.getHelper(configuredContext).isAttributeValueVolatile(it.attributeDAO) }.isNotEmpty()) {
                    true
                } else viewModel.builderCreationModeObservable.value == ItemEditionViewModelBase.BuilderCreationMode.Restored
            } else {
                false
            }

            if (needToStoreBuilder) {
                /*
                println("store Builder.")
                creationSubscriptions.add(
                        viewModel.cacheEditingInfo().subscribe { saved ->
                            finish()
                        }
                )*/
                DialogHelper.makeYesNoDialogBuilder(this, BuildConfig.APP_NAME, resources.getString(R.string.msg_confirm_item_detail_close), R.string.msg_close, R.string.msg_cancel, {
                    viewModel.clearHistory()
                    finish()
                }, {

                }, false).show()
            } else {
                println("remove builder")
                viewModel.clearHistory()
                finish()
            }
        } else {
            viewModel.clearHistory()
            finish()
        }
    }

    override fun onBackPressed() {
        onToolbarLeftButtonClicked()
    }

    override fun onClick(v: View?) {
        if (v === ui_button_next) {
            onToolbarRightButtonClicked()
        }
    }

    override fun onToolbarRightButtonClicked() {
        //push item to db
        //syncViewStateToBuilderAsync {viewModel.applyEditingToDatabase()


        val viewModel = this.viewModel
        val saveImmediately = if (viewModel is NewItemCreationViewModel) {
            val status = viewModel.redirectedPageVisitStatusObservable.value
            status != NewItemCreationViewModel.RedirectedPageStatus.NotVisited
        } else {
            true
        }

        if (saveImmediately) {
            creationSubscriptions.add(
                    Single.zip(
                            attributeListAdapter.inputViews.map { it.forceApplyValueAsync() }
                    ) { zipped -> zipped }.flatMapMaybe {
                        val incompleteFieldLocalIds = currentAttributeViewModelList.filter { attributeViewModel ->
                            attributeViewModel.isRequired && attributeViewModel.value?.value == null
                        }.map { it.attributeLocalId }

                        if (incompleteFieldLocalIds.isNotEmpty()) {
                            throw RequiredFieldsNotCompleteException(incompleteFieldLocalIds.toTypedArray())
                        } else {
                            /*
                        if(viewModel.mode == ItemEditionViewModelBase.ItemMode.New
                                && viewModel.trackerDao?.redirectUrl != null){
                                val url = Uri.parse(viewModel.trackerDao!!.redirectUrl!!).buildUpon()
                                        .appendQueryParameter("userId", authManager.userId)
                                        .appendQueryParameter("itemId", (viewModel as? NewItemCreationViewModel)?.generateNewItemId())
                                        .run{
                                            viewModel.metadataForItem?.let {
                                                it.forEach { key, value ->
                                                    try {
                                                        this.appendQueryParameter(key, value.asString)
                                                    }catch(ex: Exception){
                                                        this.appendQueryParameter(key, value.toString())
                                                    }
                                                }
                                            }
                                            this
                                        }
                                        .build().toString()
                                println("redirect to ${url}")
                                return@flatMapMaybe RxActivityResult.on(this@ItemDetailActivity)
                                        .startIntent(WebServiceLoginActivity.makeIntent(url, "Qualtrics", "Complete A Survey", this))
                                        .firstElement()
                                        .flatMap {
                                            surveyResult->
                                            (if(surveyResult.resultCode() == RESULT_OK){
                                                viewModel.applyEditingToDatabase()
                                            }else Maybe.error<String>(SurveyNotCompleteException()))
                                        }
                        }else viewModel.applyEditingToDatabase()*/
                            viewModel.applyEditingToDatabase()
                        }
                    }.subscribe({ result ->
                        viewModel.clearHistory()
                        if (viewModel.mode == ItemEditionViewModelBase.ItemMode.New) {
                            startService(OTReminderService.makeUserLoggedIntent(this, viewModel.trackerDao?.objectId!!, System.currentTimeMillis()))
                        }

                        when (viewModel.mode) {
                            ItemEditionViewModelBase.ItemMode.New -> {
                                itemSaved = true
                                eventLogger.get().logItemAddedEvent(result, ItemLoggingSource.Manual) { content ->
                                    if (this.intent.hasExtra(INTENT_EXTRA_REMINDER_TIME)) {
                                        content.add("pivotReminderTime", this.intent.getLongExtra(INTENT_EXTRA_REMINDER_TIME, 0).toJson())
                                    }
                                }

                                setResult(RESULT_OK, Intent().putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, result))
                                finish()
                            }
                            ItemEditionViewModelBase.ItemMode.Edit -> {
                                eventLogger.get().logItemEditEvent(result) { content ->
                                    content[IEventLogger.CONTENT_IS_INDIVIDUAL] = false
                                }
                                setResult(RESULT_OK, Intent().putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, result))
                                finish()
                            }
                        }
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
                                attributeListView.layoutManager?.startSmoothScroll(topFitScroller)
                            }

                            Toast.makeText(this@ItemDetailActivity, "${ex.inCompleteFieldLocalIds.size} required fields are not completed.", Toast.LENGTH_LONG).show()
                        } else if (ex is SurveyNotCompleteException) {
                            Toast.makeText(this@ItemDetailActivity, "The survey was not completed.", Toast.LENGTH_LONG).show()
                        }
                    }, {
                        println("storing item was failed. Null item id returned")
                    })
            )
        } else {
            //redirection mode
            tryOpenRedirectPage()
        }
    }

    private fun tryOpenRedirectPage() {
        val viewModel = this.viewModel
        //redirection mode
        if (viewModel is NewItemCreationViewModel && viewModel.trackerDao?.redirectUrl != null) {
            val url = Uri.parse(viewModel.trackerDao?.redirectUrl).buildUpon()
                    .appendQueryParameter("userId", authManager.userId)
                    .appendQueryParameter("itemId", (viewModel as? NewItemCreationViewModel)?.generateNewItemId())
                    .run {
                        viewModel.metadataForItem.forEach { key, value ->
                            try {
                                this.appendQueryParameter(key, value.asString)
                            } catch (ex: Exception) {
                                this.appendQueryParameter(key, value.toString())
                            }
                        }
                        this
                    }
                    .build().toString()
            creationSubscriptions.add(
                    RxActivityResult.on(this).startIntent(WebServiceLoginActivity.makeIntent(url, "Qualtrics", "Complete A Survey", this))
                            .subscribe { result ->
                                val resultCode = result.resultCode()
                                val redirectionStatus = when (resultCode) {
                                    Activity.RESULT_OK -> {
                                        NewItemCreationViewModel.RedirectedPageStatus.Completed
                                    }
                                    else -> NewItemCreationViewModel.RedirectedPageStatus.Canceled
                                }

                                if (resultCode == Activity.RESULT_OK) {
                                    viewModel.metadataForItem.removeAll(
                                            viewModel.metadataForItem.keys().filter { it.startsWith("returned::") }
                                    )

                                    if (result.data().hasExtra(WebServiceLoginActivity.EXTRA_RETURNED_PARAMETERS)) {
                                        val returnedParameters = result.data().getBundleExtra(WebServiceLoginActivity.EXTRA_RETURNED_PARAMETERS)
                                        for (key in returnedParameters.keySet()) {
                                            viewModel.metadataForItem.set(key, returnedParameters.getString(key))
                                        }
                                    }
                                }

                                viewModel.setResultOfRedirectedPage(redirectionStatus)
                                if (result.resultCode() == Activity.RESULT_OK) {
                                    Toast.makeText(this@ItemDetailActivity, "Completed Qualtrics survey.", Toast.LENGTH_LONG).show()
                                    redirectedPageCanceledSnackbar.dismiss()
                                } else {
                                    redirectedPageCanceledSnackbar.show()
                                }
                            }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
            return getItem(position).attributeDAO.getInputViewType(configuredContext, false)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

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

            private val internalSubscriptions = CompositeDisposable()

            private var currentValidationState: Boolean by Delegates.observable(true) { property, old, new ->
                if (old != new) {
                    if (new) {

                        //validation
                        if (validationIndicator.progress != 1f || validationIndicator.progress != 0f) {
                            validationIndicator.setMinProgress(0.5f)
                            validationIndicator.setMaxProgress(1.0f)
                            validationIndicator.progress = 0.5f
                            validationIndicator.playAnimation()
                        }
                    } else {
                        //invalidated
                        if (validationIndicator.progress != 0.5f) {
                            validationIndicator.setMinProgress(0.0f)
                            validationIndicator.setMaxProgress(0.5f)
                            validationIndicator.progress = 0f
                            validationIndicator.playAnimation()
                        }
                    }
                }
            }


            init {
                validationIndicator.progress = 0.0f

                container.addView(inputView, 0)

                connectionIndicatorStubProxy = ConnectionIndicatorStubProxy(frame, R.id.ui_connection_indicator_stub)

                /*
                optionButton.setOnClickListener {
                    /*
                    val tracker = tracker
                    val attributeLocalId = attributeLocalId
                    if (tracker != null && attributeLocalId != null) {
                        val historyDialog = RecentItemValuePickerBottomSheetFragment.getInstance(tracker.objectId, attributeLocalId)
                        historyDialog.show(supportFragmentManager, RecentItemValuePickerBottomSheetFragment.TAG)
                    }*/
                }*/
            }

            private fun setTimestampIndicatorText(timestamp: Long?) {
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

                attributeViewModel.attributeDAO.getHelper(configuredContext).refreshInputViewUI(inputView, attributeViewModel.attributeDAO)
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
                        inputView.valueChanged.observable.subscribe { (_, args) ->
                            val now = System.currentTimeMillis()
                            attributeViewModel.value = AnyValueWithTimestamp(args, now)
                            builderRestoredSnackbar.dismiss()
                        }
                )

                internalSubscriptions.add(
                        attributeViewModel.validationObservable.subscribe { isValid ->
                            currentValidationState = isValid
                        }
                )

                internalSubscriptions.add(
                        attributeViewModel.onAttributeChanged.subscribe {
                            attributeViewModel.attributeDAO.getHelper(configuredContext).refreshInputViewUI(inputView, attributeViewModel.attributeDAO)
                        }
                )


                connectionIndicatorStubProxy.onBind(attributeViewModel.attributeDAO.getParsedConnection(configuredContext))

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