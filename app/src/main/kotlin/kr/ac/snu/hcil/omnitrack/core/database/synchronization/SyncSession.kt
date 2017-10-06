package kr.ac.snu.hcil.omnitrack.core.database.synchronization

import kr.ac.snu.hcil.omnitrack.OTApplication
import rx.Single
import rx.schedulers.Schedulers

/**
 * Created by younghokim on 2017. 9. 27..
 */
class SyncSession(val startTimestamp: Long, val syncDataType: ESyncDataType, val direction: SyncDirection, val serviceStartId: Int) {
    fun performSync(): Single<Pair<SyncSession, Boolean>> {
        val single = when (syncDataType) {
            ESyncDataType.ITEM -> {
                OTApplication.app.synchronizationServerController.getItemsAfter(startTimestamp)
                        .flatMap { serverItems ->
                            println("server changes: ${serverItems}")
                            OTApplication.app.databaseManager.applyServerItemsToSync(serverItems)
                        }.flatMap { success ->
                    println("applied server changes to local. now get local changes")
                    return@flatMap OTApplication.app.databaseManager.getDirtyItemsToSync()
                }.flatMap { localChanges ->
                    println("push local changes: ${localChanges}")
                    OTApplication.app.synchronizationServerController.postItemsDirty(localChanges)
                }.flatMap { result ->
                    println("received synchronization timestamps of local changes. : ${result}")
                    OTApplication.app.databaseManager.setItemSynchronizationFlags(result)
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
        }
    }
}