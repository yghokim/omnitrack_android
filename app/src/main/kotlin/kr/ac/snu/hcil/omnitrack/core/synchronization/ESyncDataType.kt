package kr.ac.snu.hcil.omnitrack.core.synchronization

/**
 * Created by younghokim on 2017. 9. 27..
 */
enum class ESyncDataType(val syncPriority: Byte) {
    //synchronization should be done in the order of tracker -> trigger -> item
    TRACKER(10),
    TRIGGER(7), ITEM(5)
}