package kr.ac.snu.hcil.omnitrack.core.database.synchronization

import io.reactivex.Single

/**
 * Created by younghokim on 2017. 9. 27..
 */
class SyncSession(val startTimestamp: Long, val syncDataType: ESyncDataType, val direction: SyncDirection) {
    fun performSync(): Single<Pair<SyncSession, Boolean>> {
        /*
        val single = when (syncDataType) {
            ESyncDataType.ITEM -> {
                OTApp.instance.syncServerController.getItemsAfter(startTimestamp)
                        .flatMap { serverItems ->
                            println("server changes: ${serverItems}")
                            OTApp.instance.databaseManager.applyServerItemsToSync(serverItems)
                        }.flatMap { success ->
                    println("applied server changes to local. now get local changes")
                    return@flatMap OTApp.instance.databaseManager.getDirtyItemsToSync()
                }.flatMap { localChanges ->
                    println("push local changes: ${localChanges}")
                    OTApp.instance.syncServerController.postItemsDirty(localChanges)
                }.flatMap { result ->
                    println("received synchronization timestamps of local changes. : ${result}")
                    OTApp.instance.databaseManager.setItemSynchronizationFlags(result)
                }.map { success ->
                    Pair(this, success)
                }
                        .onErrorReturn { error -> error.printStackTrace(); Pair(this, false) }
                        .observeOn(Schedulers.io())
            }
            else -> Single.just(Pair(this, false))
        }

        return single.doOnSubscribe {
            println("start synchronization for ${syncDataType}")
        }.doOnSuccess {
            println("synchronization succeeded.")
        }*/
        return Single.error(Exception())
    }
}