package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.LockableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.common.RxBoundDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.Subscriptions

/**
 * Created by younghokim on 2017. 4. 14..
 */
class AttributeEditDialogFragment : RxBoundDialogFragment() {

    companion object {
        const val TAG = "AttributeEditDialog"
        const val EXTRA_SERIALIZED_VALUE = "serializedValue"

        fun makeInstance(itemId: String, attributeId: String, trackerId: String, listener: Listener): AttributeEditDialogFragment {
            val args = Bundle()
            args.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            args.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE, attributeId)
            args.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, itemId)

            val instance = AttributeEditDialogFragment()
            instance.arguments = args
            instance.addListener(listener)

            return instance
        }
    }

    interface Listener {
        fun onOkAttributeEditDialog(changed: Boolean, value: Any, tracker: OTTracker, attribute: OTAttribute<out Any>, itemId: String?)
    }

    private lateinit var container: LockableFrameLayout
    private lateinit var progressBar: View

    private var titleView: TextView? = null
    private var tracker: OTTracker? = null
    private var item: OTItem? = null
    private var attribute: OTAttribute<out Any>? = null
    private var valueView: AAttributeInputView<out Any>? = null

    private var isContentLoaded: Boolean = false

    private val listeners = HashSet<Listener>()

    fun addListener(listener: Listener) {
        this.listeners.add(listener)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val view = setupViews(LayoutInflater.from(activity), savedInstanceState)

        val dialog = MaterialDialog.Builder(context)
                .customView(view, true)
                .positiveColorRes(R.color.colorPointed)
                .positiveText(R.string.msg_apply)
                .negativeColorRes(R.color.colorRed_Light)
                .negativeText(R.string.msg_cancel)
                .cancelable(false)
                .title(getString(R.string.msg_attribute_edit_dialog_title))
                .onPositive { dialog, which ->
                    val tracker = this.tracker
                    val attribute = this.attribute

                    if (tracker != null && attribute != null) {
                        val value = this.valueView?.value
                        val changed: Boolean
                        if (this.item == null) {
                            changed = true
                        } else {
                            val itemValue = this.item?.getValueOf(attribute)
                            changed = value != itemValue
                        }

                        if (value != null) {

                            for (listener in listeners) {
                                listener.onOkAttributeEditDialog(changed, value, tracker, attribute, item?.objectId)
                            }
                        }
                    }
                }
                .build()

        titleView = dialog.titleView

        return dialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = activity
        if (activity is Listener && savedInstanceState != null) {
            addListener(activity)
        }
    }

    override fun onBind(savedInstanceState: Bundle?): Subscription {
        val bundle = savedInstanceState ?: arguments

        container.locked = true
        container.alpha = 0.25f
        progressBar.visibility = View.VISIBLE

        if (bundle.containsKey(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
                && bundle.containsKey(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE)) {
            val trackerId = bundle.getString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
            val attributeId = bundle.getString(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE)
            val itemId = bundle.getString(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM)

            val activity = activity
            if (activity is OTActivity) {
                return activity.signedInUserObservable.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).doOnNext {
                    user ->
                    this.tracker = user[trackerId]
                    this.attribute = user.findAttributeByObjectId(trackerId, attributeId)

                    this.titleView?.text = String.format(resources.getString(R.string.msg_format_attribute_edit_dialog_title), this.attribute?.name)

                    this.valueView = this.attribute?.getInputView(context, false, this.valueView)
                    this.valueView?.boundAttribute = this.attribute

                    if (valueView != null) {
                        valueView?.onCreate(savedInstanceState)
                        if (this.container.getChildAt(0) !== valueView) {
                            this.container.removeAllViewsInLayout()
                            this.container.addView(valueView)
                        }
                    }

                }.flatMap {
                    user ->
                    val tracker = this.tracker
                    if (tracker != null && itemId != null) {
                        FirebaseDbHelper.getItem(tracker, itemId)
                    } else Observable.just(null)
                }.subscribe {
                    item ->
                    this.item = item

                    val cachedItemValue = bundle.getString(EXTRA_SERIALIZED_VALUE)
                    if (cachedItemValue != null) {
                        val value = TypeStringSerializationHelper.deserialize(cachedItemValue)
                        this.valueView?.setAnyValue(value)
                    } else if (item != null) {
                        if (this.attribute != null) {
                            val value = item.getValueOf(this.attribute!!)
                            if (value != null) {
                                println("value : ${value.toString()}")
                                this.valueView?.setAnyValue(value)
                            }
                        }
                    }

                    progressBar.visibility = View.GONE
                    container.alpha = 1f
                    container.locked = false
                    isContentLoaded = true
                }
            } else return Subscriptions.empty()
        } else return Subscriptions.empty()
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
        tracker = null
        item = null
        attribute = null
        valueView?.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        valueView?.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        valueView?.let {
            outState?.putString(EXTRA_SERIALIZED_VALUE, TypeStringSerializationHelper.serialize(it.value))
        }

        tracker?.let {
            outState?.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, it.objectId)
        }

        attribute?.let {
            outState?.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE, it.objectId)
        }

        item?.let {
            outState?.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, it.objectId)
        }
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
        container = view.findViewById(R.id.container) as LockableFrameLayout
        return view
    }

    fun show(fragmentManager: FragmentManager) {
        this.show(fragmentManager, TAG)
    }

}