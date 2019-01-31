package kr.ac.snu.hcil.omnitrack.core.system

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.graphics.ColorUtils
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.SerialDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.di.global.Default
import kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.items.NewItemActivity
import kr.ac.snu.hcil.omnitrack.utils.VectorIconHelper
import org.jetbrains.anko.notificationManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Young-Ho Kim on 9/4/2016
 */
@Configured
class OTShortcutPanelManager @Inject constructor(
        @Singleton val context: Context,
        val authManager: Lazy<OTAuthManager>,
        val dbManager: Lazy<BackendDbManager>,
        @Default val pref: SharedPreferences,
        @Backend val backendRealmProvider: Factory<Realm>
) {

    companion object {

        const val NOTIFICATION_ID = 200000

        const val MAX_NUM_SHORTCUTS = 5
    }

    private val shortcutRefreshSubscriberTags = HashSet<String>()
    private val shortcutRefreshSubscription = SerialDisposable()

    val isWatchingForShortcutRefresh: Boolean get() = shortcutRefreshSubscription.get() != null && !shortcutRefreshSubscription.isDisposed

    val showPanels: Boolean
        get() {
            return pref.getBoolean("pref_show_shortcut_panel", false)
        }

    private fun buildNewNotificationShortcutViews(trackerList: List<OTTrackerDAO>, context: Context, bigStyle: Boolean): RemoteViews {
        val trackers = trackerList.filter {
            !it.isIndependentInputLocked()
        }

        val rv = RemoteViews(context.packageName, if (bigStyle) R.layout.remoteview_shortcut_notification_big else R.layout.remoteview_shortcut_notification_normal)

        if (bigStyle) {
            //header exist.

            val stackBuilder = TaskStackBuilder.create(context)
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(HomeActivity::class.java)
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(Intent(context, HomeActivity::class.java))

            val morePendingIntent = stackBuilder.getPendingIntent(0,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            rv.setOnClickPendingIntent(R.id.ui_button_more, morePendingIntent)
        }

        rv.removeAllViews(R.id.container)

        val buttonSize = context.resources.getDimensionPixelSize(R.dimen.button_height_small)
        val buttonRadius = buttonSize * .5f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        for (i in 0 until MAX_NUM_SHORTCUTS) {
            val element = RemoteViews(context.packageName, if (bigStyle) R.layout.remoteview_shortcut_notification_element else R.layout.remoteview_shortcut_notification_element_normal)

            if (trackers.size - 1 < i) {
                element.setViewVisibility(R.id.ui_button_instant, View.INVISIBLE)
                element.setViewVisibility(R.id.ui_name, View.INVISIBLE)
            } else {
                element.setViewVisibility(R.id.ui_button_instant, View.VISIBLE)
                element.setViewVisibility(R.id.ui_name, View.VISIBLE)

                element.setTextViewText(R.id.ui_name, trackers[i].name)

                if (trackers[i].isInstantLoggingAvailable()) {

                    element.setViewVisibility(R.id.ui_button_instant, View.VISIBLE)

                    val buttonBitmap = Bitmap.createBitmap(buttonSize, buttonSize, Bitmap.Config.ARGB_8888)
                    val buttonCanvas = Canvas(buttonBitmap)
                    paint.color = ColorUtils.setAlphaComponent(trackers[i].color, 200)
                    buttonCanvas.drawCircle(buttonRadius, buttonRadius, buttonRadius, paint)
                    element.setImageViewBitmap(R.id.ui_background_image, buttonBitmap)
                    element.setImageViewBitmap(R.id.ui_button_instant, VectorIconHelper.getConvertedBitmap(context, R.drawable.instant_add))


                    val instantLoggingIntent = PendingIntent.getService(context, i, OTItemLoggingService.makeLoggingIntent(context, ItemLoggingSource.Shortcut, true, trackers[i].objectId!!), PendingIntent.FLAG_UPDATE_CURRENT)
                    element.setOnClickPendingIntent(R.id.ui_button_instant, instantLoggingIntent)
                } else {
                    element.setViewVisibility(R.id.ui_button_instant, View.GONE)
                }

                val openItemActivityIntent = PendingIntent.getActivity(context, i, NewItemActivity.makeNewItemPageIntent(trackers[i].objectId!!, context), PendingIntent.FLAG_UPDATE_CURRENT)

                element.setOnClickPendingIntent(R.id.group, openItemActivityIntent)
            }

            rv.addView(R.id.container, element)
        }

        return rv
    }

    @Synchronized
    fun registerShortcutRefreshSubscription(userId: String, tag: String): Boolean {
        shortcutRefreshSubscriberTags.add(tag)
        if (!isWatchingForShortcutRefresh) {
            val realm = backendRealmProvider.get()
            shortcutRefreshSubscription.set(
                    dbManager.get().makeShortcutPanelRefreshObservable(userId, realm)
                            .unsubscribeOn(AndroidSchedulers.mainThread())
                            .doAfterTerminate { realm.close() }
                            .subscribe({}, {}, {})
            )
            return true
        } else return false
    }

    @Synchronized
    fun unregisterShortcutRefreshSubscription(tag: String): Boolean {
        if (isWatchingForShortcutRefresh) {
            if (shortcutRefreshSubscriberTags.remove(tag)) {
                if (shortcutRefreshSubscriberTags.isEmpty()) {
                    println("shortcut watch tags are empty. unsubscribe the subscription.")
                    shortcutRefreshSubscription.dispose()
                    return true
                }
            }
        }
        return false
    }

    fun refreshNotificationShortcutViewsObservable(context: Context): Completable {
        return Completable.defer {
            if (showPanels) {
                val userId = authManager.get().userId
                if (userId != null) {
                    println("user is signed in. initialize shortcut panel.")
                    val realm = backendRealmProvider.get()
                    return@defer dbManager.get().makeBookmarkedTrackersObservable(userId, realm).filter { it.isLoaded && it.isValid }.map { it.toList() }.first(emptyList())
                            .doOnSuccess { trackers ->
                                refreshNotificationShortcutViews(trackers)
                                realm.close()
                            }.doOnError { realm.close() }.ignoreElement()
                } else {
                    println("user is NOT signed in. dispose shortcut panel.")
                }
            }

            disposeShortcutPanel()
            return@defer Completable.complete()
        }
    }

    fun refreshNotificationShortcutViews(trackers: List<OTTrackerDAO>) {

        if (showPanels) {
            if (trackers.isNotEmpty()) {

                val bigView = buildNewNotificationShortcutViews(trackers, context, true)
                val normalView = buildNewNotificationShortcutViews(trackers, context, false)

                val noti = NotificationCompat.Builder(context, OTNotificationManager.CHANNEL_ID_WIDGETS)
                        .setSmallIcon(R.drawable.icon_simple)
                        .setLargeIcon(VectorIconHelper.getConvertedBitmap(context, R.drawable.icon_simple))
                        .setContentTitle(BuildConfig.APP_NAME)
                        .setCustomBigContentView(bigView)
                        .setCustomContentView(normalView)
                        .setDefaults(0)
                        .setOnlyAlertOnce(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .build()

                context.notificationManager
                        .notify(NOTIFICATION_ID, noti)
            } else {
                //dismiss notification
                disposeShortcutPanel()
            }
        } else {
            disposeShortcutPanel()
        }
    }


    fun disposeShortcutPanel() {
        context.notificationManager.cancel(NOTIFICATION_ID)
    }

}