package kr.ac.snu.hcil.omnitrack.core.net

import android.content.Context
import android.net.Uri
import dagger.Lazy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.LocalMediaCacheEntry
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import java.io.File
import java.io.FileNotFoundException
import java.util.*

/**
 * Created by Young-Ho on 11/16/2017.
 */
class OTLocalMediaCacheManager(val context: Context, val authManager: Lazy<OTAuthManager>, val binaryStorageController: Lazy<OTBinaryStorageController>) {


    fun getItemCacheDir(trackerId: String, createIfNotExist: Boolean = true): File {
        val file = context.externalCacheDir.resolve("${authManager.get().userId ?: "anonymous"}/${trackerId}")
        if (createIfNotExist && !file.exists()) {
            file.mkdirs()
        }
        return file
    }

    fun getTotalCacheFileSize(trackerId: String): Long {
        val cacheDirectory = getItemCacheDir(trackerId, false)
        try {
            if (cacheDirectory.isDirectory && cacheDirectory.exists()) {

                fun getSizeRecur(dir: File): Long {
                    var size = 0L

                    if (dir.isDirectory) {
                        for (file in dir.listFiles()) {
                            if (file.isFile) {
                                size += file.length()
                            } else {
                                size += getSizeRecur(file)
                            }
                        }
                    } else if (dir.isFile) {
                        size += dir.length()
                    }

                    return size
                }

                return getSizeRecur(cacheDirectory)
            } else {
                return 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    fun makeLocalCacheUri(parentDirectoryName: String, extension: String): Uri {
        return Uri.fromFile(File.createTempFile("cache", extension, getItemCacheDir(parentDirectoryName, true)))
    }

    fun getCachedUriImmediately(serverPath: String): Uri? {
        binaryStorageController.get().realmProvider.get().use { realm ->
            val cachedEntry = realm.where(LocalMediaCacheEntry::class.java)
                    .equalTo("serverPath", serverPath).findFirst()
            if (cachedEntry != null) {
                return cachedEntry.localUriCompat()
            } else return null
        }
    }

    fun getCachedUri(serverPath: String, localCacheUri: Uri = makeLocalCacheUri("cache", ".cache"), storeCache: Boolean = true): Single<Pair<Boolean, Uri>> {
        return Single.defer {
            val realm = binaryStorageController.get().realmProvider.get()
            val cachedEntry = realm.where(LocalMediaCacheEntry::class.java)
                    .equalTo("serverPath", serverPath).findFirst()
            val cachedUri = cachedEntry?.localUriCompat()
            realm.close()
            if (cachedUri != null) {
                return@defer Single.just(Pair(false, cachedUri))
            } else {
                return@defer binaryStorageController.get().downloadFileTo(serverPath, localCacheUri)
                        .flatMap { uri ->
                            if (storeCache) {
                                insertOrUpdateNewLocalMedia(uri, serverPath, System.currentTimeMillis()).map { Pair(true, uri) }
                            } else Single.just(Pair(true, uri))
                        }
            }
        }.subscribeOn(Schedulers.io())
    }

    fun generateRandomServerPath(localUri: Uri): String {
        return "temporary://${UUID.randomUUID()}_${System.currentTimeMillis()}/${localUri.lastPathSegment}"
    }

    fun isServerPathTemporal(serverPath: String): Boolean {
        return serverPath.startsWith("temporary")
    }

    /**
     * @return serverPath
     */
    fun insertOrUpdateNewLocalMedia(localUri: Uri, serverPath: String, synchronizedAt: Long? = null): Single<String> {
        return Single.defer {
            val mimeType = FileHelper.getMimeTypeOf(localUri, context)
            if (File(localUri.path).exists()) {
                binaryStorageController.get().realmProvider.get().use { realm ->
                    realm.executeTransaction {
                        val cacheEntry = realm.where(LocalMediaCacheEntry::class.java)
                                .equalTo("serverPath", serverPath)
                                .findFirst() ?: realm.createObject(LocalMediaCacheEntry::class.java, UUID.randomUUID().toString())

                        cacheEntry.serverPath = serverPath
                        cacheEntry.localUri = localUri.path
                        cacheEntry.mimeType = mimeType ?: "unknown"
                        cacheEntry.synchronizedAt = synchronizedAt
                    }
                    return@defer Single.just(serverPath)
                }
            } else {
                return@defer Single.error<String>(FileNotFoundException("File does not exists"))
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    fun replaceTemporalServerPath(oldServerPath: String, trackerId: String, itemId: String, attributeLocalId: String): String? {

        binaryStorageController.get().realmProvider.get().use { realm ->
            val entry = realm.where(LocalMediaCacheEntry::class.java).equalTo("serverPath", oldServerPath).findFirst()
            if (entry != null) {
                val newServerPath = binaryStorageController.get().makeServerPath(authManager.get().userId!!, trackerId, itemId, attributeLocalId, entry.localUriCompat().lastPathSegment)
                realm.executeTransaction {
                    realm.where(LocalMediaCacheEntry::class.java).equalTo("serverPath", newServerPath).findAll().deleteAllFromRealm()
                    entry.serverPath = newServerPath
                }
                return newServerPath
            } else return null
        }
    }

    fun registerSynchronization() {
        binaryStorageController.get().realmProvider.get().use { realm ->
            realm.where(LocalMediaCacheEntry::class.java)
                    .isNull(RealmDatabaseManager.FIELD_SYNCHRONIZED_AT)
                    .not()
                    .beginGroup()
                    .beginsWith("serverPath", "temporary")
                    .endGroup()
                    .findAll()
                    .forEach { entry ->
                        binaryStorageController.get().registerNewUploadTask(entry.localUri, entry.serverPath)
                    }
        }
    }

}