//package kr.ac.snu.hcil.omnitrack.core.database

/**
 * Created by younghokim on 2017. 9. 28..
 */
/*
class FirebaseSynchronizationServerController: ISynchronizationServerSideAPI {

    val dbRef: DatabaseReference? get() = FirebaseDatabase.getInstance().reference
    val currentUserRef: DatabaseReference? get() {
        return OTAuthManager.userId?.let { dbRef?.child(DatabaseManager.CHILD_NAME_USERS)?.child(it) }
    }


    override fun getItemsAfter(timestamp: Long): Single<List<OTItemPOJO>> {
        return fetchTrackerIdsOfCurrentUser().flatMap{
            trackerIds->
            trackerIds.map{
                trackerId->
                val listRef = getItemListOfTrackerChild(trackerId)!!.orderByChild("synchronizedAt")
                        .startAt(timestamp.toDouble(), "timestamp")
                listRef.addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onCancelled(error: DatabaseError) {

                    }

                    override fun onDataChange(snapshot: DataSnapshot) {

                    }

                })
            }
        }
    }

    override fun postItemsDirty(items: List<OTItemPOJO>): Single<List<Pair<String, Long>>> {

    }

    private fun fetchTrackerIdsOfCurrentUser(): Single<List<String>>
    {
        return Single.create{
            subscriber->
            currentUserRef?.child(DatabaseManager.CHILD_NAME_TRACKERS)?.addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onCancelled(error: DatabaseError) {
                    error.toException().printStackTrace()
                    if(!subscriber.isUnsubscribed) {
                        subscriber.onError(error.toException())
                    }
                }

                override fun onDataChange(trackerIdsSnapshot: DataSnapshot) {
                    if(trackerIdsSnapshot.hasChildren())
                    {
                        val trackerIdList = ArrayList<String>()
                        for(child in trackerIdsSnapshot.children)
                        {
                            if(child.value == true)
                            {
                                trackerIdList.add(child.key)
                            }
                        }

                        if(!subscriber.isUnsubscribed)
                        {
                            subscriber.onSuccess(trackerIdList)
                        }
                    }
                    else{
                        if(!subscriber.isUnsubscribed)
                        {
                            subscriber.onSuccess(emptyList())
                        }
                    }
                }
            })

        }
    }

    fun getItemListContainerOfTrackerChild(trackerId: String): DatabaseReference? {
        return dbRef?.child(DatabaseManager.CHILD_NAME_ITEMS)?.child(trackerId)
    }

    fun getItemListOfTrackerChild(trackerId: String): DatabaseReference? {
        return getItemListContainerOfTrackerChild(trackerId)?.child("list")
    }
}*/