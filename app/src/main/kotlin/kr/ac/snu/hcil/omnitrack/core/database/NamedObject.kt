package kr.ac.snu.hcil.omnitrack.core.database

import com.google.firebase.database.DatabaseReference
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
abstract class NamedObject(objectId: String?, name: String) : IDatabaseSyncedObject {
    override var isDirtySinceLastSync: Boolean = true

    val nameChangeEvent = Event<String>()

    open val objectId: String by lazy {
        objectId ?: makeNewObjectId()
    }

    abstract val databasePointRef: DatabaseReference?
    var suspendDatabaseSync: Boolean = false

    var name: String by Delegates.observable(name){
        prop, old, new ->
        if (old != new) {
            if (!suspendDatabaseSync)
                databasePointRef?.child("name")?.setValue(new)
            onNameChanged(new)
        }
    }

    init {
        isDirtySinceLastSync == null // if it is directly loaded from db
    }

    constructor() : this(null, "Noname")


    protected abstract fun makeNewObjectId(): String

    protected open fun onNameChanged(newName: String){
        isDirtySinceLastSync = true
        nameChangeEvent.invoke(this, newName)
    }
}
