package kr.ac.snu.hcil.omnitrack.core.database.synchronization

/**
 * Created by younghokim on 2017. 9. 27..
 */
interface IServerSideAPI {
    fun pullItemsAfter(timestamp: Long)

}