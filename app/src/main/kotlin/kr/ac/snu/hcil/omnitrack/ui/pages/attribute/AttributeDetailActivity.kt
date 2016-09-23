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
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.WizardView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.APropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.ConnectionWizardView
import kr.ac.snu.hcil.omnitrack.utils.*
import java.util.*

class AttributeDetailActivity : MultiButtonActionBarActivity(R.layout.activity_attribute_detail), View.OnClickListener {


    var attribute: OTAttribute<out Any>? = null


    private lateinit var propertyViewContainer: LinearLayout

    private lateinit var columnNameView: ShortTextPropertyView

    private lateinit var connectionFrame: FrameLayout
    private lateinit var newConnectionButton: Button
    private lateinit var connectionView: AttributeConnectionView

    private var propertyViewHorizontalMargin: Int = 0

    private val propertyViewList = ArrayList<ReadOnlyPair<Int?, View>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.Back)

        propertyViewHorizontalMargin = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)

        propertyViewContainer = findViewById(R.id.ui_list) as LinearLayout

        columnNameView = findViewById(R.id.nameProperty) as ShortTextPropertyView
        columnNameView.title = resources.getString(R.string.msg_column_name)

        columnNameView.addNewValidator(String.format(resources.getString(R.string.msg_format_cannot_be_blank), resources.getString(R.string.msg_column_name)), ShortTextPropertyView.NOT_EMPTY_VALIDATOR)

        columnNameView.valueChanged += {
            sender, value ->
            if (columnNameView.validate())
                attribute?.name = value
        }

        connectionFrame = findViewById(R.id.ui_attribute_connection_frame) as FrameLayout
        connectionView = findViewById(R.id.ui_attribute_connection) as AttributeConnectionView
        connectionView.onRemoveButtonClicked += {
            sender, arg ->
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack", resources.getString(R.string.msg_confirm_remove_connection), {
                attribute?.valueConnection = null
                refreshConnection(true)
            }).show()
        }

        newConnectionButton = findViewById(R.id.ui_button_new_connection) as Button

        InterfaceHelper.removeButtonTextDecoration(newConnectionButton)

        newConnectionButton.setOnClickListener(this)

    }

    override fun onStart() {
        super.onStart()
        if (intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE) != null) {
            attribute = OTApplication.app.currentUser.findAttributeByObjectId(intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE))
            refresh()
        }
    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {
    }

    override fun onPause() {
        super.onPause()
        for (entry in propertyViewList) {
            if (entry.first != null) {
                if (entry.second is APropertyView<*>) {
                    if (entry.second.validate()) {
                        attribute?.setPropertyValue(entry.first!!, entry.second.value!!)
                    }
                }
            }
        }
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

                    propView.value = attr.getPropertyValue(entry.first)
                    propView.valueChanged += {
                        sender, value ->
                        if (sender is APropertyView<*>) {
                            if (sender.validate()) {
                                //attr.setPropertyValue(entry.first, value)
                            }
                        }
                    }
                }

                propertyViewContainer.addView(entry.second, layoutParams)
            }
            //end: refresh properties==================================================================================
            refreshConnection(false)

            //refresh connections======================================================================================


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

    private fun refreshConnection(animated: Boolean) {
        if (animated) {
            TransitionManager.beginDelayedTransition(connectionFrame)
        }

        if (attribute?.valueConnection != null) {
            newConnectionButton.visibility = View.GONE
            connectionView.visibility = View.VISIBLE
            connectionView.connection = attribute?.valueConnection
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
                    attribute?.valueConnection = wizardView.connection
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
