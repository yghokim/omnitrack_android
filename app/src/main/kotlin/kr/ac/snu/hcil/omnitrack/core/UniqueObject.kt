package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.UUID;
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
abstract class UniqueObject(objectId: String?, dbId: Long?,  name: String) {

    val nameChangeEvent = Event<String>()

    val objectId: String by lazy {
        objectId ?: UUID.randomUUID().toString()
    }

    var dbId : Long? = dbId
        protected set
        get


    var name: String by Delegates.observable(name){
        prop, old, new ->onNameChanged(new)
    }

    constructor() : this(null, null, "Noname")


    protected open fun onNameChanged(newName: String){
        nameChangeEvent.invoke(this, newName)
    }
}
