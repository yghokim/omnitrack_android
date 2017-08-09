package kr.ac.snu.hcil.omnitrack.core.database

import com.google.firebase.database.DatabaseReference
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
abstract class NamedObject(objectId: String?, name: String) : IDatabaseSyncedObject {

    companion object {
        const val PROPERTY_NAME = "name"
    }

    override var isDirtySinceLastSync: Boolean = true

    open val objectId: String by lazy {
        objectId ?: makeNewObjectId()
    }

    val nameChanged = SerializedSubject(PublishSubject.create<Pair<NamedObject, String>>())

    abstract val databasePointRef: DatabaseReference?
    var suspendDatabaseSync: Boolean = false

    var name: String by Delegates.observable(name) {
        prop, old, new ->
        if (old != new) {
            if (!suspendDatabaseSync)
                databasePointRef?.child(PROPERTY_NAME)?.setValue(new)

            nameChanged.onNext(Pair(this, new))
            onNameChanged(new)
        }
    }

    init {
        isDirtySinceLastSync == null // if it is directly loaded from db
    }

    constructor() : this(null, "Noname")


    protected abstract fun makeNewObjectId(): String

    protected open fun onNameChanged(newName: String) {
    }
}
