package kr.ac.snu.hcil.android.common.view

import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat

fun Boolean.toVisibility(userGone: Boolean=true): Int{
    return if(this) View.VISIBLE else if(userGone) View.GONE else View.INVISIBLE
}

fun View.contains(x: Float, y: Float): Boolean {
    if (x < left || x > right || y < top || y > bottom) {
        return false
    }
    return true
}

fun View.getActivity(): AppCompatActivity? {

    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is AppCompatActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

fun View.setPaddingLeft(padding: Int) {
    setPadding(padding, paddingTop, paddingRight, paddingBottom)
}

fun View.setPaddingRight(padding: Int) {
    setPadding(paddingLeft, paddingTop, padding, paddingBottom)
}

fun View.setPaddingTop(padding: Int) {
    setPadding(paddingLeft, padding, paddingRight, paddingBottom)
}

fun View.setPaddingBottom(padding: Int) {
    setPadding(paddingLeft, paddingTop, paddingRight, padding)
}

fun ViewGroup.inflateMergeLayout(layout: Int) {
    LayoutInflater.from(context).inflate(layout, this, true)
}

fun ViewGroup.inflateContent(layout: Int, attach: Boolean): View {
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    return inflater.inflate(layout, this, attach)
}

fun TimePicker.getHourOfDayCompat(): Int {
    if (android.os.Build.VERSION.SDK_INT >= 23) {
        return this.hour
    } else {
        @Suppress("DEPRECATION")
        return this.currentHour
    }
}

fun TimePicker.getMinuteCompat(): Int {
    if (android.os.Build.VERSION.SDK_INT >= 23) {
        return this.minute
    } else {
        @Suppress("DEPRECATION")
        return this.currentMinute
    }
}

fun TimePicker.setHourOfDayCompat(hourOfDay: Int) {
    if (android.os.Build.VERSION.SDK_INT >= 23) {
        this.hour = hourOfDay
    } else {
        @Suppress("DEPRECATION")
        this.currentHour = hourOfDay
    }
}

fun TimePicker.setMinuteCompat(minute: Int) {
    if (android.os.Build.VERSION.SDK_INT >= 23) {
        this.minute = minute
    } else {
        @Suppress("DEPRECATION")
        this.currentMinute = minute
    }
}

fun applyTint(drawable: Drawable, color: Int): Drawable {
    val wrapped = DrawableCompat.wrap(drawable).mutate()
    DrawableCompat.setTint(wrapped, color)
    return wrapped
}