package kr.ac.snu.hcil.omnitrack.core

import java.util.*

/**
 * Created by Young-Ho on 7/11/2016.
 */
class Project(id: String?, name: String) : UniqueObject(id, name) {

    private var trackers: ArrayList<Tracker> = ArrayList<Tracker>()

    constructor() : this(null, "New Project")

    override fun onPropertyChanged(propertyName: String, to: Any) {

    }
}