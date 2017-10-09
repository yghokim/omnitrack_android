package kr.ac.snu.hcil.omnitrack.ui.pages.attribute

import android.app.Activity
import android.app.AlertDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionManager
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.WizardView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.BooleanPropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.ConnectionWizardView
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.setPaddingLeft
import kr.ac.snu.hcil.omnitrack.utils.setPaddingRight
import mehdi.sakout.fancybuttons.FancyButton
import rx.subscriptions.CompositeSubscription
import java.util.*

class AttributeDetailActivity : MultiButtonActionBarActivity(R.layout.activity_attribute_detail), View.OnClickListener {

    companion object {
        const val INTENT_EXTRA_SERIALIZED_ATTRIBUTE_DAO = "serializedAttributeDao"
        const val INTENT_EXTRA_APPLY_TO_DB = "applyToDb"

        const val STATE_COLUMN_NAME = "columnName"
        const val STATE_CONNECTION = "connection"
        const val STATE_PROPERTIES = "properties"

        fun makeIntent(context: Context, dao: OTAttributeDAO, applyToDb: Boolean): Intent {

            val intent = Intent(context, AttributeDetailActivity::class.java)
            intent.putExtra(INTENT_EXTRA_SERIALIZED_ATTRIBUTE_DAO, OTAttributeDAO.parser.toJson(dao, OTAttributeDAO::class.java))
            intent.putExtra(INTENT_EXTRA_APPLY_TO_DB, applyToDb)
            return intent
        }
    }

    private lateinit var viewModel: AttributeDetailViewModel

    private val propertyViewContainer: LinearLayout by bindView(R.id.ui_list)

    private val columnNameView: ShortTextPropertyView by bindView(R.id.nameProperty)

    private val requiredView: BooleanPropertyView by bindView(R.id.requiredProperty)

    private val connectionGroup: ViewGroup by bindView(R.id.ui_connection_group)

    private val connectionFrame: FrameLayout by bindView(R.id.ui_attribute_connection_frame)

    private val newConnectionButton: FancyButton by bindView(R.id.ui_button_new_connection)
    private val connectionView: AttributeConnectionView by bindView(R.id.ui_attribute_connection)

    private val connectionViewTitle: TextView by bindView(R.id.ui_property_title_value_connection)

    private var propertyViewHorizontalMargin: Int = 0

    private val propertyViewList = ArrayList<ReadOnlyPair<String, View>>()

    private val startSubscriptions = CompositeSubscription()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(AttributeDetailViewModel::class.java)
        if (savedInstanceState == null) {

            val serializedAttributeDao = intent.getStringExtra(INTENT_EXTRA_SERIALIZED_ATTRIBUTE_DAO)
            val applyToDb = intent.getBooleanExtra(INTENT_EXTRA_APPLY_TO_DB, false)

            if (!serializedAttributeDao.isNullOrBlank()) {
                val attributeDao = OTAttributeDAO.parser.fromJson(serializedAttributeDao, OTAttributeDAO::class.java)
                viewModel.init(attributeDao)
            }
        }

        setActionBarButtonMode(Mode.SaveCancel)
        rightActionBarTextButton?.setText(R.string.msg_apply)

