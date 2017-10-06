package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.arch.lifecycle.ViewModelProviders
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.widget.NestedScrollView
import android.support.v7.util.DiffUtil
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import butterknife.bindView
import com.afollestad.materialdialogs.MaterialDialog
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.AttributePresetInfo
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.components.common.AdapterLinearLayout
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.BooleanPropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ColorPalettePropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.tutorial.TutorialManager
import kr.ac.snu.hcil.omnitrack.ui.pages.ConnectionIndicatorStubProxy
import kr.ac.snu.hcil.omnitrack.utils.DefaultNameGenerator
import kr.ac.snu.hcil.omnitrack.utils.dipRound
import rx.subscriptions.CompositeSubscription

/**
 * Created by Young-Ho Kim on 16. 7. 29
 */
class TrackerDetailStructureTabFragment : OTFragment() {

    companion object {
        val toastForAdded by lazy { Toast.makeText(OTApplication.app, R.string.msg_shortcut_added, Toast.LENGTH_SHORT) }
        val toastForRemoved by lazy { Toast.makeText(OTApplication.app, R.string.msg_shortcut_removed, Toast.LENGTH_SHORT) }
    }

    lateinit private var rootScrollView: NestedScrollView

    lateinit private var attributeListView: AdapterLinearLayout
    //lateinit private var attributeListAdapter: AttributeListAdapter

    lateinit private var attributeListItemTouchHelper: ItemTouchHelper

    private lateinit var namePropertyView: ShortTextPropertyView
    private lateinit var colorPropertyView: ColorPalettePropertyView
    private lateinit var isOnShortcutPropertyView: BooleanPropertyView
    //private lateinit var fab: FloatingActionButton

    private lateinit var contentContainer: ViewGroup

    private lateinit var removalSnackbar: Snackbar

    private lateinit var newAttributeButton: FloatingActionButton

    private var scrollToBottomReserved = false

    private lateinit var viewModel: TrackerDetailViewModel

    private val currentAttributeViewModelList = ArrayList<TrackerDetailViewModel.AttributeInformationViewModel>()

    private val attributeListAdapter = AttributeListAdapter()

    private val creationSubscriptions = CompositeSubscription()

    private val viewHolderSubscriptions = CompositeSubscription()

