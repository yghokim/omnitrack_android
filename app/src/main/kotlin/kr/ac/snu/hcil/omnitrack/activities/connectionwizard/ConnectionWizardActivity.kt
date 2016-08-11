package kr.ac.snu.hcil.omnitrack.activities.connectionwizard

import android.os.Bundle
import android.view.View
import android.widget.Button
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.activities.MultiButtonActionBarActivity

class ConnectionWizardActivity : MultiButtonActionBarActivity(R.layout.activity_connection_wizard), View.OnClickListener {

    private lateinit var leftButton: Button
    private lateinit var rightButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.Back)
        setTitle(R.string.title_activity_new_connection)

        leftButton = findViewById(R.id.ui_button_bottom_left) as Button
        leftButton.text = "Prev"
        rightButton = findViewById(R.id.ui_button_bottom_right) as Button
        rightButton.text = "Next"

        leftButton.setOnClickListener(this)
        rightButton.setOnClickListener(this)
    }

    override fun onToolbarLeftButtonClicked() {
        finishAfterTransition()
    }

    override fun onToolbarRightButtonClicked() {
    }

    override fun onClick(p0: View?) {

        if (p0 === leftButton) {

        } else if (p0 === rightButton) {
            setResult(RESULT_OK)
            finish()
        }

    }

}