        propertyViewHorizontalMargin = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)

        columnNameView.addNewValidator(String.format(resources.getString(R.string.msg_format_cannot_be_blank), resources.getString(R.string.msg_column_name)), ShortTextPropertyView.NOT_EMPTY_VALIDATOR)


        columnNameView.valueChanged += {
            sender, value ->
            if (columnNameView.validate()) {
                viewModel.name = value
            }

            columnNameView.showEdited = viewModel.isNameChanged
        }

        /*requiredView.valueChanged += {
            sender, value ->
            attribute?.isRequired = value
        }*/

        connectionView.onRemoveButtonClicked += {
            sender, arg ->
            DialogHelper.makeNegativePhrasedYesNoDialogBuilder(this, "OmniTrack", resources.getString(R.string.msg_confirm_remove_connection), R.string.msg_remove, onYes = {
                viewModel.connection = null
            }).show()
        }


        newConnectionButton.setOnClickListener(this)



        creationSubscriptions.add(
                viewModel.nameObservable.subscribe { name ->
                    columnNameView.value = name
                }
        )

        creationSubscriptions.add(
                viewModel.connectionObservable.subscribe { conn ->
                    connectionView.connection = conn.datum
                    refreshConnection(true)
                }
        )

        creationSubscriptions.add(
                connectionView.onConnectionChanged.subscribe { newConnection ->
                    viewModel.connection = newConnection.datum
                    refreshConnection(false)
                }
        )

        /*

        creationSubscriptions.add(
                viewModel.onPropertyValueChanged.observeOn(AndroidSchedulers.mainThread()).subscribe { (key, value) ->
                    println("on property value changed: ${key}, ${value}")

                    @Suppress("UNCHECKED_CAST")
                    val propView = propertyViewList.find { it.first == key }?.second as? APropertyView<Any>

                    if (propView != null && value != null) {
                        propView.value = value
                    }
                }
        )*/

        creationSubscriptions.add(
                viewModel.typeObservable.subscribe { type ->
                    println("type changed: ${type}")
                    try {
                        val attrHelper = OTAttributeManager.getAttributeHelper(type)
                        propertyViewContainer.removeAllViewsInLayout()

                        val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)


                        propertyViewList.clear()
                        for (entryWithIndex in attrHelper.makePropertyViews(this).withIndex()) {
                            println("setting property view for ${entryWithIndex.value.first}")
                            val entry = entryWithIndex.value

                            propertyViewList.add(entry)

                            @Suppress("UNCHECKED_CAST")
                            val propView: APropertyView<Any> = entry.second as APropertyView<Any>
                            if (!propView.useIntrinsicPadding) {
                                propView.setPaddingLeft(propertyViewHorizontalMargin)
                                propView.setPaddingRight(propertyViewHorizontalMargin)
                            }

                            val viewModelValue = viewModel.currentPropertyTable[entry.first]
                            if (viewModelValue != null) {
                                propView.value = viewModelValue
                            }

                            //propView.value = attr.getPropertyValue(entry.first)
                            propView.valueChanged += { sender, value ->
                                if (sender is APropertyView<*>) {
                                    if (sender.validate()) {
                                        viewModel.setPropertyValue(entry.first, value)
                                        sender.showEdited = viewModel.isPropertyChanged(entry.first)
                                    }
                                }
                            }

                            println("original view id: ${entry.second.id}, type: ${entry.second.javaClass}")
                            entry.second.id = View.generateViewId()
                            println("assigned view id: ${entry.second.id}, type: ${entry.second.javaClass}")
                            propertyViewContainer.addView(entry.second, layoutParams)
                        }
                        //end: refresh properties==================================================================================

                        val dao = viewModel.attributeDao
                        if (dao != null) {
                            if (OTExternalService.getFilteredMeasureFactories {
                                it.isAttachableTo(dao)
                            }.isEmpty()) {
                                connectionGroup.visibility = View.GONE
                            } else {
                                connectionGroup.visibility = View.VISIBLE
                            }
                        }

                        if (viewModel.isValid || viewModel.attributeHelper?.propertyKeys?.isEmpty() != false) {
                            //no property
                            propertyViewContainer.setBackgroundResource(R.drawable.bottom_separator_light)
                        } else if (viewModel.attributeHelper?.propertyKeys?.size == 1) {
                            //single property
                            propertyViewContainer.setBackgroundResource(R.drawable.top_bottom_separator_light)
                        } else {
                            //multiple properties
                            propertyViewContainer.setBackgroundResource(R.drawable.expanded_view_inner_shadow)
                        }
                    } catch (ex: Exception) {

                    }
                }
        )

        setResult(Activity.RESULT_CANCELED)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        //do not call super method to prevent view hierarchy checking.
    }

    override fun onStop() {
        super.onStop()
        startSubscriptions.clear()
    }

    override fun onToolbarLeftButtonClicked() {
        if (viewModel.isChanged()) askChangeAndFinish()
        else finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (viewModel.isChanged()) {
                askChangeAndFinish(true)
                return true
            } else {
                return super.onKeyDown(keyCode, event)
            }
        } else return super.onKeyDown(keyCode, event)
    }

    fun setCurrentStateToResult() {
        setResult(Activity.RESULT_OK, Intent().putExtra(INTENT_EXTRA_SERIALIZED_ATTRIBUTE_DAO, OTAttributeDAO.parser.toJson(viewModel.attributeDao, OTAttributeDAO::class.java)))
    }

    private fun askChangeAndFinish(backInsteadOfFinish: Boolean = false) {
        DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack", resources.getString(R.string.msg_confirm_field_apply_change), R.string.msg_apply, onYes = {
            saveChanges()
            if (backInsteadOfFinish)
                super.onBackPressed()
            else {
                finish()
            }
        }, onNo = {
            if (backInsteadOfFinish)
                super.onBackPressed()
            else
                finish()
        }
        ).cancelable(true)
                .neutralText(R.string.msg_cancel)
                .show()
    }

    override fun onToolbarRightButtonClicked() {
        saveChanges()
        finish()
    }

    /*
    private fun checkIsChangedAndIndicate() {
        columnNameView.showEdited = columnNameView.value != attribute?.name
        if (attribute?.valueConnection != connectionView.connection) {
            connectionViewTitle.text = "${resources.getString(R.string.msg_value_autocompletion)}*"
        } else {
            connectionViewTitle.text = resources.getString(R.string.msg_value_autocompletion)
        }

        for (entry in propertyViewList) {
            if (entry.second is APropertyView<*>) {
                entry.second.showEdited = attribute?.getPropertyValue<Any>(entry.first) != entry.second.value!!
            }
        }
    }*/
