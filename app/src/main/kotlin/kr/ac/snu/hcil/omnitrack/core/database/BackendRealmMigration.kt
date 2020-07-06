package kr.ac.snu.hcil.omnitrack.core.database

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration


class BackendRealmMigration : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        println("migrate realm from $oldVersion to $newVersion")
        val schema = realm.schema

        var oldVersionPointer = oldVersion

        if (oldVersionPointer === 0L) {
            val descriptionPanelSchema = schema.createWithPrimaryKeyField("OTDescriptionPanelDAO", "_id", String::class.java, FieldAttribute.REQUIRED)
                    .addField("trackerId", String::class.java, FieldAttribute.INDEXED, FieldAttribute.REQUIRED)
                    .addField("content", String::class.java, FieldAttribute.REQUIRED)
                    .addField("serializedCreationFlags", String::class.java, FieldAttribute.REQUIRED)

            val layoutElementSchema = schema.createWithPrimaryKeyField("OTTrackerLayoutElementDao", "id", String::class.java, FieldAttribute.REQUIRED)
                    .addField("type", String::class.java, FieldAttribute.REQUIRED)
                    .addField("reference", String::class.java, FieldAttribute.REQUIRED)


            schema.get("OTTrackerDAO")
                    ?.addRealmListField("layout", layoutElementSchema)?.setRequired("layout", false)
                    ?.addRealmListField("descriptionPanels", descriptionPanelSchema)?.setRequired("descriptionPanels", false)
                    ?.transform { obj ->
                        obj.setNull("layout")
                        obj.setNull("descriptionPanels")
                    }

            oldVersionPointer++
        }
    }

    override fun hashCode(): Int {
        return 3234
    }

    override fun equals(other: Any?): Boolean {
        return other is RealmMigration
    }
}