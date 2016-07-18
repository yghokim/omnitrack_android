package kr.ac.snu.hcil.omnitrack.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-18.
 */
abstract class OkCancelActivity : AppCompatActivity()  {

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)

        val cancelButton = findViewById(R.id.action_bar_button_back)
        cancelButton!!.setOnClickListener {
            onCanceled()
        }

        val okButton = findViewById(R.id.action_bar_button_done)
        okButton!!.setOnClickListener {
            onOk()
        }

    }

    abstract protected fun onCanceled()

    abstract protected fun onOk()
}