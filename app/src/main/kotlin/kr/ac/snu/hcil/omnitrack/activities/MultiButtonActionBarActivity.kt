package kr.ac.snu.hcil.omnitrack.activities

import android.app.Activity
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

    enum class Mode {
        OKCancel, Back, BackAndMenu, None
    }

    protected var leftActionBarButton: ImageButton?=null
    protected var rightActionBarButton: ImageButton?=null
    protected var titleView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(layoutId)

        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)

        leftActionBarButton = findViewById(R.id.ui_appbar_button_left) as ImageButton
        leftActionBarButton?.setOnClickListener {

            super.setResult(leftButtonResultCode)
            onToolbarLeftButtonClicked()
        }


        rightActionBarButton = findViewById(R.id.ui_appbar_button_right) as ImageButton

        rightActionBarButton?.setOnClickListener {

            super.setResult(rightButtonResultCode)
            onToolbarRightButtonClicked()
        }

        titleView = findViewById(R.id.ui_appbar_title) as TextView?
        titleView?.setText(title)

    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        titleView?.setText(title)
    }

    protected open val leftButtonResultCode = Activity.RESULT_CANCELED
    protected open val rightButtonResultCode = Activity.RESULT_OK


    abstract protected fun onToolbarLeftButtonClicked()

    abstract protected fun onToolbarRightButtonClicked()

    protected fun setActionBarButtonMode(mode: Mode) {
        when (mode) {
            Mode.Back -> {
                rightActionBarButton?.visibility = View.GONE
                leftActionBarButton?.visibility = View.VISIBLE
                leftActionBarButton?.setImageResource(R.drawable.back_rhombus)
            }
            Mode.OKCancel -> {
                rightActionBarButton?.visibility = View.VISIBLE
                leftActionBarButton?.visibility = View.VISIBLE
                rightActionBarButton?.setImageResource(R.drawable.done)
                leftActionBarButton?.setImageResource(R.drawable.cancel)
            }
            Mode.None -> {
                rightActionBarButton?.visibility = View.GONE
                leftActionBarButton?.visibility = View.GONE
            }
            Mode.BackAndMenu -> {

            }
        }
    }
}