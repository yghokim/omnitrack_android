package kr.ac.snu.hcil.omnitrack.core.database.configured

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTTriggerAlarmInstance
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTTriggerReminderEntry


class BackendRealmMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        val schema = realm.schema

        var oldVersionPointer = oldVersion

        //migrate 0 to 1
        if (oldVersionPointer == 0L) {
            schema.get("OTTriggerAlarmInstance")
                    ?.addField(OTTriggerAlarmInstance.FIELD_RESERVED_WHEN_DEVICE_ACTIVE, Boolean::class.java)
                    ?.transform {
                        it.set(OTTriggerAlarmInstance.FIELD_RESERVED_WHEN_DEVICE_ACTIVE, false)
                    }
            oldVersionPointer++
        }

        if (oldVersionPointer == 1L) {

            schema.get("OTTriggerReminderEntry")
                    ?.addField(OTTriggerReminderEntry.FIELD_AUTO_EXPIRY_ALARM_ID, Int::class.java)
                    ?.setNullable(OTTriggerReminderEntry.FIELD_AUTO_EXPIRY_ALARM_ID, true)
                    ?.addField(OTTriggerReminderEntry.FIELD_IS_AUTO_EXPIRY_ALARM_RESERVED_WHEN_DEVICE_ACTIVE, Boolean::class.java, FieldAttribute.INDEXED)
                    ?.transform {
                        it.setNull(OTTriggerReminderEntry.FIELD_AUTO_EXPIRY_ALARM_ID)
                        it.setBoolean(OTTriggerReminderEntry.FIELD_IS_AUTO_EXPIRY_ALARM_RESERVED_WHEN_DEVICE_ACTIVE, false)
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