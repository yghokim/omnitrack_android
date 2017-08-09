package kr.ac.snu.hcil.omnitrack.ui.activities

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.ColorUtils
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-18.
 */
abstract class MultiButtonActionBarActivity(val layoutId: Int) : OTActivity() {

    enum class Mode {
        OKCancel, Back, BackAndMenu, SaveCancel, ApplyCancel, None
    }

    protected val header: AppBarLayout by bindView(R.id.appbar)

    protected var leftActionBarButton: ImageButton? = null
    protected var rightActionBarButton: ImageButton? = null
    protected var rightActionBarSubButton: ImageButton? = null

    protected var rightActionBarTextButton: AppCompatButton? = null
    protected var titleView: TextView? = null

    private var isCanceled = false

    override fun onSessionLogContent(contentObject: Bundle) {
        super.onSessionLogContent(contentObject)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(layoutId)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        leftActionBarButton = findViewById<AppCompatImageButton>(R.id.ui_appbar_button_left)
        leftActionBarButton?.setOnClickListener {

            super.setResult(leftButtonResultCode)
            onToolbarLeftButtonClicked()
        }


        rightActionBarButton = findViewById(R.id.ui_appbar_button_right)

        rightActionBarButton?.setOnClickListener {

            super.setResult(rightButtonResultCode)
            onToolbarRightButtonClicked()
        }

        rightActionBarTextButton = findViewById(R.id.ui_appbar_text_button_right)
        rightActionBarTextButton?.setOnClickListener {
            super.setResult(rightButtonResultCode)
            onToolbarRightButtonClicked()
        }

        rightActionBarSubButton = findViewById(R.id.ui_appbar_button_right_sub)
        rightActionBarSubButton?.setOnClickListener {
            onToolbarRightSubButtonClicked()
        }

        titleView = findViewById(R.id.ui_appbar_title)
        titleView?.text = title

    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        titleView?.text = title
    }

    protected open val leftButtonResultCode = Activity.RESULT_CANCELED
    protected open val rightButtonResultCode = Activity.RESULT_OK

    protected fun setHeaderColor(color: Int, blend: Boolean = true) {
        val c = if (blend) getDimmedHeaderColor(color) else color
        onChangedHeaderColor(c, color)
    }

    protected open fun onChangedHeaderColor(actualColor: Int, originalColor: Int) {
        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = actualColor
        }

        header.setBackgroundColor(actualColor)
    }

    fun getDimmedHeaderColor(color: Int): Int {
        return ColorUtils.blendARGB(ContextCompat.getColor(this, R.color.colorPrimary), color, 0.6f)
    }

    fun getDimmedHeaderColorDark(color: Int): Int {
        return ColorUtils.blendARGB(Color.BLACK, getDimmedHeaderColor(color), 0.9f)
    }


    abstract protected fun onToolbarLeftButtonClicked()

    abstract protected fun onToolbarRightButtonClicked()

    protected open fun onToolbarRightSubButtonClicked() {

    }

    protected fun showRightSubButton() {
        rightActionBarSubButton?.visibility = View.VISIBLE
    }

    protected fun hideRightSubButton() {
        rightActionBarSubButton?.visibility = View.GONE
    }

    protected fun setRightSubButtonImage(res: Int) {
        rightActionBarSubButton?.setImageResource(res)
    }

    protected fun setActionBarButtonMode(mode: Mode) {
        when (mode) {
            Mode.Back -> {
                rightActionBarButton?.visibility = View.GONE
                rightActionBarTextButton?.visibility = View.GONE
                leftActionBarButton?.visibility = View.VISIBLE
                leftActionBarButton?.setImageResource(R.drawable.back_rhombus)
            }
            Mode.OKCancel -> {
                rightActionBarButton?.visibility = View.VISIBLE
                rightActionBarTextButton?.visibility = View.GONE
                leftActionBarButton?.visibility = View.VISIBLE
                rightActionBarButton?.setImageResource(R.drawable.done)
                leftActionBarButton?.setImageResource(R.drawable.cancel)
            }
            Mode.None -> {
                rightActionBarButton?.visibility = View.GONE
                rightActionBarTextButton?.visibility = View.GONE
                leftActionBarButton?.visibility = View.GONE
            }
            Mode.BackAndMenu -> {

            }
            Mode.SaveCancel -> {

                rightActionBarTextButton?.visibility = View.VISIBLE
                rightActionBarButton?.visibility = View.GONE

                leftActionBarButton?.visibility = View.VISIBLE
                leftActionBarButton?.setImageResource(R.drawable.cancel)
                rightActionBarTextButton?.setText(R.string.msg_save)
            }

            Mode.ApplyCancel -> {

                rightActionBarTextButton?.visibility = View.VISIBLE
                rightActionBarButton?.visibility = View.GONE

                leftActionBarButton?.visibility = View.VISIBLE
                leftActionBarButton?.setImageResource(R.drawable.cancel)
                rightActionBarTextButton?.setText(R.string.msg_apply)
            }

        }
    }
}