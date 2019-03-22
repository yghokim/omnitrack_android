package kr.ac.snu.hcil.omnitrack.core.triggers.measures

import android.content.Context
import io.reactivex.Observable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO

class OTDataDrivenTriggerMetadataMeasureLogicImpl(context: Context) : OTItemMetadataMeasureFactoryLogicImpl(context) {
    override fun checkAvailability(tracker: OTTrackerDAO, invalidMessages: MutableList<CharSequence>?): Boolean {
        return checkAvailabilityOfTriggerCount(tracker.liveTriggersQuery?.equalTo("conditionType", OTTriggerDAO.CONDITION_TYPE_DATA)?.count()
                ?: 0, invalidMessages)
    }

    private fun checkAvailabilityOfTriggerCount(dataDrivenTriggerCount: Long, invalidMessages: MutableList<CharSequence>?): Boolean {
        return if (dataDrivenTriggerCount > 0) {
            true
        } else {
            invalidMessages?.add(context.getString(R.string.msg_trigger_data_measure_error_no_data_driven))
            false
        }
    }


    fun makeAvailabilityCheckObservable(attribute: OTAttributeDAO): Observable<Pair<Boolean, List<CharSequence>?>> {
        if (attribute.trackerId != null) {
            return Observable.defer {
                val realm = realmProvider.get()
                val tracker = dbManager.get().getTrackerQueryWithId(attribute.trackerId!!, realm).findFirst()
                if (tracker != null) {
                    return@defer tracker.liveTriggersQuery!!
                            .equalTo("conditionType", OTTriggerDAO.CONDITION_TYPE_DATA)
                            .findAllAsync().asFlowable().filter { it.isLoaded && it.isValid }.toObservable()
                            .map {
                                val invalidMessages = ArrayList<CharSequence>()
                                val valid = checkAvailabilityOfTriggerCount(it.count().toLong(), invalidMessages)
                                return@map Pair(valid, invalidMessages)
                            }.doAfterTerminate {
                                realm.close()
                            }
                } else {
                    realm.close()
                    return@defer Observable.just(Pair(false, listOf("No tracker in the database.")))
                }
            }
        } else return Observable.just(Pair(true, null))
    }

}