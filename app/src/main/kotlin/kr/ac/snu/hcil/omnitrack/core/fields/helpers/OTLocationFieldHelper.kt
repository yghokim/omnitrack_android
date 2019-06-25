package kr.ac.snu.hcil.omnitrack.core.fields.helpers

import android.Manifest
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import com.patloew.rxlocation.RxLocation
import io.reactivex.BackpressureStrategy
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.fields.FallbackPolicyResolver
import kr.ac.snu.hcil.omnitrack.core.fields.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import java.util.concurrent.TimeUnit

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTLocationFieldHelper(context: Context) : OTFieldHelper(context) {

    class CurrentLocationFallbackResolver(context: Context) : FallbackPolicyResolver(context, R.string.msg_intrinsic_location, isValueVolatile = true) {
        override fun getFallbackValue(field: OTFieldDAO, realm: Realm): Single<Nullable<out Any>> {
            return Single.defer {
                val rxLocation = RxLocation(context)
                val request = LocationRequest.create() //standard GMS LocationRequest
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setNumUpdates(2)
                        .setInterval(100)
                        .setMaxWaitTime(500)
                        .setExpirationDuration(2000)

                try {
                    return@defer rxLocation.location().updates(request, BackpressureStrategy.LATEST)
                            .map { location -> LatLng(location.latitude, location.longitude) }
                            .timeout(2L, TimeUnit.SECONDS, rxLocation.location().lastLocation().map { location -> LatLng(location.latitude, location.longitude) }.toFlowable())
                            .firstOrError()
                            .map { loc -> Nullable(loc) }
                            .onErrorReturn { err -> Nullable(LatLng(0.0, 0.0)) }
                } catch (ex: SecurityException) {
                    ex.printStackTrace()
                    return@defer Single.just(Nullable(LatLng(0.0, 0.0)))
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    return@defer Single.just(Nullable(LatLng(0.0, 0.0)))
                }
            }
        }
    }

    private val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

    override val supportedFallbackPolicies: LinkedHashMap<String, FallbackPolicyResolver> by lazy {
        val original = super.supportedFallbackPolicies
        original[OTFieldDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE] = CurrentLocationFallbackResolver(context)
        original.remove(OTFieldDAO.DEFAULT_VALUE_POLICY_NULL)
        return@lazy original
    }

    override fun getValueNumericCharacteristics(field: OTFieldDAO): NumericCharacteristics = NumericCharacteristics(false, true)

    override fun getTypeNameResourceId(field: OTFieldDAO): Int = R.string.type_location_name

    override fun getTypeSmallIconResourceId(field: OTFieldDAO): Int = R.drawable.icon_small_location

    override fun getRequiredPermissions(field: OTFieldDAO?): Array<String>? = permissions

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_LATITUDE_LONGITUDE

    override fun formatAttributeValue(field: OTFieldDAO, value: Any): CharSequence {
        return if (value is LatLng) {
            "${Location.convert(value.latitude, Location.FORMAT_DEGREES)},${Location.convert(value.longitude, Location.FORMAT_DEGREES)}"
        } else super.formatAttributeValue(field, value)
    }

    override fun initialize(field: OTFieldDAO) {
        field.fallbackValuePolicy = OTFieldDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE
    }
}