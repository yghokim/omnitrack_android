package kr.ac.snu.hcil.omnitrack.services

import android.app.IntentService
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.utils.io.StringTableSheet
import rx.internal.util.SubscriptionList

/**
 * Created by Young-Ho on 3/9/2017.
 */
class OTTableExportService: IntentService("Table Export Service") {
    private val subscriptions = SubscriptionList()

    override fun onHandleIntent(intent: Intent) {
        val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        if(trackerId != null)
        {
            OTApplication.app.currentUserObservable.toBlocking().subscribe {
                user->
                val tracker = user[trackerId]
                if(tracker != null)
                {
                    val table = StringTableSheet()
                    synchronized(tracker.attributes.unObservedList)
                    {
                        tracker.attributes.unObservedList.forEach {

                        }
                    }
                }
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }
}