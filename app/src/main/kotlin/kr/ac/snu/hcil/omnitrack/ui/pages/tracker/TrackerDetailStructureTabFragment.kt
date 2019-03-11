package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import com.afollestad.materialdialogs.MaterialDialog
import com.github.salomonbrys.kotson.set
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import kotlinx.android.synthetic.main.attribute_list_element.view.*
import kotlinx.android.synthetic.main.fragment_tracker_detail_structure.*
import kr.ac.snu.hcil.android.common.DefaultNameGenerator
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.dipSize
import kr.ac.snu.hcil.android.common.view.DialogHelper
import kr.ac.snu.hcil.android.common.view.IReadonlyObjectId
import kr.ac.snu.hcil.android.common.view.container.AdapterLinearLayout
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.LockedPropertiesHelper
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.attributes.AttributePresetInfo
import kr.ac.snu.hcil.omnitrack.core.database.DaoSerializationManager
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AttributeViewFactoryManager
import kr.ac.snu.hcil.omnitrack.ui.components.tutorial.TutorialManager
import kr.ac.snu.hcil.omnitrack.ui.pages.ConnectionIndicatorStubProxy
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.AttributeDetailActivity
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Created by Young-Ho Kim on 16. 7. 29
 */
@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class TrackerDetailStructureTabFragment : OTFragment() {

    companion object {
        const val REQUEST_CODE_ATTRIBUTE_DETAIL = 52423
        //val toastForAdded by lazy { Toast.makeText(OTApp.instance, R.string.msg_shortcut_added, Toast.LENGTH_SHORT) }
        //val toastForRemoved by lazy { Toast.makeText(OTApp.instance, R.string.msg_shortcut_removed, Toast.LENGTH_SHORT) }
    }

    private lateinit var rootScrollView: NestedScrollView

    private lateinit var attributeListView: AdapterLinearLayout
    //lateinit private var attributeListAdapter: AttributeListAdapter

    private lateinit var attributeListItemTouchHelper: ItemTouchHelper

    //private lateinit var fab: FloatingActionButton

    private lateinit var contentContainer: ViewGroup

    private lateinit var removalSnackbar: Snackbar

    private lateinit var newAttributeButton: FloatingActionButton

    private var scrollToBottomReserved = false

    private lateinit var viewModel: TrackerDetailViewModel

    private val currentAttributeViewModelList = ArrayList<TrackerDetailViewModel.AttributeInformationViewModel>()

    private val attributeListAdapter = AttributeListAdapter()

    private val viewHolderSubscriptions = CompositeDisposable()

    private val columnNameDialogBuilder: MaterialDialog.Builder by lazy {
        MaterialDialog.Builder(requireContext())
                .title(R.string.msg_change_field_name)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .inputRangeRes(1, 20, R.color.colorRed)
                .cancelable(true)
                .negativeText(R.string.msg_cancel)
    }

    private val newAttributePanel: FieldPresetSelectionBottomSheetFragment by lazy {

        val newAttributePanel = FieldPresetSelectionBottomSheetFragment()
        newAttributePanel.callback = object : FieldPresetSelectionBottomSheetFragment.Callback {
            override fun onAttributePermittedToAdd(typeInfo: AttributePresetInfo) {
                addNewAttribute(typeInfo)
            }
        }

        newAttributePanel
    }

    @Inject
    lateinit var serializationManager: Lazy<DaoSerializationManager>

    @Inject
    lateinit var tutorialManager: TutorialManager

    @Inject
    lateinit var attributeViewFactoryManager: Lazy<AttributeViewFactoryManager>

    override fun onInject(app: OTAndroidApp) {
        app.applicationComponent.inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        this.viewModel = ViewModelProviders.of(activity!!).get(TrackerDetailViewModel::class.java)

        if (this.viewModel.attributeViewModelListObservable.hasValue()) {
            currentAttributeViewModelList.clear()
            currentAttributeViewModelList.addAll(this.viewModel.attributeViewModelListObservable.value!!)
        }

        //set UI
        nameProperty.value = this.viewModel.name
        nameProperty.showEditedOnTitle = viewModel.isNameDirty
        nameProperty.inputLengthMin = 1
        nameProperty.inputLengthMax = 30
        nameProperty.dialogTitle = String.format(getString(R.string.msg_format_change_name), getString(R.string.msg_text_tracker))


        isOnShortcutProperty.value = this.viewModel.isBookmarked
        isOnShortcutProperty.showEditedOnTitle = this.viewModel.isBookmarkedDirty

        applyColorTheme(this.viewModel.color, false)
        colorProperty.value = this.viewModel.color
        colorProperty.showEditedOnTitle = this.viewModel.isColorDirty

        currentAttributeViewModelList.clear()

        creationSubscriptions.add(
                this.viewModel.attributeViewModelListObservable.subscribe { newList ->

                    val diffResult = DiffUtil.calculateDiff(
                            IReadonlyObjectId.DiffUtilCallback(currentAttributeViewModelList, newList)
                    )

                    currentAttributeViewModelList.clear()
                    currentAttributeViewModelList.addAll(newList)

                    diffResult.dispatchUpdatesTo(attributeListAdapter)
                }
        )



        creationSubscriptions.add(
                this.viewModel.lockedPropertiesObservable.subscribe { (lockedProperties) ->
                    val isBookmarkChangeable = !LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.TRACKER_BOOKMARK, lockedProperties)
                    val canAddAttributes = !LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.TRACKER_ADD_NEW_ATTRIBUTE, lockedProperties)
                    val isNameChangeable = !LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.TRACKER_CHANGE_NAME, lockedProperties)
                    val isAttributeOrderChangeable = !LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.TRACKER_CHANGE_ATTRIBUTE_ORDER, lockedProperties)

                    isOnShortcutProperty.isEnabled = isBookmarkChangeable
                    nameProperty.isEnabled = isNameChangeable

                    if (canAddAttributes) {
                        newAttributeButton.show()
                    } else {
                        newAttributeButton.hide()
                    }

                    //TODO: implement disabling the attribute order change.
                }
        )

        assignedExperimentProperty.visibility = View.GONE
        /*
        if (!BuildConfig.DEFAULT_EXPERIMENT_ID.isNullOrBlank()) {
            assignedExperimentProperty.visibility = View.GONE
        } else {
            val experimentListObservable: Observable<Pair<Boolean, List<ExperimentInfo>>> = Observable.combineLatest(this.viewModel.isInjectedObservable, this.viewModel.experimentListObservable, BiFunction { t1: Boolean, t2: List<ExperimentInfo> -> Pair(t1, t2) })

            creationSubscriptions.add(
                    experimentListObservable.subscribe { (isInjected, experimentList) ->
                        if (isInjected) {
                            assignedExperimentProperty.visibility = View.GONE
                        } else {
                            assignedExperimentProperty.visibility = View.VISIBLE
                            assignedExperimentProperty.setAdapter(ExperimentSelectionSpinnerAdapter(this@TrackerDetailStructureTabFragment.act, experimentList))
                            viewModel.assignedExperimentId?.let { experimentId ->
                                assignedExperimentProperty.value = experimentList.find { it.id == experimentId }
                            }
                        }
                    }
            )

            creationSubscriptions.add(
                    viewModel.experimentIdObservable.subscribe { (experimentId) ->
                        val spinnerAdapter = (assignedExperimentProperty.adapter as? ExperimentSelectionSpinnerAdapter)
                        if (spinnerAdapter != null) {
                            val info = spinnerAdapter.items.find { it is ExperimentInfo && it.id == experimentId }
                            if (info != null) {
                                assignedExperimentProperty.value = info
                            }
                        }
                    }
            )

            creationSubscriptions.add(
                    assignedExperimentProperty.valueChanged.observable.subscribe { (_, args) ->
                        println("Assigned Experiment Id was changed : ${args}")
                        if (args is ExperimentInfo) {
                            viewModel.assignedExperimentId = args.id
                        } else {
                            viewModel.assignedExperimentId = null
                        }
                    }
            )
        }*/

        creationSubscriptions.add(
                nameProperty.valueChanged.observable.subscribe { result ->
                    this.viewModel.name = result.second
                    if (viewModel.isNameDirty) {
                        nameProperty.showEditedOnTitle = true

                        eventLogger.get().logTrackerChangeEvent(IEventLogger.SUB_EDIT, viewModel.trackerId) { content ->
                            content[IEventLogger.CONTENT_KEY_PROPERTY] = "color"
                            content[IEventLogger.CONTENT_KEY_NEWVALUE] = result.second
                        }
                    } else nameProperty.showEditedOnTitle = false
                }
        )

        creationSubscriptions.add(
                colorProperty.valueChanged.observable.subscribe { result ->
                    println("viewModel color set to ${colorProperty.value}")
                    this.viewModel.color = colorProperty.value
                    applyColorTheme(colorProperty.value, true)
                    if (viewModel.isColorDirty) {
                        colorProperty.showEditedOnTitle = true
                        eventLogger.get().logTrackerChangeEvent(IEventLogger.SUB_EDIT, viewModel.trackerId) { content ->
                            content[IEventLogger.CONTENT_KEY_PROPERTY] = "color"
                            content[IEventLogger.CONTENT_KEY_NEWVALUE] = result.second
                        }
                    } else colorProperty.showEditedOnTitle = false
                }
        )

        creationSubscriptions.add(
                isOnShortcutProperty.valueChanged.observable.subscribe { result ->
                    this.viewModel.isBookmarked = result.second
                    if (viewModel.isBookmarkedDirty) {
                        isOnShortcutProperty.showEditedOnTitle = true
                        eventLogger.get().logTrackerOnShortcutChangeEvent(viewModel.trackerId, result.second)
                    } else isOnShortcutProperty.showEditedOnTitle = false
                }
        )

        checkRequiredInformationVisibility()

        if (savedInstanceState == null) {
            tutorialManager.checkAndShowTargetPrompt("TrackerDetail_add_attribute", true, requireActivity(), newAttributeButton, R.string.msg_tutorial_add_attribute_primary, R.string.msg_tutorial_add_attribute_secondary, this.viewModel.color)
        }
    }

    fun checkRequiredInformationVisibility() {
        ui_required_information_text.visibility = if (currentAttributeViewModelList.find { it.isRequired == true } != null) {
            View.VISIBLE
        } else View.INVISIBLE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.fragment_tracker_detail_structure, container, false)

        rootScrollView = rootView.findViewById(R.id.scroll_root)

        contentContainer = rootView.findViewById(R.id.ui_content_container)

        /*
            isOnShortcutPropertyView.valueChanged += {
                sender, isOnShortcut ->
                if (tracker?.isOnShortcut != isOnShortcut) {
                    tracker?.isOnShortcut = isOnShortcut

                    tracker?.let {
                        EventLoggingManager.logTrackerOnShortcutChangeEvent(it, isOnShortcut)
                    }

                    if (tracker?.isOnShortcut == true) {
                        toastForAdded.show()
                    } else {
                        toastForRemoved.show()
                    }
                }
            }*/

        attributeListView = rootView.findViewById(R.id.ui_attribute_list)
        attributeListView.setViewIntervalDistance(dipSize(requireContext(), 8).roundToInt())
        attributeListView.emptyView = rootView.findViewById(R.id.ui_empty_list_message)

        attributeListView.addOnLayoutChangeListener { view, a, b, c, d, e, f, g, h ->
            if (scrollToBottomReserved) {
                scrollToBottom()
                scrollToBottomReserved = false
            }
        }

        //val snackBarContainer: CoordinatorLayout = rootView.findViewById(R.id.ui_snackbar_container) as CoordinatorLayout
        removalSnackbar = Snackbar.make(rootView, resources.getText(R.string.msg_attribute_removed_message), 5000)
        removalSnackbar.setAction(resources.getText(R.string.msg_undo)) { view ->
            createViewSubscriptions.add(
                    viewModel.undoRemove().subscribe({

                    }, {

                    })
            )
        }

        newAttributeButton = rootView.findViewById(R.id.ui_button_new_attribute)

        newAttributeButton.setOnClickListener {
            newAttributePanel.show(requireFragmentManager(), newAttributePanel.tag)
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attributeListView.setOnViewSwapListener { firstView, firstPosition, secondView, secondPosition ->
            attributeListAdapter.onMoveItem(firstPosition, secondPosition)
        }

        @Suppress("UNCHECKED_CAST")
        attributeListView.adapter = attributeListAdapter as AdapterLinearLayout.ViewHolderAdapter<AdapterLinearLayout.AViewHolder>

    }

    private fun applyColorTheme(color: Int, animate: Boolean) {
        (activity as TrackerDetailActivity).transitionToColor(color, animate)
        newAttributeButton.backgroundTintList = ColorStateList.valueOf(color)
        val hsv = floatArrayOf(0f, 0f, 0f)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.8f
        newAttributeButton.rippleColor = Color.HSVToColor(hsv)
    }

    override fun onStop() {
        super.onStop()
        viewHolderSubscriptions.clear()
    }


    fun showRemovalSnackbar() {
        if (!removalSnackbar.isShown) {
            removalSnackbar.show()
        }
    }


    fun openAttributeDetailActivity(position: Int) {
        val attrViewModel = currentAttributeViewModelList[position]
        startActivityForResult(AttributeDetailActivity.makeIntent(requireContext(), attrViewModel.makeFrontalChangesToDao()), REQUEST_CODE_ATTRIBUTE_DETAIL)
    }

    fun scrollToBottom() {
        rootScrollView.smoothScrollTo(0, contentContainer.measuredHeight)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ATTRIBUTE_DETAIL && resultCode == RESULT_OK && data != null) {
            val editedDao = serializationManager.get().parseAttribute(data.getStringExtra(AttributeDetailActivity.INTENT_EXTRA_SERIALIZED_ATTRIBUTE_DAO))
            val correspondingViewModel = currentAttributeViewModelList.find { it.attributeDAO.objectId == editedDao.objectId }

            if (correspondingViewModel != null) {
                correspondingViewModel.applyDaoChangesToFront(editedDao)
                if (viewModel.isEditMode) {
                    correspondingViewModel.applyChanges()
                    viewModel.clearTrackerSynchronizationFlag()
                    viewModel.registerSyncJob()
                }

                attributeListAdapter.notifyItemChanged(currentAttributeViewModelList.indexOf(correspondingViewModel))
            }
        }
    }


    inner class AttributeListAdapter : AdapterLinearLayout.ViewHolderAdapter<AttributeListAdapter.ViewHolder>() {


        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun isEmpty(): Boolean {
            return currentAttributeViewModelList.isEmpty()
        }

        override fun getItem(position: Int): Any {
            return currentAttributeViewModelList[position]
        }

        override fun getItemViewType(position: Int): Int {
            return 0
        }


        override fun hasStableIds(): Boolean {
            return false
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.attribute_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            viewHolder.bindAttribute(currentAttributeViewModelList[position])
        }

        override fun getCount(): Int {
            return currentAttributeViewModelList.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        fun onMoveItem(fromPosition: Int, toPosition: Int) {
            viewModel.moveAttribute(fromPosition, toPosition)
            //notifyItemMoved(fromPosition, toPosition)
        }

        inner class ViewHolder(val view: View) : AdapterLinearLayout.AViewHolder(view), View.OnClickListener {

            private val connectionIndicatorStubProxy: ConnectionIndicatorStubProxy

            var preview: AAttributeInputView<out Any>? = null
                set(value) {
                    if (field !== value) {
                        view.ui_preview_container.removeAllViews()
                        if (value != null) {
                            view.ui_preview_container.addView(value, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                            value.onCreate(null)
                        }

                        field = value
                    }
                }

            init {

                itemView.ui_preview_container.locked = true

                connectionIndicatorStubProxy = ConnectionIndicatorStubProxy(view, R.id.ui_connection_indicator_stub)

                itemView.ui_button_edit.setOnClickListener(this)
                itemView.ui_button_remove.setOnClickListener(this)
                itemView.ui_column_name_button.setOnClickListener(this)

                itemView.ui_button_visible.setOnClickListener(this)

                itemView.ui_preview_container.setOnClickListener(this)
            }

            override fun onClick(view: View) {
                if (view === itemView.ui_button_edit || view === itemView.ui_preview_container) {
                    openAttributeDetailActivity(adapterPosition)
                } else if (view === itemView.ui_button_remove) {
                    val attrViewModel = currentAttributeViewModelList[adapterPosition]
                    DialogHelper.makeNegativePhrasedYesNoDialogBuilder(activity!!,
                            getString(R.string.msg_remove_field),
                            String.format(getString(if (!attrViewModel.isHidden) R.string.msg_format_confirm_remove_can_hide else R.string.msg_format_confirm_move_to_trashcan_field),
                                    attrViewModel.name
                            ),
                            R.string.msg_remove, R.string.msg_cancel, {
                        viewModel.removeAttribute(attrViewModel)
                        showRemovalSnackbar()

                        eventLogger.get().logAttributeChangeEvent(IEventLogger.SUB_REMOVE, attrViewModel.attributeDAO.localId, viewModel.trackerId)

                    }, onNo = null)
                            .let {
                                if (!attrViewModel.isHidden) {
                                    it.neutralText(getString(R.string.msg_hide_field))
                                            .neutralColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                                            .onNeutral { dialog, which ->
                                                setFieldHidden(true)
                                            }
                                } else {
                                    it
                                }
                            }.show()
                } else if (view === itemView.ui_column_name_button) {

                    columnNameDialogBuilder
                            .input(null, currentAttributeViewModelList[adapterPosition].name, false) { dialog, input ->
                                if (currentAttributeViewModelList[adapterPosition].name.compareTo(input.toString()) != 0) {
                                    currentAttributeViewModelList[adapterPosition].name = input.toString()
                                    if (currentAttributeViewModelList[adapterPosition].isInDatabase) {
                                        currentAttributeViewModelList[adapterPosition].applyChanges()
                                    }
                                    attributeListAdapter.notifyItemChanged(adapterPosition)
                                }
                            }.show()
                } else if (view === itemView.ui_button_visible) {
                    setFieldHidden(!currentAttributeViewModelList[adapterPosition].isHidden)
                }
            }

            private fun setFieldHidden(isHidden: Boolean): Boolean {
                val attrViewModel = currentAttributeViewModelList[adapterPosition]
                if (attrViewModel.isHidden != isHidden) {
                    attrViewModel.isHidden = isHidden
                    if (attrViewModel.isInDatabase) {
                        attrViewModel.applyChanges()
                    }
                    attributeListAdapter.notifyItemChanged(adapterPosition)
                    eventLogger.get().logAttributeChangeEvent(IEventLogger.SUB_EDIT, attrViewModel.attributeDAO.localId, viewModel.trackerId)
                    { it ->
                        it.addProperty(IEventLogger.CONTENT_KEY_PROPERTY, "isHidden")
                        it.addProperty(IEventLogger.CONTENT_KEY_NEWVALUE, isHidden)
                    }
                    return true
                } else return false
            }

            fun bindAttribute(attributeViewModel: TrackerDetailViewModel.AttributeInformationViewModel) {
                itemView.ui_attribute_type.setImageResource(attributeViewModel.icon)
                itemView.ui_column_name.text = attributeViewModel.name

                itemView.ui_preview_container.alpha = 0.5f

                viewHolderSubscriptions.add(
                        Observable.combineLatest<Nullable<JsonObject>, Boolean, Boolean>(viewModel.lockedPropertiesObservable, attributeViewModel.isEditable,
                                BiFunction { lockedProperties: Nullable<JsonObject>, localEditable: Boolean ->
                                    val trackerEditLocked = LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.TRACKER_EDIT_ATTRIBUTES, lockedProperties.datum)
                                    val attributeEditLocked = !localEditable
                                    !trackerEditLocked && !attributeEditLocked
                                }).subscribe { isEditable: Boolean ->

                            itemView.ui_preview_container.isEnabled = isEditable
                            itemView.ui_column_name_button.isEnabled = isEditable
                            itemView.ui_button_edit.isEnabled = isEditable
                            itemView.ui_button_edit.alpha = if (isEditable) 1.0f else 0.2f
                        }
                )

                viewHolderSubscriptions.add(
                        Observable.combineLatest<Nullable<JsonObject>, Boolean, Boolean>(viewModel.lockedPropertiesObservable, attributeViewModel.isRemovable,
                                BiFunction { lockedProperties: Nullable<JsonObject>, localRemovable: Boolean ->
                                    (!LockedPropertiesHelper.isLockedNotNull(LockedPropertiesHelper.TRACKER_REMOVE_ATTRIBUTES, lockedProperties.datum) && localRemovable)
                                }).subscribe { isRemovable: Boolean ->
                            itemView.ui_button_remove.isEnabled = isRemovable
                            itemView.ui_button_remove.alpha = if (isRemovable) 1.0f else 0.2f
                        }
                )

                viewHolderSubscriptions.add(
                        attributeViewModel.isVisibilityEditable.subscribe { isEditable ->
                            itemView.ui_button_visible.isEnabled = isEditable
                            itemView.ui_button_visible.alpha = if (isEditable) 1.0f else 0.2f
                        }
                )

                viewHolderSubscriptions.add(
                        attributeViewModel.isRequiredObservable.subscribe {
                            itemView.ui_required_marker.visibility = if (it) {
                                View.VISIBLE
                            } else {
                                View.INVISIBLE
                            }

                            checkRequiredInformationVisibility()
                        }
                )

                viewHolderSubscriptions.add(
                        attributeViewModel.connectionObservable.subscribe { connection ->
                            connectionIndicatorStubProxy.onBind(connection.datum)
                        }
                )

                viewHolderSubscriptions.add(
                        attributeViewModel.nameObservable.subscribe { args ->
                            itemView.ui_column_name.text = args
                        }
                )

                viewHolderSubscriptions.add(
                        attributeViewModel.iconObservable.subscribe { args ->
                            itemView.ui_attribute_type.setImageResource(args)
                        }
                )

                viewHolderSubscriptions.add(
                        attributeViewModel.isHiddenObservable.subscribe { isHidden ->
                            itemView.ui_button_visible.setImageResource(if (isHidden) R.drawable.icon_invisible else R.drawable.icon_visible)

                            if (isHidden) {
                                itemView.ui_attribute_type.alpha = 0.2f
                                itemView.ui_column_name_button.alpha = 0.2f
                                itemView.ui_preview_container.visibility = View.GONE
                                connectionIndicatorStubProxy.setContainerVisibility(View.GONE)
                            } else {
                                itemView.ui_attribute_type.alpha = 1.0f
                                itemView.ui_column_name_button.alpha = 1.0f
                                itemView.ui_preview_container.visibility = View.VISIBLE
                                connectionIndicatorStubProxy.setContainerVisibility(View.VISIBLE)
                            }
                        }
                )

                viewHolderSubscriptions.add(
                        attributeViewModel.typeObservable.subscribe { args ->

                            preview = attributeViewFactoryManager.get().get(args).getInputView(requireContext(), true, attributeViewModel.makeFrontalChangesToDao(), preview)
                        }
                )

                viewHolderSubscriptions.add(
                        attributeViewModel.onPropertyChanged.subscribe {
                            attributeViewModel.typeObservable.value?.let { type ->
                                preview = attributeViewFactoryManager.get().get(type).getInputView(requireContext(), true, attributeViewModel.makeFrontalChangesToDao(), preview)
                            }
                        }
                )

                //viewHolderSubscriptions.add(propertySub)
            }
        }

    }

    protected fun addNewAttribute(typeInfo: AttributePresetInfo) {

        val newAttributeName = DefaultNameGenerator.generateName(typeInfo.name, currentAttributeViewModelList.map {
            it.nameObservable.value ?: ""
        }, true)
        this.viewModel.addNewAttribute(newAttributeName, typeInfo.typeId, typeInfo.processor)
        scrollToBottomReserved = true
        eventLogger.get().logAttributeChangeEvent(IEventLogger.SUB_ADD, "", viewModel.trackerId) { content ->
            content["type"] = typeInfo.typeId
            content["name"] = newAttributeName
        }
    }

}