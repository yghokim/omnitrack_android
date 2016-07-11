package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.UUID;
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
abstract class UniqueObject(id: String?, name: String) {

    val nameChangeEvent = Event<String>()

    val objectId: String by lazy {
        id ?: UUID.randomUUID().toString()
    }


    var name: String by Delegates.observable(name){
        prop, old, new -> nameChangeEvent.invoke(this, new)
    }

    constructor() : this(null, "Nomame")

}
