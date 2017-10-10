package kr.ac.snu.hcil.omnitrack.ui.pages

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_send_report.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseManager
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper

/**
 * Created by younghokim on 2017. 3. 19..
 */
class SendReportActivity : MultiButtonActionBarActivity(R.layout.activity_send_report) {

    companion object {
        const val KEY_IS_ANONYMOUS = "anonymous"
        const val KEY_TYPE = "type"
        const val KEY_CONTENT = "content"
    }

    private val selectedReportType: String get() = reportTypes[ui_spinner_report_type.selectedIndex]
    private val reportContent: String get() = ui_text_content.text.toString()
    private val isAnonymous: Boolean get() = ui_checkbox_anonymous.isChecked

    private lateinit var reportTypes: List<String>
    private lateinit var reportTypeTexts: List<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rightActionBarTextButton?.visibility = View.VISIBLE
        rightActionBarTextButton?.setText(R.string.msg_send)
        rightActionBarButton?.visibility = View.GONE

        val reportTypeSources = resources.getStringArray(R.array.send_report_type).map { it.split("|") }
        reportTypes = reportTypeSources.map { it.first() }
        reportTypeTexts = reportTypeSources.map { it.last() }

        ui_spinner_report_type.setItems(reportTypeTexts)

        ui_text_content.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank()) {
                    disableRightButton()
                } else {
                    enableRightButton()
                }
            }

        })

        disableRightButton()
    }

    override fun onResume() {
        super.onResume()

        if (reportContent.isNullOrBlank()) {
            disableRightButton()
        } else {
            enableRightButton()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean(KEY_IS_ANONYMOUS, isAnonymous)
        outState?.putString(KEY_TYPE, selectedReportType)
        outState?.putString(KEY_CONTENT, reportContent)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        ui_checkbox_anonymous.isChecked = savedInstanceState?.getBoolean(KEY_IS_ANONYMOUS, false)!!
        ui_spinner_report_type.selectedIndex = reportTypes.indexOf(savedInstanceState.getString(KEY_TYPE))
        ui_text_content.setText(savedInstanceState.getString(KEY_CONTENT))
    }

    private fun disableRightButton() {
        rightActionBarTextButton?.isEnabled = false
        rightActionBarTextButton?.alpha = 0.2f
    }

    private fun enableRightButton() {
        rightActionBarTextButton?.isEnabled = true
        rightActionBarTextButton?.alpha = 1.0f
    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {

        val errorDialog = DialogHelper.makeSimpleAlertBuilder(this, getString(R.string.msg_send_report_error_message))

        DialogHelper.makeYesNoDialogBuilder(this, title.toString(), getString(R.string.msg_send_report_to_omnitrack_team), R.string.msg_send, R.string.msg_no, onYes = {
            getUserOrGotoSignIn().subscribe({
                user ->
                val inquiry = HashMap<String, Any?>()
                inquiry["anonymous"] = isAnonymous
                if (!isAnonymous) {
                    OTAuthManager.reloadUserInfo()
                    inquiry["email"] = OTAuthManager.email
                    inquiry["sender"] = OTAuthManager.userId
                }

                inquiry["type"] = selectedReportType
                inquiry["content"] = reportContent

                println(inquiry)

                DatabaseManager.dbRef?.child("inquiries")?.push()?.setValue(inquiry)?.addOnCompleteListener {
                    task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, getString(R.string.msg_send_report_success_message), Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        errorDialog.show()
                    }
                } ?: errorDialog.show()
            }, {
                errorDialog.show()
            })

        }).show()
    }
}