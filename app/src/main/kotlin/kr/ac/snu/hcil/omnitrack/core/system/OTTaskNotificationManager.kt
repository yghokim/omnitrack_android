package kr.ac.snu.hcil.omnitrack.core.system

import android.content.Context
import br.com.goncalves.pugnotification.notification.PugNotification
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2017-03-10.
 */
object OTTaskNotificationManager {

    const val PROGRESS_INDETERMINATE = -1

    fun setTaskProgressNotification(context: Context, tag: String? = null, id: Int, title: String, content: String, progress: Int, largeIcon: Int = R.drawable.icon_cloud_download, smallIcon: Int = android.R.drawable.stat_sys_download) {
        val noti = PugNotification.with(context)
                .load()
                .title(title)
                .identifier(id)
                .smallIcon(smallIcon)
                .color(R.color.colorPrimary)
                .largeIcon(largeIcon)

        if (tag != null) {
            noti.tag(tag)
        }

        noti.progress()
                .value(progress, 100, progress == PROGRESS_INDETERMINATE)
                .build()
    }

    fun dismissNotification(context: Context, id: Int, tag: String? = null) {
        println("cancel notification")
        if (tag != null) {
            PugNotification.with(context).cancel(tag, id)
        } else PugNotification.with(context).cancel(id)
    }

}