/*
    private fun isChanged(): Boolean {

        if (columnNameView.value != attribute?.name) {
            return true
        }

        if (attribute?.valueConnection != connectionView.connection) {
            return true
        }

        for (entry in propertyViewList) {
            if (entry.second is APropertyView<*>) {
                if (attribute?.getPropertyValue<Any>(entry.first) != entry.second.value!!) {
                    return true
                }
            }
        }

        return false
    }*/

    private fun saveChanges() {
        viewModel.applyChanges()
        setResult(Activity.RESULT_OK, Intent().putExtra(INTENT_EXTRA_SERIALIZED_ATTRIBUTE_DAO, OTAttributeDAO.parser.toJson(viewModel.attributeDao, OTAttributeDAO::class.java)))
    }

    /*
    fun refresh() {
        val attr = attribute
        if (attr != null) {
            //refresh properties===============================================================================================================

            columnNameView.value = attr.name
            columnNameView.watchOriginalValue()

            propertyViewContainer.removeAllViewsInLayout()

            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)


            propertyViewList.clear()
            for (entryWithIndex in attr.makePropertyViews(this).withIndex()) {
                val entry = entryWithIndex.value

                propertyViewList.add(entry)

                @Suppress("UNCHECKED_CAST")
                val propView: APropertyView<Any> = entry.second as APropertyView<Any>
                if (!propView.useIntrinsicPadding) {
                    propView.setPaddingLeft(propertyViewHorizontalMargin)
                    propView.setPaddingRight(propertyViewHorizontalMargin)
                }

                //propView.value = attr.getPropertyValue(entry.first)
                propView.valueChanged += {
                    sender, value ->
                    if (sender is APropertyView<*>) {
                        if (sender.validate()) {
                            //attr.setPropertyValue(entry.first, value)
                        }
                    }
                }

                entry.second.id = View.generateViewId()
                propertyViewContainer.addView(entry.second, layoutParams)
            }
            //end: refresh properties==================================================================================

            //refresh connections======================================================================================

            if (OTExternalService.getFilteredMeasureFactories {
                it.isAttachableTo(attr)
            }.isEmpty()) {
                connectionGroup.visibility = View.GONE
                connectionView.connection = null
            } else {
                connectionGroup.visibility = View.VISIBLE
                connectionView.connection = attr.valueConnection
                refreshConnection(false)
            }
        }

        if (attr == null || attr.propertyKeys.isEmpty()) {
            //no property
            propertyViewContainer.setBackgroundResource(R.drawable.bottom_separator_light)
        } else if (attr.propertyKeys.size == 1) {
            //single property
            propertyViewContainer.setBackgroundResource(R.drawable.top_bottom_separator_light)
        } else {
            //multiple properties
            propertyViewContainer.setBackgroundResource(R.drawable.expanded_view_inner_shadow)
        }
    }*/

    private fun refreshConnection(animated: Boolean) {
        if (animated) {
            TransitionManager.beginDelayedTransition(connectionFrame)
        }

        if (connectionView.connection != null) {
            newConnectionButton.visibility = View.GONE
            connectionView.visibility = View.VISIBLE
        } else {
            connectionView.visibility = View.GONE
            newConnectionButton.visibility = View.VISIBLE
        }

        connectionViewTitle.text = "${resources.getString(R.string.msg_value_autocompletion)}"
    }

    override fun onClick(view: View?) {
        println(view === newConnectionButton)
        if (view === newConnectionButton) {
            /*
            val intent = Intent(this, ConnectionWizardActivity::class.java)
            startActivityForResult(intent, 0)*/
            /*
            val newConnection = OTConnection()
            newConnection.source = GoogleFitStepsFactory.makeMeasure()
            attribute?.valueConnection = newConnection
            refreshConnection(true)
            */

            val wizardView = ConnectionWizardView(this)

            val wizardDialog = AlertDialog.Builder(this)
                    .setView(wizardView)
                    .create()

            viewModel.attributeDao?.let {
                wizardView.init(it)
            }

            wizardView.setWizardListener(object : WizardView.IWizardListener {
                override fun onComplete(wizard: WizardView) {
                    connectionView.connection = wizardView.connection
                    refreshConnection(true)
                    println("new connection refreshed.")
                    wizardDialog.dismiss()
                }

                override fun onCanceled(wizard: WizardView) {
                    wizardDialog.dismiss()
                }

            })

            wizardDialog.show()
        }
    }
}
