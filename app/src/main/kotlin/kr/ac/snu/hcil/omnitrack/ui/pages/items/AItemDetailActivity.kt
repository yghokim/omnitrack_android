package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.*
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import butterknife.bindView
import com.afollestad.materialdialogs.MaterialDialog
import com.github.salomonbrys.kotson.*
import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_new_item.*
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.LoadingIndicatorBar
import kr.ac.snu.hcil.omnitrack.ui.components.common.container.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.pages.ConnectionIndicatorStubProxy
import kr.ac.snu.hcil.omnitrack.utils.AnyValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import java.util.*
import kotlin.properties.Delegates

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
/**
 * Created by Young-Ho Kim on 16. 7. 24
 */
abstract class AItemDetailActivity<ViewModelType : ItemEditionViewModelBase>(val viewModelClass: Class<ViewModelType>) : MultiButtonActionBarActivity(R.layout.activity_new_item), View.OnClickListener {

    class RequiredFieldsNotCompleteException(val inCompleteFieldLocalIds: Array<String>) : Exception("Required fields are not completed.")

    companion object {

        const val KEY_ITEM_SAVED = "itemSaved"
        const val KEY_METADATA = "metadata"

        const val REQUEST_CODE_REDIRECT_SURVEY = 23153
    }

    private val attributeListAdapter = AttributeListAdapter()

    protected lateinit var viewModel: ViewModelType

    private val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    private val loadingIndicatorBar: LoadingIndicatorBar by bindView(R.id.ui_loading_indicator)

    protected val currentAttributeViewModelList = ArrayList<ItemEditionViewModelBase.AttributeInputViewModel>()

    //State=============================================================================================================
    protected var itemSaved: Boolean = false
    //==================================================================================================================

    protected val invalidOutsideDialogBuilder: MaterialDialog.Builder by lazy {
        DialogHelper.makeSimpleAlertBuilder(this, "", null) {
            finish()
        }
    }

    abstract fun getTitle(trackerName: String): String

    abstract fun initViewModel(viewModel: ViewModelType, savedInstanceState: Bundle?)