    private val newAttributePanel: FieldPresetSelectionBottomSheetFragment by lazy {

        val newAttributePanel = FieldPresetSelectionBottomSheetFragment()
        newAttributePanel.callback = object : FieldPresetSelectionBottomSheetFragment.Callback {
            override fun onAttributePermittedToAdd(typeInfo: AttributePresetInfo) {
                addNewAttribute(typeInfo)
            }
        }

        newAttributePanel
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        this.viewModel = ViewModelProviders.of(activity).get(TrackerDetailViewModel::class.java)

        //set UI
        namePropertyView.value = this.viewModel.name
        isOnShortcutPropertyView.value = this.viewModel.isBookmarked
        applyColorTheme(this.viewModel.color, false)
        colorPropertyView.value = this.viewModel.color

        currentAttributeViewModelList.clear()

        creationSubscriptions.add(
                this.viewModel.attributeViewModelListObservable.subscribe { newList ->

                    println("new attribute viewmodel list: ${newList}, old: ${currentAttributeViewModelList}")

                    val diffResult = DiffUtil.calculateDiff(
                            TrackerDetailViewModel.AttributeViewModelListDiffUtilCallback(currentAttributeViewModelList, newList)
                    )

                    currentAttributeViewModelList.clear()
                    currentAttributeViewModelList.addAll(newList)

                    diffResult.dispatchUpdatesTo(attributeListAdapter)
                }
        )

        creationSubscriptions.add(
                namePropertyView.valueChanged.observable.subscribe { result ->
                    this.viewModel.name = result.second
                }
        )

        creationSubscriptions.add(
                colorPropertyView.valueChanged.observable.subscribe { result ->
                    println("viewModel color set to ${colorPropertyView.value}")
                    this.viewModel.color = colorPropertyView.value
                    applyColorTheme(colorPropertyView.value, true)
                }
        )

        creationSubscriptions.add(
                isOnShortcutPropertyView.valueChanged.observable.subscribe { result ->
                    this.viewModel.isBookmarked = result.second
                }
        )


        if (savedInstanceState == null) {
            TutorialManager.checkAndShowTargetPrompt("TrackerDetail_add_attribute", true, this.activity, newAttributeButton, R.string.msg_tutorial_add_attribute_primary, R.string.msg_tutorial_add_attribute_secondary, this.viewModel.color)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_tracker_detail_structure, container, false)

        rootScrollView = rootView.findViewById(R.id.scroll_root)

        contentContainer = rootView.findViewById(R.id.ui_content_container)

        namePropertyView = rootView.findViewById(R.id.nameProperty)
        namePropertyView.addNewValidator("Name cannot be empty.", ShortTextPropertyView.NOT_EMPTY_VALIDATOR)

        colorPropertyView = rootView.findViewById(R.id.colorProperty)

        isOnShortcutPropertyView = rootView.findViewById(R.id.isOnShortcutProperty)
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
        attributeListView.setViewIntervalDistance(dipRound(8))
        attributeListView.emptyView = rootView.findViewById(R.id.ui_empty_list_message)
        val layoutManager = object : LinearLayoutManager(context, VERTICAL, false) {
            override fun canScrollVertically(): Boolean {
                return false
            }

            override fun onItemsAdded(recyclerView: RecyclerView?, positionStart: Int, itemCount: Int) {
                super.onItemsAdded(recyclerView, positionStart, itemCount)
                scrollToBottom()
            }
        }

        attributeListView.addOnLayoutChangeListener { view, a, b, c, d, e, f, g, h ->
            if (scrollToBottomReserved) {
                scrollToBottom()
                scrollToBottomReserved = false
            }
        }

        //val snackBarContainer: CoordinatorLayout = rootView.findViewById(R.id.ui_snackbar_container) as CoordinatorLayout
        removalSnackbar = Snackbar.make(rootView, resources.getText(R.string.msg_attribute_removed_message), 5000)
        removalSnackbar.setAction(resources.getText(R.string.msg_undo)) {
            view ->
            //attributeListAdapter.undoRemove()
        }

        newAttributeButton = rootView.findViewById(R.id.ui_button_new_attribute)

        newAttributeButton.setOnClickListener {
            newAttributePanel.show(this.fragmentManager, newAttributePanel.tag)
        }

        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        attributeListView.setOnViewSwapListener { firstView, firstPosition, secondView, secondPosition ->
            attributeListAdapter.onMoveItem(firstPosition, secondPosition)
        }

        attributeListView.adapter = attributeListAdapter as AdapterLinearLayout.ViewHolderAdapter<AdapterLinearLayout.AViewHolder>

    }

    override fun onDestroyView() {
        super.onDestroyView()
        creationSubscriptions.clear()

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
        //TODO Open Attribute Detail Activity
        //startActivityOnDelay(AttributeDetailActivity.makeIntent(activity, tracker!!, tracker!!.attributes.get(position)))
    }

