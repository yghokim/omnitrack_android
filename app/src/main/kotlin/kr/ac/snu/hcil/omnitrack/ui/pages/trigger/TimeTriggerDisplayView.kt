package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Chronometer
import android.widget.LinearLayout
import android.widget.TextView
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import java.util.regex.Pattern
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 8/25/2016.
 */
class TimeTriggerDisplayView : LinearLayout {
/*
    companion object {
        private val tickerSubscription = SerialDisposable()

        private val shownInstances = HashSet<TimeTriggerDisplayView>()

        private fun registerViewInstance(view: TimeTriggerDisplayView)
        {
            if(!shownInstances.contains(view))
            {
                shownInstances.add(view)
                if(tickerSubscription.get() == null || tickerSubscription.isDisposed == true)
                {
                    tickerSubscription.set(
                            Observable.interval(1, TimeUnit.SECONDS)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe {
                                        sequence->
                                        for (instance in shownInstances) {
                                            instance.refreshNextTriggerTimeTextWithNow(System.currentTimeMillis())
                                        }
                                    }
                    )
                }
            }
        }

        private fun unregisterViewInstance(view: TimeTriggerDisplayView)
        {
            if(shownInstances.remove(view) && shownInstances.isEmpty())
            {
                tickerSubscription.dispose()
                tickerSubscription.set(null)
            }
        }
    }*/

    private val mainView: TextView by bindView(R.id.ui_value)
    private val nextTriggerView: Chronometer by bindView(R.id.ui_next_trigger)

    init {
        orientation = VERTICAL

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.trigger_display_time, this, true)

        nextTriggerView.visibility = GONE
        nextTriggerView.setOnChronometerTickListener { chronometer ->
            refreshNextTriggerTimeTextWithNow(System.currentTimeMillis())
        }
    }

    var nextTriggerTime: Long? by Delegates.observable(null as Long?) {
        prop, old, new ->

        if (old != new) {
            if (new != null) {
                nextTriggerView.text = DateUtils.getRelativeTimeSpanString(new, System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_ALL)
                nextTriggerView.visibility = View.VISIBLE
                nextTriggerView.start()
            } else {
                nextTriggerView.visibility = View.GONE
                nextTriggerView.stop()
            }
        }
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun refreshNextTriggerTimeTextWithNow(now: Long) {
        if (nextTriggerTime != null)
            nextTriggerView.text = DateUtils.getRelativeTimeSpanString(nextTriggerTime!!, now, 0, DateUtils.FORMAT_ABBREV_ALL)
    }

    fun setAlarmInformation(hour: Int, minute: Int, amPm: Int) {
        val amPmText = " " + if (amPm == 0) {
            resources.getString(R.string.time_am)
        } else {
            resources.getString(R.string.time_pm)
        }
        val sb = SpannableString(String.format("%02d", if (hour == 0) 12 else hour) + ":" + String.format("%02d", minute) + amPmText)
        sb.setSpan(AbsoluteSizeSpan(resources.getDimensionPixelSize(R.dimen.number_digit_size)), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val amPmTextIndex = sb.indexOf(amPmText)
        sb.setSpan(AbsoluteSizeSpan(resources.getDimensionPixelSize(R.dimen.number_unit_size)), amPmTextIndex, amPmTextIndex + amPmText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(TypefaceSpan("sans-serif-normal"), amPmTextIndex, amPmTextIndex + amPmText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.textColorMid)), amPmTextIndex, amPmTextIndex + amPmText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)


        mainView.text = sb
    }

    fun setIntervalInformation(duration: Int) {

        val text = SpannableString(TimeHelper.durationToText(duration * 1000L, true, context))

        val matcher = Pattern.compile("\\D+").matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = if (matcher.group().endsWith(' ')) {
                matcher.end() - 1
            } else matcher.end()
            text.setSpan(AbsoluteSizeSpan(resources.getDimensionPixelSize(R.dimen.number_unit_size_small)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            text.setSpan(TypefaceSpan("sans-serif-normal"), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.textColorMid)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        mainView.text = text
    }
/*
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerViewInstance(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unregisterViewInstance(this)
    }*/

}