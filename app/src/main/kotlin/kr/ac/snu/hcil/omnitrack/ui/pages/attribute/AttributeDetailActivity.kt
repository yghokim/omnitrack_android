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
import android.widget.LinearLayout
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.android.synthetic.main.activity_attribute_detail.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.WizardView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.ConnectionWizardView
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.setPaddingLeft
import kr.ac.snu.hcil.omnitrack.utils.setPaddingRight
import java.util.*

class AttributeDetailActivity : MultiButtonActionBarActivity(R.layout.activity_attribute_detail), View.OnClickListener {

    companion object {
        const val INTENT_EXTRA_SERIALIZED_ATTRIBUTE_DAO = "serializedAttributeDao"
        const val INTENT_EXTRA_APPLY_TO_DB = "applyToDb"

        fun makeIntent(context: Context, dao: OTAttributeDAO, applyToDb: Boolean): Intent {

            val intent = Intent(context, AttributeDetailActivity::class.java)
            intent.putExtra(INTENT_EXTRA_SERIALIZED_ATTRIBUTE_DAO, OTApp.instance.daoSerializationComponent.manager().get().serializeAttribute(dao))
            intent.putExtra(INTENT_EXTRA_APPLY_TO_DB, applyToDb)
            return intent
        }
    }

    private lateinit var viewModel: AttributeDetailViewModel

    private var propertyViewHorizontalMargin: Int = 0

    private val currentSpinnerEntries = arrayListOf(
            FallbackSpinnerEntry(OTAttributeDAO.DEFAULT_VALUE_POLICY_NULL, OTApp.getString(R.string.msg_attribute_fallback_policy_null)),
            FallbackSpinnerEntry(OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_LAST_ITEM, OTApp.getString(R.string.msg_attribute_fallback_policy_last)),
            FallbackSpinnerEntry(OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_PRESET, OTApp.getString(R.string.msg_attribute_fallback_policy_preset))
    )