    fun scrollToBottom() {
        rootScrollView.smoothScrollTo(0, contentContainer.measuredHeight)
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

        private var removed: OTAttribute<out Any>? = null
        private var removedPosition: Int = -1

        init {
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

        fun undoRemove() {
            if (removed != null) {
                //tracker?.attributes?.addAt(removed!!, removedPosition)
                //notifyItemInserted(removedPosition)
            }
        }

        fun clearTrashcan() {
            removed = null
        }

        fun onMoveItem(fromPosition: Int, toPosition: Int) {
            viewModel.moveAttribute(fromPosition, toPosition)
            //notifyItemMoved(fromPosition, toPosition)
        }

        inner class ViewHolder(val view: View) : AdapterLinearLayout.AViewHolder(view), View.OnClickListener {

            private val previewContainer: LockableFrameLayout by bindView(R.id.ui_preview_container)
            private val columnNameView: TextView by bindView(R.id.ui_column_name)
            private val typeIconView: AppCompatImageView by bindView(R.id.ui_attribute_type)

            private val editButton: ImageButton by bindView(R.id.ui_button_edit)
            private val removeButton: ImageButton by bindView(R.id.ui_button_remove)

            private val requiredMarker: View by bindView(R.id.ui_required_marker)

            private val columnNameButton: View by bindView(R.id.ui_column_name_button)

            private val connectionIndicatorStubProxy: ConnectionIndicatorStubProxy

            private var connectionIndicator: View? = null
            private var connectionSourceNameView: TextView? = null

            private val columnNameChangeDialog: MaterialDialog.Builder

            var preview: AAttributeInputView<out Any>? = null
                get
                set(value) {
                    if (field !== value) {
                        previewContainer.removeAllViews()
                        if (value != null) {
                            previewContainer.addView(value, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                        }

                        field = value
                    }
                }

            init {

                previewContainer.locked = true

                connectionIndicatorStubProxy = ConnectionIndicatorStubProxy(view, R.id.ui_connection_indicator_stub)

                editButton.setOnClickListener(this)
                removeButton.setOnClickListener(this)
                columnNameButton.setOnClickListener(this)

                columnNameChangeDialog = MaterialDialog.Builder(this.view.context)
                        .title(R.string.msg_change_field_name)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .inputRangeRes(1, 20, R.color.colorRed)
                        .cancelable(true)
                        .negativeText(R.string.msg_cancel)

                previewContainer.setOnClickListener(this)
            }

            override fun onClick(view: View) {
                if (view === editButton || view === previewContainer) {
                    openAttributeDetailActivity(adapterPosition)
                } else if (view === removeButton) {
                    /*
                    val tracker = tracker
                    tracker?.let {
                        DialogHelper.makeNegativePhrasedYesNoDialogBuilder(this@TrackerDetailStructureTabFragment.context,
                                getString(R.string.msg_remove_field),
                                String.format(getString(R.string.msg_format_confirm_remove_field),
                                        tracker.attributes[adapterPosition].name
                                ),
                                R.string.msg_remove, R.string.msg_cancel, {
                            removed = tracker.attributes[adapterPosition]
                            removedPosition = adapterPosition
                            tracker.attributes.remove(tracker.attributes[adapterPosition])
                            showRemovalSnackbar()
                            if (removed != null) {
                                EventLoggingManager.logAttributeChangeEvent(EventLoggingManager.EVENT_NAME_CHANGE_ATTRIBUTE_REMOVE, removed!!.typeId, removed!!.objectId, tracker.objectId)
                            }
                        }, onNo = null).show()
                    }*/
                } else if (view === columnNameButton) {

                        columnNameChangeDialog
                                .input(null, currentAttributeViewModelList[adapterPosition].name, false) {
                                    dialog, input ->
                                    if (currentAttributeViewModelList[adapterPosition].name.compareTo(input.toString()) != 0) {
                                        currentAttributeViewModelList[adapterPosition].name = input.toString()
                                        attributeListAdapter.notifyItemChanged(adapterPosition)
                                    }
                                }.show()
                }
            }

            fun bindAttribute(attributeViewModel: TrackerDetailViewModel.AttributeInformationViewModel) {
                //typeIconView.setImageResource(attribute.typeSmallIconResourceId)
                columnNameView.text = attributeViewModel.name
                /*
                requiredMarker.visibility = if (attribute.isRequired) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }

                previewContainer.alpha = 0.5f
                preview = attribute.getInputView(context, true, preview)

                connectionIndicatorStubProxy.onBind(attribute)

                val propertySub =
                        attribute.propertyValueChangedSubject.subscribe {
                            preview = attribute.getInputView(context, true, preview)
                        }*/

                val nameSub =
                        attributeViewModel.nameObservable.subscribe {
                            args ->
                            columnNameView.text = args
                        }

                //viewHolderSubscriptions.add(propertySub)
                viewHolderSubscriptions.add(nameSub)
            }
        }

    }

    protected fun addNewAttribute(typeInfo: AttributePresetInfo) {

        val newAttributeName = DefaultNameGenerator.generateName(typeInfo.name, currentAttributeViewModelList.map { it.nameObservable.value }, true)
        this.viewModel.addNewAttribute(newAttributeName, typeInfo.typeId)
        scrollToBottomReserved = true

        //EventLoggingManager.logAttributeChangeEvent(EventLoggingManager.EVENT_NAME_CHANGE_ATTRIBUTE_ADD, newAttribute.typeId, newAttribute.objectId, tracker.objectId)
    }

}