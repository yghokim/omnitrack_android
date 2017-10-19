package kr.ac.snu.hcil.omnitrack.core.database

import io.reactivex.subjects.PublishSubject
import kotlin.properties.Delegates

/**
 * Created by Young-Ho on 7/11/2016.
 */
abstract class NamedObject(objectId: String?, name: String) {

    companion object {
        const val PROPERTY_NAME = "name"
    }

    open val objectId: String by lazy {
        objectId ?: makeNewObjectId()
    }

    val nameChanged = (PublishSubject.create<Pair<NamedObject, String>>())
    var suspendDatabaseSync: Boolean = false

    var name: String by Delegates.observable(name) {
        prop, old, new ->
        if (old != new) {

            if (!suspendDatabaseSync) save()

            nameChanged.onNext(Pair(this, new))
            onNameChanged(new)
        }
    }

    init {
    }

    constructor() : this(null, "Noname")


    protected abstract fun makeNewObjectId(): String

    protected open fun onNameChanged(newName: String) {
    }

    abstract fun save()
}