    private val propertyViewList = ArrayList<ReadOnlyPair<String, View>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(AttributeDetailViewModel::class.java)
        if (!viewModel.isInitialized) {
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

        nameProperty.addNewValidator(String.format(resources.getString(R.string.msg_format_cannot_be_blank), resources.getString(R.string.msg_column_name)), ShortTextPropertyView.NOT_EMPTY_VALIDATOR)


        nameProperty.valueChanged += {
            sender, value ->
            if (nameProperty.validate()) {
                viewModel.name = value
            }

            nameProperty.showEditedOnTitle = viewModel.isNameDirty
        }

        requiredProperty.valueChanged += {
            sender, value ->
            viewModel.isRequired = value

            requiredProperty.showEditedOnTitle = viewModel.isRequiredDirty
        }

        ui_attribute_connection.onRemoveButtonClicked += {
            sender, arg ->
            DialogHelper.makeNegativePhrasedYesNoDialogBuilder(this, "OmniTrack", resources.getString(R.string.msg_confirm_remove_connection), R.string.msg_remove, onYes = {
                viewModel.connection = null
            }).show()
        }


        ui_button_new_connection.setOnClickListener(this)

        ui_fallback_policy_spinner.setOnItemSelectedListener(object : MaterialSpinner.OnItemSelectedListener<FallbackSpinnerEntry> {
            override fun onItemSelected(view: MaterialSpinner?, position: Int, id: Long, item: FallbackSpinnerEntry?) {
                if (item != null) {
                    viewModel.defaultValuePolicy = item.id
                }
            }

        })


        creationSubscriptions.add(
                viewModel.nameObservable.subscribe { name ->
                    nameProperty.value = name
                    nameProperty.showEditedOnTitle = viewModel.isNameDirty
                }
        )

        creationSubscriptions.add(
                viewModel.isRequiredObservable.subscribe { isRequired ->
                    requiredProperty.value = isRequired
                    requiredProperty.showEditedOnTitle = viewModel.isRequiredDirty
                }
        )

        creationSubscriptions.add(
                viewModel.connectionObservable.subscribe { conn ->
                    ui_attribute_connection.connection = conn.datum
                    refreshConnection(true)

                    if (viewModel.isConnectionDirty) {
                        ui_property_title_value_connection.text = "${OTApp.getString(R.string.msg_value_autocompletion)}*"
                    } else {
                        ui_property_title_value_connection.setText(R.string.msg_value_autocompletion)
                    }
                }
        )

        creationSubscriptions.add(
                ui_attribute_connection.onConnectionChanged.subscribe { newConnection ->
                    viewModel.connection = newConnection.datum
                    refreshConnection(false)
                }
        )

        creationSubscriptions.add(
                viewModel.defaultValuePolicyObservable.subscribe { policy ->
                    ui_fallback_policy_spinner.selectedIndex = currentSpinnerEntries.indexOf(currentSpinnerEntries.find { it.id == policy })

                    if (viewModel.isDefaultValuePolicyDirty) {
                        ui_title_fallback.text = "${OTApp.getString(R.string.msg_attribute_fallback_policy)}*"
                    } else ui_title_fallback.setText(R.string.msg_attribute_fallback_policy)
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
                        ui_list.removeAllViewsInLayout()

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

                            val originalViewModelValue = viewModel.getOriginalPropertyValue(entry.first)
                            val viewModelFrontalValue = viewModel.currentPropertyTable[entry.first]
                            if (viewModelFrontalValue != null) {
                                propView.value = viewModelFrontalValue
                                if (originalViewModelValue != null) {
                                    propView.compareAndShowEdited(originalViewModelValue)
                                }
                            }

                            //propView.value = attr.getPropertyValue(entry.first)
                            propView.valueChanged += { sender, value ->
                                if (sender is APropertyView<*>) {
                                    if (sender.validate()) {
                                        viewModel.setPropertyValue(entry.first, value)
                                        if (originalViewModelValue != null) {
                                            sender.compareAndShowEditedAny(originalViewModelValue)
                                        }
                                    }
                                }
                            }

                            println("original view id: ${entry.second.id}, type: ${entry.second.javaClass}")
                            entry.second.id = View.generateViewId()
                            println("assigned view id: ${entry.second.id}, type: ${entry.second.javaClass}")
                            ui_list.addView(entry.second, layoutParams)
                        }
                        //end: refresh properties==================================================================================

                        val dao = viewModel.makeFrontalChangesToDao()
                        if (dao != null) {
                            if (OTExternalService.getFilteredMeasureFactories {
                                it.isAttachableTo(dao)
                            }.isEmpty()) {
                                ui_connection_group.visibility = View.GONE
                            } else {
                                ui_connection_group.visibility = View.VISIBLE
                            }

                            //refresh Fallback spinners============================================================================
                            refreshFallbackSpinner(dao, attrHelper)
                        }

                        if (viewModel.isValid || viewModel.attributeHelper?.propertyKeys?.isEmpty() != false) {
                            //no property
                            ui_list.setBackgroundResource(R.drawable.bottom_separator_light)
                        } else if (viewModel.attributeHelper?.propertyKeys?.size == 1) {
                            //single property
                            ui_list.setBackgroundResource(R.drawable.top_bottom_separator_light)
                        } else {
                            //multiple properties
                            ui_list.setBackgroundResource(R.drawable.expanded_view_inner_shadow)
                        }
                    } catch (ex: Exception) {

                    }
                }
        )

        setResult(Activity.RESULT_CANCELED)
    }

    private fun refreshFallbackSpinner(dao: OTAttributeDAO, attrHelper: OTAttributeHelper) {
        currentSpinnerEntries.clear()
        currentSpinnerEntries.addAll(attrHelper.supportedFallbackPolicies.map { FallbackSpinnerEntry(it.key, it.value.toString()) })

        ui_fallback_policy_spinner.setItems(currentSpinnerEntries)
        ui_fallback_policy_spinner.selectedIndex = currentSpinnerEntries.indexOf(currentSpinnerEntries.find { it.id == dao.fallbackValuePolicy })

        println("refreshed fallback spinner. policy: ${dao.fallbackValuePolicy}, index: ${currentSpinnerEntries.indexOf(currentSpinnerEntries.find { it.id == dao.fallbackValuePolicy })}")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        //do not call super method to prevent view hierarchy checking.
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

    private fun askChangeAndFinish(backInsteadOfFinish: Boolean = false) {
        DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack",
                String.format(OTApp.getString(R.string.msg_format_confirm_apply_change), OTApp.getString(R.string.msg_text_field)),
                yesLabel = R.string.msg_save,
                noLabel = R.string.msg_do_not_save,
                onYes = {
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
        columnNameView.showEditedOnTitle = columnNameView.value != attribute?.name
        if (attribute?.valueConnection != connectionView.connection) {
            connectionViewTitle.text = "${resources.getString(R.string.msg_value_autocompletion)}*"
        } else {
            connectionViewTitle.text = resources.getString(R.string.msg_value_autocompletion)
        }

        for (entry in propertyViewList) {
            if (entry.second is APropertyView<*>) {
                entry.second.showEditedOnTitle = attribute?.getPropertyValue<Any>(entry.first) != entry.second.value!!
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
        setResult(Activity.RESULT_OK, Intent().putExtra(INTENT_EXTRA_SERIALIZED_ATTRIBUTE_DAO, OTAttributeDAO.parser.toJson(viewModel.attributeDAO, OTAttributeDAO::class.java)))
    }

    /*
    fun refresh() {
        val attr = attribute
        if (attr != null) {
            //refresh properties===============================================================================================================

            columnNameView.value = attr.name
            columnNameView.watchOriginalValue()

            ui_list.removeAllViewsInLayout()

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
                ui_list.addView(entry.second, layoutParams)
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
            ui_list.setBackgroundResource(R.drawable.bottom_separator_light)
        } else if (attr.propertyKeys.size == 1) {
            //single property
            ui_list.setBackgroundResource(R.drawable.top_bottom_separator_light)
        } else {
            //multiple properties
            ui_list.setBackgroundResource(R.drawable.expanded_view_inner_shadow)
        }
    }*/

    private fun refreshConnection(animated: Boolean) {
        if (animated) {
            TransitionManager.beginDelayedTransition(ui_attribute_connection_frame)
        }

        if (ui_attribute_connection.connection != null) {
            ui_button_new_connection.visibility = View.GONE
            ui_attribute_connection.visibility = View.VISIBLE
        } else {
            ui_attribute_connection.visibility = View.GONE
            ui_button_new_connection.visibility = View.VISIBLE
        }
    }

    override fun onClick(view: View?) {
        println(view === ui_button_new_connection)
        if (view === ui_button_new_connection) {
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

            viewModel.attributeDAO?.let {
                wizardView.init(it)
            }

            wizardView.setWizardListener(object : WizardView.IWizardListener {
                override fun onComplete(wizard: WizardView) {
                    ui_attribute_connection.connection = wizardView.connection
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

    override fun onDestroy() {
        super.onDestroy()
        currentSpinnerEntries.clear()
    }

    data class FallbackSpinnerEntry(val id: Int, val text: String) {
        override fun toString(): String {
            return text
        }
    }
}
