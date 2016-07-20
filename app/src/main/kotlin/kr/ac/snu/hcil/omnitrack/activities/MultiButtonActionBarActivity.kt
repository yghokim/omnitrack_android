package kr.ac.snu.hcil.omnitrack.activities

import android.app.Activity
import android.content.Context
import android.opengl.Visibility
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-18.
 */
abstract class MultiButtonActionBarActivity(val layoutId: Int) : AppCompatActivity()  {

    protected var leftActionBarButton: ImageButton?=null
    protected var rightActionBarButton: ImageButton?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(layoutId)

        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)

        leftActionBarButton = findViewById(R.id.action_bar_button_left) as ImageButton
        leftActionBarButton?.setOnClickListener {

            super.setResult(leftButtonResultCode)
            onLeftButtonClicked()
        }


        rightActionBarButton = findViewById(R.id.action_bar_button_right) as ImageButton

        rightActionBarButton?.setOnClickListener {

            super.setResult(rightButtonResultCode)
            onRightButtonClicked()
        }

        val titleView = findViewById(R.id.action_bar_title) as TextView?
        titleView?.setText(title)
    }

    protected open val leftButtonResultCode = Activity.RESULT_CANCELED
    protected open val rightButtonResultCode = Activity.RESULT_OK


    abstract protected fun onLeftButtonClicked()

    abstract protected fun onRightButtonClicked()
}