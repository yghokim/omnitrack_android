package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.android.common.view.container.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactoryManager
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 4. 14..
 */
class AttributeEditDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "AttributeEditDialog"

        fun makeInstance(itemId: String, fieldLocalId: String, trackerId: String, listener: Listener): AttributeEditDialogFragment {
            val args = Bundle()
            args.putString(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            args.putString(OTApp.INTENT_EXTRA_LOCAL_ID_ATTRIBUTE, fieldLocalId)
            args.putString(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, itemId)

            val instance = AttributeEditDialogFragment()
            instance.arguments = args
            instance.addListener(listener)

            return instance
        }
    }

    interface Listener {
        fun onOkAttributeEditDialog(changed: Boolean, value: Any?, trackerId: String, fieldLocalId: String, itemId: String?)
    }

    private lateinit var container: LockableFrameLayout
    private lateinit var progressBar: View

    private var titleView: TextView? = null
    private var valueView: AFieldInputView<out Any>? = null

    private val listeners = HashSet<Listener>()

    private lateinit var viewModel: ViewModel

    lateinit var realm: Realm


    @field:[Inject Backend]
    lateinit var realmFactory: Factory<Realm>

    @Inject
    lateinit var dbManager: BackendDbManager

    @Inject
    lateinit var fieldViewFactoryManager: Lazy<OTFieldViewFactoryManager>

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
            val fieldLocalId = arguments.getString(OTApp.INTENT_EXTRA_LOCAL_ID_ATTRIBUTE)
            val itemId = arguments.getString(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM)

            viewModel.init(trackerId, itemId, fieldLocalId)
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
                                        val newValue = viewModel.frontalItemValue.value?.datum
                                        if (viewModel.validateInputValue(newValue)) {
                                            changed = viewModel.originalValue != viewModel.frontalItemValue.value?.datum

                                            for (listener in listeners) {
                                                listener.onOkAttributeEditDialog(changed, value, viewModel.trackerId, viewModel.fieldLocalId, viewModel.itemId)
                                            }
                                            dialog.dismiss()
                                        } else {
                                            Toast.makeText(context, "Not a valid input.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else dialog.dismiss()
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
                    println("item edit dialog: loaded field and item")
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
        private lateinit var field: OTFieldDAO

        lateinit var fieldLocalId: String
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

        fun makeItemView(context: Context, originalView: AFieldInputView<out Any>?): AFieldInputView<out Any> {
            val view = (context.applicationContext as OTAndroidApp).applicationComponent
                    .getAttributeViewFactoryManager().get(field.type).getInputView(context, false, field, originalView)
            view.boundAttributeObjectId = field._id
            return view
        }

        fun init(trackerId: String, itemId: String, fieldLocalId: String) {
            if (!initialized) {
                this.trackerId = trackerId
                this.fieldLocalId = fieldLocalId
                this.itemId = itemId
                val itemObservable = dbManager.get()
                        .makeItemsQuery(trackerId, null, null, realm)
                        .equalTo("_id", itemId).findFirstAsync()
                        .asFlowable<OTItemDAO>().filter { it.isValid && it.isLoaded }
                        .firstOrError()
                        .doOnSuccess { this.item = it }

                val attributeObservable = realm.where(OTFieldDAO::class.java).equalTo("trackerId", trackerId).equalTo("localId", fieldLocalId)
                        .findFirstAsync()
                        .asFlowable<OTFieldDAO>().filter { it.isValid && it.isLoaded }
                        .firstOrError()
                        .doOnSuccess {
                            this.field = it
                            attributeName.onNextIfDifferAndNotNull(it.name)
                        }

                loadingSubscription.set(Single.zip(listOf(attributeObservable, itemObservable)) { array ->
                    println("item edit dialog: loaded field and item (zip)")
                    val attribute = array[0] as OTFieldDAO
                    val item = array[1] as OTItemDAO
                    Pair(attribute, item)
                }.subscribe { (attr, item) ->
                    initialized = true
                    this.field = attr
                    this.item = item
                    originalValue = item.getValueOf(fieldLocalId)
                    frontalItemValue.onNext(Nullable(originalValue))

                    onInformationMounted.onNext(attr.type)

                })
            }
        }

        fun validateInputValue(value: Any?): Boolean {
            return this.field.isValueValid(value, getApplication())
        }

        override fun onCleared() {
            loadingSubscription.set(null)
            super.onCleared()
        }
    }
}