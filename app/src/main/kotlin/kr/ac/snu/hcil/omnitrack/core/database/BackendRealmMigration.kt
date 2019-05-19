package kr.ac.snu.hcil.omnitrack.core.database

import io.realm.DynamicRealm
import io.realm.RealmMigration


class BackendRealmMigration : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        println("migrate realm from $oldVersion to $newVersion")
        val schema = realm.schema
    }

    override fun hashCode(): Int {
        return 3234
    }

    override fun equals(other: Any?): Boolean {
        return other is RealmMigration
    }
}