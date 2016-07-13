package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-07-13.
 */
abstract class APropertyView<T>(layoutId: Int, context: Context, attrs: AttributeSet?)  : FrameLayout(context, attrs) {

    protected lateinit var titleView: TextView

    var validationFunc : ((T)->Boolean)? = null

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

    fun validation(func: (T)->Boolean){
        validationFunc = func
    }

    fun validate(): Boolean{
        return validationFunc?.invoke(value) ?: true
    }

    abstract var value : T

}