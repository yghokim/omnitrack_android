package kr.ac.snu.hcil.omnitrack.core.datatypes

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class Timespan(from: Long, to: Long) {
    var from: Long = from
    var to: Long = to

    constructor(): this(0, 0)
}