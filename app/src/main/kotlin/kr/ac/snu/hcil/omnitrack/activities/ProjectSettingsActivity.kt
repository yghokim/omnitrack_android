package kr.ac.snu.hcil.omnitrack.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTProject
import kr.ac.snu.hcil.omnitrack.ui.components.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper

class ProjectSettingsActivity : UserSyncedActivity() {

    private lateinit var project : OTProject

    private lateinit var namePropertyView : ShortTextPropertyView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_settings)

        namePropertyView = findViewById(R.id.nameProperty) as ShortTextPropertyView


        val cancelButton = findViewById(R.id.button_cancel)
        val okButton = findViewById(R.id.button_ok)

        cancelButton?.setOnClickListener {
            finish()
        }

        okButton?.setOnClickListener {
            DialogHelper.makeYesNoDialogBuilder(this, project.name, resources.getString(R.string.msg_confirm_apply_settings), {-> applySettings(); finish() }).show()
        }
    }

    private fun applySettings(){
        project.name = namePropertyView.value
    }

    override fun onStart() {
        super.onStart()
        project = user.projects.filter{ it.objectId == intent.getStringExtra("projectId") }.first()
        namePropertyView.value = project.name
    }
}
