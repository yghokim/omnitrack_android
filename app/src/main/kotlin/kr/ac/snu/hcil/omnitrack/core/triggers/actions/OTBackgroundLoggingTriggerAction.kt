package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import android.content.Context
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO

/**
 * Created by younghokim on 2017. 4. 17..
 */
class OTBackgroundLoggingTriggerAction(override var trigger: OTTriggerDAO) : OTTriggerAction() {
    override fun getSerializedString(): String? {
        return null
    }

    override fun performAction(triggerTime: Long, context: Context): Single<OTTriggerDAO> {
        println("trigger fired - logging in background")

        //Toast.makeText(OTApp.instance, "Logged!", Toast.LENGTH_SHORT).show()
        /*
        return Observable.create {
            subscriber ->
            Observable.merge(trigger.trackers./*filter { it.isValid(null) }.*/map { OTBackgroundLoggingService.log(OTApp.instance, it, ItemLoggingSource.Trigger).subscribeOn(Schedulers.newThread()) })
                    .subscribe({}, {}, {
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onNext(trigger)
                            subscriber.onCompleted()
                        }
                    })
        }*/
        TODO("Implement")
    }


}