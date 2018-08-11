package kr.ac.snu.hcil.omnitrack.utils.time

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import java.text.SimpleDateFormat

class LocalTimeFormats(context: Context) {
    val FORMAT_DATETIME: SimpleDateFormat by lazy { SimpleDateFormat(context.resources.getString(R.string.dateformat_minute)) }
    val FORMAT_DAY: SimpleDateFormat by lazy { SimpleDateFormat(context.resources.getString(R.string.msg_date_format_scope_day)) }
    val FORMAT_MONTH: SimpleDateFormat by lazy { SimpleDateFormat(context.resources.getString(R.string.msg_date_format_scope_month)) }
    val FORMAT_MONTH_SHORT: SimpleDateFormat by lazy { SimpleDateFormat(context.resources.getString(R.string.msg_date_format_scope_month_short)) }
    val FORMAT_DAY_WITHOUT_YEAR: SimpleDateFormat by lazy { SimpleDateFormat(context.resources.getString(R.string.msg_date_format_scope_day_for_week)) }
    val FORMAT_DAY_OF_WEEK_SHORT: SimpleDateFormat by lazy { SimpleDateFormat(context.resources.getString(R.string.dateformat_day_of_week_short)) }
}