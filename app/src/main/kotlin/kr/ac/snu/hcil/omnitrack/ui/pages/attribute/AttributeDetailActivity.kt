package kr.ac.snu.hcil.omnitrack.ui.pages.attribute

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import butterknife.bindView
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.WizardView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.BooleanPropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.ConnectionWizardView
import kr.ac.snu.hcil.omnitrack.utils.*
import java.util.*

class AttributeDetailActivity : MultiButtonActionBarActivity(R.layout.activity_attribute_detail), View.OnClickListener {

    companion object {
        const val STATE_COLUMN_NAME = "columnName"
        const val STATE_CONNECTION = "connection"
        const val STATE_PROPERTIES = "properties"
    }

    var attribute: OTAttribute<out Any>? = null


    private val propertyViewContainer: LinearLayout by bindView(R.id.ui_list)

    private val columnNameView: ShortTextPropertyView by bindView(R.id.nameProperty)

    private val requiredView: BooleanPropertyView by bindView(R.id.requiredProperty)

    private val connectionFrame: FrameLayout by bindView(R.id.ui_attribute_connection_frame)
    private val newConnectionButton: Button by bindView(R.id.ui_button_new_connection)
    private val connectionView: AttributeConnectionView by  bindView(R.id.ui_attribute_connection)

    private var propertyViewHorizontalMargin: Int = 0

    private val propertyViewList = ArrayList<ReadOnlyPair<Int?, View>>()

    override fun onSessionLogContent(contentObject: JsonObject) {
        super.onSessionLogContent(contentObject)
        contentObject.addProperty("attribute_id", attribute?.objectId)
        contentObject.addProperty("attribute_name", attribute?.name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.OKCancel)

        propertyViewHorizontalMargin = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)

        columnNameView.addNewValidator(String.format(resources.getString(R.string.msg_format_cannot_be_blank), resources.getString(R.string.msg_column_name)), ShortTextPropertyView.NOT_EMPTY_VALIDATOR)

        /*
        columnNameView.valueChanged += {
            sender, value ->
            if (columnNameView.validate())
                attribute?.name = value
        }*/
/*
        requiredView.valueChanged += {
            sender, value ->
            attribute?.isRequired = value
        }*/

        connectionView.onRemoveButtonClicked += {
            sender, arg ->
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack", resources.getString(R.string.msg_confirm_remove_connection), {
                connectionView.connection = null
                refreshConnection(true)
            }).show()
        }

        InterfaceHelper.removeButtonTextDecoration(newConnectionButton)

        newConnectionButton.setOnClickListener(this)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        println("restore state")

        savedInstanceState.getString(STATE_COLUMN_NAME)?.let {
            columnNameView.value = it
        }

        connectionView.connection = null
        savedInstanceState.getString(STATE_CONNECTION)?.let {
            try {
                connectionView.connection = OTConnection(it)
            } catch(e: Exception) {
                connectionView.connection = null
            }
        }


        refreshConnection(false)

        savedInstanceState.getStringArray(STATE_PROPERTIES)?.let {
            for (entry in it.withIndex()) {
                (propertyViewList[entry.index].second as? APropertyView<out Any>)?.setSerializedValue(entry.value)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_COLUMN_NAME, columnNameView.value)
        outState.putString(STATE_CONNECTION, connectionView.connection?.getSerializedString())

        outState.putStringArray(STATE_PROPERTIES, propertyViewList.map { (it.second as APropertyView<out Any>).getSerializedValue() }.toTypedArray())
    }

    override fun onStart() {
        super.onStart()

        if (intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE) != null) {
            attribute = OTApplication.app.currentUser.findAttributeByObjectId(intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE))
            refresh()
            applyAttributeToPropertyView(attribute!!)
        }
    }

    override fun onToolbarLeftButtonClicked() {
        DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack", resources.getString(R.string.msg_confirm_apply_change),
                {
                    saveChanges()
                    finish()
                },
                {
                    finish()
                }
        ).cancelListener { finish() }
                .show()
    }

    override fun onToolbarRightButtonClicked() {
        saveChanges()
        finish()
    }

    private fun isChanged(): Boolean {
        return true
    }

    private fun saveChanges() {
        if (isChanged()) {
            if (columnNameView.validate())
                attribute?.name = columnNameView.value

            attribute?.valueConnection = connectionView.connection

            for (entry in propertyViewList) {
                if (entry.first != null) {
                    if (entry.second is APropertyView<*>) {
                        if (entry.second.validate()) {
                            attribute?.setPropertyValue(entry.first, entry.second.value!!)
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        (application as OTApplication).syncUserToDb()
    }

    fun refresh() {
        val attr = attribute
        if (attr != null) {
            //refresh properties===============================================================================================================

            columnNameView.value = attr.name

            propertyViewContainer.removeAllViewsInLayout()

            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)


            propertyViewList.clear()
            for (entryWithIndex in attr.makePropertyViews(this).withIndex()) {
                val entry = entryWithIndex.value

                if (entry.first != null) {
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
                }

                entry.second.id = View.generateViewId()
                propertyViewContainer.addView(entry.second, layoutParams)
            }
            //end: refresh properties==================================================================================

            //refresh connections======================================================================================
            connectionView.connection = attr.valueConnection
            refreshConnection(false)
        }

        if (attr == null || attr.propertyKeys.size == 0) {
            //no property
            propertyViewContainer.setBackgroundResource(R.drawable.bottom_separator_light)
        } else if (attr.propertyKeys.size == 1) {
            //single property
            propertyViewContainer.setBackgroundResource(R.drawable.top_bottom_separator_light)
        } else {
            //multiple properties
            propertyViewContainer.setBackgroundResource(R.drawable.expanded_view_inner_shadow)
        }
    }

    private fun applyAttributeToPropertyView(attribute: OTAttribute<out Any>) {
        for (propertyViewEntry in propertyViewList) {
            @Suppress("UNCHECKED_CAST")
            val propView: APropertyView<Any> = propertyViewEntry.second as APropertyView<Any>

            propView.value = attribute.getPropertyValue(propertyViewEntry.first!!)
        }
    }

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

            wizardView.init(attribute!!)

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                println("result: Ok")
            }
        }
    }
}
