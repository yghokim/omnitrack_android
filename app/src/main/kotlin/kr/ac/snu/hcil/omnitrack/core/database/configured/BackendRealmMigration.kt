package kr.ac.snu.hcil.omnitrack.core.database.configured

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import io.realm.RealmObjectSchema
import kr.ac.snu.hcil.omnitrack.core.CreationFlagsHelper
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTTriggerAlarmInstance
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTTriggerReminderEntry


class BackendRealmMigration : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        println("migrate realm from ${oldVersion} to ${newVersion}")

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

            val entityTransform = RealmObjectSchema.Function { obj ->
                obj.getString("serializedCreationFlags")?.let { serializedCreationFlags ->
                    try {
                        CreationFlagsHelper.getExperimentId(
                                CreationFlagsHelper.parseFlags(serializedCreationFlags)
                        )?.let { experimentId ->
                            obj.setString(BackendDbManager.FIELD_EXPERIMENT_ID_IN_FLAGS, experimentId)
                        } ?: obj.setNull(BackendDbManager.FIELD_EXPERIMENT_ID_IN_FLAGS)
                    } catch (ex: Exception) {
                        obj.setNull(BackendDbManager.FIELD_EXPERIMENT_ID_IN_FLAGS)
                    }
                } ?: obj.setNull(BackendDbManager.FIELD_EXPERIMENT_ID_IN_FLAGS)
            }

            arrayOf("OTTriggerDAO", "OTTrackerDAO")
                    .forEach {
                        schema.get(it)
                                ?.addField(BackendDbManager.FIELD_EXPERIMENT_ID_IN_FLAGS, String::class.java)
                                ?.transform(entityTransform)
                    }

            oldVersionPointer++
        }

        if (oldVersionPointer == 2L) {

            arrayOf("OTTriggerSchedule", "OTTriggerReminderEntry", "OTItemDAO")
                    .forEach {
                        schema.get(it)
                                ?.addField(BackendDbManager.FIELD_METADATA_SERIALIZED, String::class.java)
                                ?.setNullable(BackendDbManager.FIELD_METADATA_SERIALIZED, true)
                                ?.transform {
                                    it.setNull(BackendDbManager.FIELD_METADATA_SERIALIZED)
                                }
                    }

            oldVersionPointer++
        }

        if (oldVersionPointer == 3L) {
            schema.get("OTTrackerDAO")
                    ?.addField(BackendDbManager.FIELD_REDIRECT_URL, String::class.java)
                    ?.transform {
                        it.setNull(BackendDbManager.FIELD_REDIRECT_URL)
                    }

            oldVersionPointer++
        }

        if (oldVersionPointer == 4L) {
            schema.get("OTUser")
                    ?.removeField("consentApproved")

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