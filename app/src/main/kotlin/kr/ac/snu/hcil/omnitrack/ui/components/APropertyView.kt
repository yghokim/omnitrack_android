package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
abstract class APropertyView<T>(layoutId: Int, context: Context, attrs: AttributeSet?)  : FrameLayout(context, attrs) {

    protected lateinit var titleView: TextView


    private val validators : ArrayList<Pair<CharSequence?, (T)->Boolean>> = ArrayList<Pair<CharSequence?, (T)->Boolean>>()

    protected val validationErrorMessageList = ArrayList<CharSequence>()

    var title : CharSequence
        get() = titleView.text
        set(value){
            titleView.text = value
        }

    constructor(layoutId: Int, context: Context): this(layoutId, context, null)

    init{
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addView(inflater.inflate(layoutId, this, false))

        titleView = findViewById(R.id.title) as TextView

    }

    fun addNewValidator(failedMessage: CharSequence?, func: (T)->Boolean){
        validators.add(Pair<CharSequence?, (T)->Boolean>(failedMessage, func))
    }

    fun validate(): Boolean{
        validationErrorMessageList.clear()

        var passed = true
        for(entry in validators)
        {
            if(entry.second(value) == false)
            {
                passed = false
                if( entry.first != null) {
                    validationErrorMessageList.add(entry.first!!)
                }
            }
        }

        onValidated(passed)
        return passed
    }

    open fun onValidated(result: Boolean)
    {
        ;
    }

    abstract var value : T

    abstract fun focus(): Unit
}