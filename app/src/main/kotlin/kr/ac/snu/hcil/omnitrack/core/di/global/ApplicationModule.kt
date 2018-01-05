package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.di.configured.*
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.inject.Qualifier
import javax.inject.Singleton


/**
 * Created by Young-Ho on 10/31/2017.
 */
@Module(subcomponents = [
    ConfiguredAppComponent::class,
    FirebaseComponent::class,
    ScheduledJobComponent::class,
    TriggerSystemComponent::class,
    ResearchComponent::class
])
class ApplicationModule(private val mApp: OTApp) {

    @Provides
    @Singleton
    fun application(): OTApp {
        return mApp
    }

    @Provides
    @Singleton
    fun wrappedContext(): Context
    {
        return mApp.contextCompat
    }

    @Provides
    @Singleton
    fun wrappedResources(): Resources
    {
        return mApp.resourcesWrapped
    }

    @Provides
    @Singleton
    @Default
    fun sharedPreferences(): SharedPreferences
    {
        return PreferenceManager.getDefaultSharedPreferences(mApp)
    }

    @Provides
    @Singleton
    @DeviceId
    fun deviceId(): String {
        return mApp.deviceId
    }

    @Provides
    @Singleton
    @Sha1FingerPrint
    fun getCertificateSHA1Fingerprint(): String {
        try {
            val pm = mApp.packageManager
            val packageName = mApp.packageName
            val flags = PackageManager.GET_SIGNATURES
            val packageInfo: PackageInfo = pm.getPackageInfo(packageName, flags)

            val signatures = packageInfo.signatures
            val cert = signatures[0].toByteArray()
            val input = ByteArrayInputStream(cert)
            var cf: CertificateFactory? = null
            try {
                cf = CertificateFactory.getInstance("X509")
            } catch (e: CertificateException) {
                e.printStackTrace()
            }

            var c: X509Certificate? = null
            try {
                c = cf!!.generateCertificate(input) as X509Certificate
            } catch (e: CertificateException) {
                e.printStackTrace()
            }

            var hexString: String? = null
            try {
                val md = MessageDigest.getInstance("SHA1")
                val publicKey = md.digest(c!!.encoded)
                hexString = byte2HexFormatted(publicKey)
            } catch (e1: NoSuchAlgorithmException) {
                e1.printStackTrace()
            } catch (e: CertificateEncodingException) {
                e.printStackTrace()
            }

            return hexString ?: ""
        } catch (ex: Exception) {
            return ""
        }
    }

    private fun byte2HexFormatted(arr: ByteArray): String {
        val str = StringBuilder(arr.size * 2)
        for (i in arr.indices) {
            var h = Integer.toHexString(arr[i].toInt())
            val l = h.length
            if (l == 1) h = "0" + h
            if (l > 2) h = h.substring(l - 2, l)
            str.append(h.toUpperCase())
            if (i < arr.size - 1) str.append(':')
        }
        return str.toString()
    }
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class DeviceId

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class Sha1FingerPrint

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class Default