    override fun onSessionLogContent(contentObject: JsonObject) {
        super.onSessionLogContent(contentObject)
        if (isFinishing) {
            contentObject["item_saved"] = itemSaved
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {

        outState.putBoolean(KEY_ITEM_SAVED, itemSaved)
        outState.putString(KEY_METADATA, viewModel.metadataForItem.toString())

        super.onSaveInstanceState(outState)

        this.viewModel.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(MultiButtonActionBarActivity.Mode.SaveCancel)
        rightActionBarTextButton?.visibility = View.GONE
        rightActionBarButton?.visibility = View.GONE
        rightActionBarSubButton?.visibility = View.GONE

        ui_button_next.setOnClickListener(this)

        ui_attribute_list.layoutManager = layoutManager
        ui_attribute_list.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, this))
        (ui_attribute_list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        ui_attribute_list.adapter = attributeListAdapter

        loadingIndicatorBar.setMessage(R.string.msg_indicator_message_item_autocomplete)

        viewModel = ViewModelProviders.of(this).get(viewModelClass)
        initViewModel(viewModel, savedInstanceState)

        if (savedInstanceState != null) {
            this.itemSaved = savedInstanceState.getBoolean(KEY_ITEM_SAVED)
        }

        creationSubscriptions.add(
                viewModel.isBusyObservable.subscribe { isBusy ->
                    if (isBusy)
                        loadingIndicatorBar.show()
                    else loadingIndicatorBar.dismiss()
                }
        )

        creationSubscriptions.add(
                viewModel.trackerNameObservable.subscribe { name ->
                    title = getTitle(name)
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
                viewModel.isAllInputCompleteObservable.subscribe {
                    if (it) {
                        onTrackerInputComplete()
                    } else onTrackerInputIncomplete()
                }
        )
    }

    protected open fun onTrackerInputComplete() {

    }

    protected open fun onTrackerInputIncomplete() {

    }

    override fun onPause() {
        super.onPause()

        for (inputView in attributeListAdapter.inputViews) {
            inputView.clearFocus()
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
        itemSaved = false
    }

    override fun onBackPressed() {
        onToolbarLeftButtonClicked()
    }

    override fun onClick(v: View?) {
        if (v === ui_button_next) {
            onToolbarRightButtonClicked()
        }
    }

    protected fun checkInputComplete(): Completable {
        return Single.zip(
                attributeListAdapter.inputViews.map { it.forceApplyValueAsync() }
        ) { zipped -> zipped }.flatMapCompletable {
            val incompleteFieldLocalIds = currentAttributeViewModelList.asSequence().filter { attributeViewModel ->
                attributeViewModel.isRequired && attributeViewModel.value?.value == null
            }.map { it.attributeLocalId }.toList()

            if (incompleteFieldLocalIds.isNotEmpty()) {
                throw RequiredFieldsNotCompleteException(incompleteFieldLocalIds.toTypedArray())
            } else {
                return@flatMapCompletable Completable.complete()
            }
        }.doOnError { ex ->
            if (ex is RequiredFieldsNotCompleteException) {
                val incompleteFields = currentAttributeViewModelList.asSequence().mapIndexed { index, attributeInputViewModel -> Pair(index, attributeInputViewModel) }.filter { ex.inCompleteFieldLocalIds.contains(it.second.attributeLocalId) }.toList()

                val minPosition = incompleteFields.minBy { it.first }?.first
                if (minPosition != null) {
                    val topFitScroller = object : LinearSmoothScroller(this@AItemDetailActivity) {
                        override fun getVerticalSnapPreference(): Int {
                            return LinearSmoothScroller.SNAP_TO_START
                        }
                    }

                    topFitScroller.targetPosition = minPosition
                    ui_attribute_list.layoutManager?.startSmoothScroll(topFitScroller)
                }

                Toast.makeText(this@AItemDetailActivity, "${ex.inCompleteFieldLocalIds.size} required fields are not completed.", Toast.LENGTH_LONG).show()
            }
        }
    }


    protected open fun preOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (!preOnActivityResult(requestCode, resultCode, data)) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val attributePosition = AAttributeInputView.getPositionFromRequestCode(requestCode)
                val inputView = attributeListAdapter.inputViews.find { it.position == attributePosition }
                inputView?.setValueFromActivityResult(data, AAttributeInputView.getRequestTypeFromRequestCode(requestCode))
            }
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

            val frame = LayoutInflater.from(this@AItemDetailActivity).inflate(R.layout.attribute_input_frame, parent, false)

            val inputView = AAttributeInputView.makeInstance(viewType, this@AItemDetailActivity)
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

        inner class ViewHolder(val inputView: AAttributeInputView<out Any>, frame: View) : View.OnClickListener, RecyclerView.ViewHolder(frame) {

            private val columnNameView: TextView by bindView(R.id.ui_column_name)
            private val requiredMarker: View by bindView(R.id.ui_required_marker)
            //private val attributeTypeView: TextView by bindView(R.id.ui_attribute_type)

            private val container: LockableFrameLayout by bindView(R.id.ui_input_view_container)

            private val timestampIndicator: TextView by bindView(R.id.ui_timestamp)

            private var itemTimestamp: Long? = null

            private val connectionIndicatorStubProxy: ConnectionIndicatorStubProxy

            private val loadingIndicatorInContainer: View by bindView(R.id.ui_container_indicator)

            internal val validationIndicator: AppCompatImageView by bindView(R.id.ui_validation_indicator)

            private val internalSubscriptions = CompositeDisposable()

            private var currentValidationState: Boolean by Delegates.observable(true) { property, old, new ->
                /*if (new) {
                    //valid
                    if (validationIndicator.progress != 1f || validationIndicator.progress != 0f) {
                        validationIndicator.setMinProgress(0.5f)
                        validationIndicator.setMaxProgress(1.0f)
                        validationIndicator.progress = 0.5f
                        validationIndicator.playAnimation()
                    }
                } else {
                    //invalid
                    if (validationIndicator.progress != 0.5f) {
                        validationIndicator.setMinProgress(0.0f)
                        validationIndicator.setMaxProgress(0.5f)
                        validationIndicator.progress = 0f
                        validationIndicator.playAnimation()
                    }
                }*/

                validationIndicator.isActivated = new
            }


            init {

                container.addView(inputView, 0)

                connectionIndicatorStubProxy = ConnectionIndicatorStubProxy(frame, R.id.ui_connection_indicator_stub)

                timestampIndicator.setOnClickListener(this)

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

            override fun onClick(v: View?) {
                if (v === timestampIndicator) {
                    this.setTimestampIndicatorText(this.itemTimestamp)
                }
            }

            private fun setTimestampIndicatorText(timestamp: Long?) {
                this.itemTimestamp = timestamp
                if (timestamp == null || timestamp == 0L) {
                    timestampIndicator.text = ""
                } else {
                    val now = System.currentTimeMillis()
                    timestampIndicator.text = if (now - timestamp < 1 * DateUtils.MINUTE_IN_MILLIS) {
                        resources.getString(R.string.time_just_now)
                    } else {
                        DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL)
                    }
                }
            }

            fun bind(attributeViewModel: ItemEditionViewModelBase.AttributeInputViewModel) {

                InterfaceHelper.alertBackground(this.itemView)

                validationIndicator.isActivated = attributeViewModel.isValidated

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

                            TooltipCompat.setTooltipText(timestampIndicator, valueNullable.datum?.timestamp?.let { (applicationContext as OTAndroidApp).currentConfiguredContext.configuredAppComponent.getLocalTimeFormats().FORMAT_DATETIME.format(Date(it)) })
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