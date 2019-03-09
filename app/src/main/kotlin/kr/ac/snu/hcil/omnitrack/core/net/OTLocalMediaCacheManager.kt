package kr.ac.snu.hcil.omnitrack.core.net

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.Lazy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.android.common.file.FileHelper
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.LocalMediaCacheEntry
import kr.ac.snu.hcil.omnitrack.core.datatypes.OTServerFile
import java.io.File
import java.io.FileNotFoundException
import java.util.*

/**
 * Created by Young-Ho on 11/16/2017.
 */
class OTLocalMediaCacheManager(val context: Context, val authManager: Lazy<OTAuthManager>, val binaryStorageController: Lazy<OTBinaryStorageController>) {


    fun getDefaultItemCacheDir(trackerId: String, createIfNotExist: Boolean = true): File {
        val file = context.externalCacheDir.resolve("${authManager.get().userId
                ?: "anonymous"}/$trackerId")
        if (createIfNotExist && !file.exists()) {
            file.mkdirs()
        }
        return file
    }

    private fun findSynchronizedCacheEntries(trackerId: String, realm: Realm): List<LocalMediaCacheEntry> {
        return realm.where(LocalMediaCacheEntry::class.java)
                .isNotNull(BackendDbManager.FIELD_SYNCHRONIZED_AT)
                .and()
                .and()
                .not()
                .beginsWith("serverPath", "temporary")
                .findAll().filter {
            binaryStorageController.get().core.decodeTrackerIdFromServerPath(it.serverPath) != null
        }
    }

    fun calcTotalCacheFileSize(trackerId: String): Single<Long> {
        return Single.defer {
            binaryStorageController.get().realmProvider.get().use { realm ->
                val cacheEntries = findSynchronizedCacheEntries(trackerId, realm)
                        .map { FileHelper.getFileSizeOf(it.localUriCompat(), context) }

                return@defer Single.just(cacheEntries.sum())
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * @return A Single containing the number of files removed
     *
     */
    fun purgeSynchronizedCacheFiles(trackerId: String): Single<Int> {
        return Single.defer {
            binaryStorageController.get().realmProvider.get().use { realm ->
                var removeCount = 0
                val cacheEntries = findSynchronizedCacheEntries(trackerId, realm)
                cacheEntries.forEach { entry ->
                    if (File(entry.localUriCompat().path).delete()) {
                        removeCount++
                    }
                    realm.executeTransaction {
                        entry.deleteFromRealm()
                    }
                }

                return@defer Single.just(removeCount)
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    fun makeLocalCacheUri(parentDirectoryName: String, extension: String): Uri {
        return Uri.fromFile(File.createTempFile("cache", extension, getDefaultItemCacheDir(parentDirectoryName, true)))
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

    fun getCachedUri(serverFile: OTServerFile, localCacheUri: Uri = makeLocalCacheUri("cache", MimeTypeMap.getFileExtensionFromUrl(serverFile.originalFileName)), storeCache: Boolean = true): Single<Pair<Boolean, Uri>> {
        return Single.defer {
            val realm = binaryStorageController.get().realmProvider.get()
            val cachedEntry = realm.where(LocalMediaCacheEntry::class.java)
                    .equalTo("serverPath", serverFile.serverPath).findFirst()

            val cachedUri = if (cachedEntry != null
                    && cachedEntry.originalFileName == serverFile.originalFileName
                    && cachedEntry.originalFileByteSize == serverFile.fileSize
                    && cachedEntry.originalMimeType == serverFile.mimeType) {
                cachedEntry.localUriCompat()
            } else null

            realm.close()
            if (cachedUri != null) {
                return@defer Single.just(Pair(false, cachedUri))
            } else {
                return@defer binaryStorageController.get().downloadFileTo(serverFile.serverPath, localCacheUri)
                        .flatMap { uri ->
                            if (storeCache) {
                                insertOrUpdateNewLocalMedia(uri, serverFile, System.currentTimeMillis()).map { Pair(true, uri) }
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
    fun insertOrUpdateNewLocalMedia(localUri: Uri, serverFile: OTServerFile, synchronizedAt: Long? = null): Single<OTServerFile> {
        return Single.defer {
            if (File(localUri.path).exists()) {
                binaryStorageController.get().realmProvider.get().use { realm ->
                    realm.executeTransaction {
                        val cacheEntry = realm.where(LocalMediaCacheEntry::class.java)
                                .equalTo("serverPath", serverFile.serverPath)
                                .findFirst() ?: realm.createObject(LocalMediaCacheEntry::class.java, UUID.randomUUID().toString())

                        cacheEntry.serverPath = serverFile.serverPath
                        cacheEntry.localUri = localUri.path
                        cacheEntry.originalMimeType = serverFile.mimeType
                        cacheEntry.originalFileByteSize = serverFile.fileSize
                        cacheEntry.originalFileName = serverFile.originalFileName
                        cacheEntry.synchronizedAt = synchronizedAt
                    }
                    return@defer Single.just(serverFile)
                }
            } else {
                return@defer Single.error<OTServerFile>(FileNotFoundException("File does not exists"))
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    fun replaceTemporalServerPath(oldServerPath: String, trackerId: String, itemId: String, attributeLocalId: String): String? {

        binaryStorageController.get().realmProvider.get().use { realm ->
            val entry = realm.where(LocalMediaCacheEntry::class.java).equalTo("serverPath", oldServerPath).findFirst()
            if (entry != null) {
                val newServerPath = binaryStorageController.get().makeServerPath(authManager.get().userId!!, trackerId, itemId, attributeLocalId, "0")
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
                    .isNull(BackendDbManager.FIELD_SYNCHRONIZED_AT)
                    .not()
                    .beginGroup()
                    .beginsWith("serverPath", "temporary")
                    .endGroup()
                    .findAll()
                    .forEach { entry ->
                        binaryStorageController.get().registerNewUploadTask(entry.localUri, entry.toServerFile())
                    }
        }
    }

}