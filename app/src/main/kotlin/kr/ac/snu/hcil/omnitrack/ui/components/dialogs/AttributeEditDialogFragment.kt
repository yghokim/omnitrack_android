package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import dagger.internal.Factory
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.ui.components.common.container.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 4. 14..
 */
class AttributeEditDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "AttributeEditDialog"

        fun makeInstance(itemId: String, attributeLocalId: String, trackerId: String, listener: Listener): AttributeEditDialogFragment {
            val args = Bundle()
            args.putString(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            args.putString(OTApp.INTENT_EXTRA_LOCAL_ID_ATTRIBUTE, attributeLocalId)
            args.putString(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, itemId)

            val instance = AttributeEditDialogFragment()
            instance.arguments = args
            instance.addListener(listener)

            return instance
        }
    }

    interface Listener {
        fun onOkAttributeEditDialog(changed: Boolean, value: Any?, trackerId: String, attributeLocalId: String, itemId: String?)
    }

    private lateinit var container: LockableFrameLayout
    private lateinit var progressBar: View

    private var titleView: TextView? = null
    private var valueView: AAttributeInputView<out Any>? = null

    private val listeners = HashSet<Listener>()

    private lateinit var viewModel: ViewModel

    lateinit var realm: Realm


    @field:[Inject Backend]
    lateinit var realmFactory: Factory<Realm>

    @Inject
    lateinit var dbManager: BackendDbManager

    private val subscriptions = CompositeDisposable()

    fun addListener(listener: Listener) {
        this.listeners.add(listener)
    }

    override fun onDetach() {
        super.onDetach()
        subscriptions.clear()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //retainInstance = true
        (requireActivity().application as OTAndroidApp).applicationComponent.inject(this)
        realm = realmFactory.get()

        viewModel = ViewModelProviders.of(this).get(ViewModel::class.java)

        val arguments = this.arguments
        if (arguments != null) {
            val trackerId = arguments.getString(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)
            val attributeLocalId = arguments.getString(OTApp.INTENT_EXTRA_LOCAL_ID_ATTRIBUTE)
            val itemId = arguments.getString(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM)

            viewModel.init(trackerId, itemId, attributeLocalId)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = setupViews(LayoutInflater.from(activity), savedInstanceState)

        val dialog = MaterialDialog.Builder(context!!)
                .customView(view, true)
                .positiveColorRes(R.color.colorPointed)
                .positiveText(R.string.msg_apply)
                .negativeColorRes(R.color.colorRed_Light)
                .negativeText(R.string.msg_cancel)
                .cancelable(false)
                .title(getString(R.string.msg_attribute_edit_dialog_title))
                .autoDismiss(false)
                .onNegative { dialog, which ->
                    dialog.dismiss()
                }
                .onPositive { dialog, which ->
                    this.valueView?.let { valueView ->
                        subscriptions.add(
                                valueView.forceApplyValueAsync().subscribe { (value) ->
                                    val changed: Boolean
                                    if (viewModel.initialized) {
                                        changed = viewModel.originalValue != viewModel.frontalItemValue.value?.datum
                                        for (listener in listeners) {
                                            listener.onOkAttributeEditDialog(changed, value, viewModel.trackerId, viewModel.attributeLocalId, viewModel.itemId)
                                        }
                                    }

                                    dialog.dismiss()
                                }
                        )
                    }
                }
                .build()

        titleView = dialog.titleView

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        realm.close()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = activity
        if (activity is Listener && savedInstanceState != null) {
            addListener(activity)
        }

        container.locked = true
        container.alpha = 0.25f
        progressBar.visibility = View.VISIBLE

        subscriptions.add(
                viewModel.attributeName.subscribe {
                    this.titleView?.text = String.format(resources.getString(R.string.msg_format_attribute_edit_dialog_title), it)
                }
        )

        subscriptions.add(
                viewModel.onInformationMounted.subscribe {
                    println("item edit dialog: loaded attribute and item")
                    this.valueView = viewModel.makeItemView(requireActivity(), this.valueView)
                    if (valueView != null) {
                        valueView?.onCreate(savedInstanceState)
                        valueView?.onResume()
                        if (this.container.getChildAt(0) !== valueView) {
                            this.container.removeAllViewsInLayout()
                            valueView?.id = View.generateViewId()
                            this.container.addView(valueView)
                        }


                        subscriptions.add(
                                this.valueView!!.valueChanged.observable.subscribe { (sender, value) ->
                                    viewModel.frontalItemValue.onNextIfDifferAndNotNull(Nullable(value))
                                }
                        )
                    }

                    subscriptions.add(
                            viewModel.frontalItemValue.subscribe { (value) ->
                                println("value : $value")
                                this.valueView?.setAnyValue(value)

                                progressBar.visibility = View.GONE
                                container.alpha = 1f
                                container.locked = false
                            }
                    )

                }
        )

    }

    override fun onLowMemory() {
        super.onLowMemory()
        valueView?.onLowMemory()
    }

    override fun onResume() {
        super.onResume()
        valueView?.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        valueView?.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        valueView?.onPause()
    }

    private fun setupViews(inflater: LayoutInflater, savedInstanceState: Bundle?): View {
        /*
    val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    val view = FrameLayout(activity)
    view.layoutParams = layoutParams
    val horizontalPadding = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
    val verticalPadding = resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)
    view.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
    */
        val view = inflater.inflate(R.layout.layout_item_field_edit_dialog, null, false)
        progressBar = view.findViewById(R.id.ui_progress_bar)
        container = view.findViewById(R.id.container)
        return view
    }

    fun show(fragmentManager: FragmentManager) {
        this.show(fragmentManager, TAG)
    }

    class ViewModel(app: Application) : RealmViewModel(app) {

        lateinit var trackerId: String
            private set
        private lateinit var item: OTItemDAO
        private lateinit var attribute: OTAttributeDAO

        lateinit var attributeLocalId: String
            private set
        lateinit var itemId: String
            private set

        var initialized = false
            private set

        private val loadingSubscription = SerialDisposable()

        val onInformationMounted = BehaviorSubject.create<Int>()

        var originalValue: Any? = null
        val frontalItemValue = BehaviorSubject.create<Nullable<Any>>()

        val attributeName = BehaviorSubject.create<String>()

        fun makeItemView(context: Context, originalView: AAttributeInputView<out Any>?): AAttributeInputView<out Any> {
            val view = attribute.getHelper(getApplication()).getInputView(context, false, attribute, originalView)
            view.boundAttributeObjectId = attribute.objectId
            return view
        }

        fun init(trackerId: String, itemId: String, attributeLocalId: String) {
            if (!initialized) {
                this.trackerId = trackerId
                this.attributeLocalId = attributeLocalId
                this.itemId = itemId
                val itemObservable = dbManager.get()
                        .makeItemsQuery(trackerId, null, null, realm)
                        .equalTo("objectId", itemId).findFirstAsync()
                        .asFlowable<OTItemDAO>().filter { it.isValid && it.isLoaded }
                        .firstOrError()
                        .doOnSuccess { this.item = it }

                val attributeObservable = realm.where(OTAttributeDAO::class.java).equalTo("trackerId", trackerId).equalTo("localId", attributeLocalId)
                        .findFirstAsync()
                        .asFlowable<OTAttributeDAO>().filter { it.isValid && it.isLoaded }
                        .firstOrError()
                        .doOnSuccess {
                            this.attribute = it
                            attributeName.onNextIfDifferAndNotNull(it.name)
                        }

                loadingSubscription.set(Single.zip(listOf(attributeObservable, itemObservable)) { array ->
                    println("item edit dialog: loaded attribute and item (zip)")
                    val attribute = array[0] as OTAttributeDAO
                    val item = array[1] as OTItemDAO
                    Pair(attribute, item)
                }.subscribe { (attr, item) ->
                    initialized = true
                    this.attribute = attr
                    this.item = item
                    originalValue = item.getValueOf(attributeLocalId)
                    frontalItemValue.onNext(Nullable(originalValue))

                    onInformationMounted.onNext(attr.type)

                })
            }
        }

        override fun onCleared() {
            loadingSubscription.set(null)
            super.onCleared()
        }
    }
}