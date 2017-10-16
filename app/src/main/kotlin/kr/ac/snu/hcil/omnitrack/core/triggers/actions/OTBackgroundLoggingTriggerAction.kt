package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import android.content.Context
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.services.OTBackgroundLoggingService
import rx.Observable
import rx.schedulers.Schedulers

/**
 * Created by younghokim on 2017. 4. 17..
 */
class OTBackgroundLoggingTriggerAction(trigger: OTTrigger) : OTTriggerAction(trigger) {
    override fun performAction(triggerTime: Long, context: Context): Observable<OTTrigger> {
        println("trigger fired - logging in background")

        //Toast.makeText(OTApplication.app, "Logged!", Toast.LENGTH_SHORT).show()
        return Observable.create {
            subscriber ->
            Observable.merge(trigger.trackers./*filter { it.isValid(null) }.*/map { OTBackgroundLoggingService.log(OTApplication.app, it, ItemLoggingSource.Trigger).subscribeOn(Schedulers.newThread()) })
                    .subscribe({}, {}, {
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onNext(trigger)
                            subscriber.onCompleted()
                        }
                    })
        }
    }


}