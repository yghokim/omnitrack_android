package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.content.Intent
import android.view.View
import androidx.lifecycle.LifecycleObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import kotlinx.android.synthetic.main.layout_home_sidebar.view.*
import kr.ac.snu.hcil.android.common.view.container.adapter.RecyclerViewMenuAdapter
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.net.OTBinaryStorageController
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.AboutActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.auth.MyAccountActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.SettingsActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.diagnostics.SystemLogActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.export.PackageExportActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.services.ApiKeySettingsActivity
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2017-01-31.
 */
class SidebarWrapper(val view: View, val parentActivity: OTActivity) : LifecycleObserver {

    @Inject
    lateinit var authManager: OTAuthManager

    @Inject
    lateinit var syncManager: Lazy<OTSyncManager>

    @Inject
    lateinit var binaryUploadManager: Lazy<OTBinaryStorageController>

    @Inject
    lateinit var eventLogger: Lazy<IEventLogger>

    private val userWatchDisposable = SerialDisposable()

    private val menuList: RecyclerView = view.findViewById(R.id.ui_menu_list)

    private val subscriptions = CompositeDisposable()

/*
    private val screenNameDialogBuilder: MaterialDialog.Builder by lazy {
        MaterialDialog.Builder(parentActivity)
                .title(R.string.msg_change_screen_name)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .inputRangeRes(1, 40, R.color.colorRed)
                .cancelable(true)
                .negativeText(R.string.msg_cancel)
    }*/

    init {

        (parentActivity.application as OTAndroidApp).applicationComponent.inject(this)

        menuList.layoutManager = LinearLayoutManager(parentActivity, RecyclerView.VERTICAL, false)
        menuList.adapter = SidebarMenuAdapter()
    }

    fun onCreate() {
        refreshUsername()

        /*
parentActivity.signedInUserObservable.toFlowable(BackpressureStrategy.LATEST).flatMap { userId ->
    backendRealm = realmFactory.get()
    return@flatMap backendRealm.where(OTUserDAO::class.java).equalTo("uid", userId).findFirstAsync().asFlowable<OTUserDAO>().filter { it.isValid && it.isLoaded }
}.subscribe { user ->
    view.ui_user_name.text = user.name
    view.ui_user_email.text = user.email
    Picasso.get().load(user.photoServerPath).fit().into(photoView)
})*/

/*
        view.ui_button_edit_screen_name.setOnClickListener {
            screenNameDialogBuilder.input(null, view.ui_user_name.text, false) { dialog, input ->
                val newScreenName = input.trim().toString()
                if (newScreenName.isNotBlank()) {
                    if (authManager.userId != null) {
                        realmFactory.get().use { realm ->
                            val user = realm.where(OTUserDAO::class.java).equalTo("uid", authManager.userId
                                    ?: "").findFirst()
                            if (user != null) {
                                if (user.name != newScreenName) {
                                    realm.executeTransaction {
                                        user.name = newScreenName
                                        user.nameUpdatedAt = System.currentTimeMillis()
                                        user.nameSynchronizedAt = null
                                    }

                                    WorkManager.getInstance().enqueueUniqueWork(OTInformationUploadWorker.INFORMATION_USERNAME, ExistingWorkPolicy.APPEND,
                                            informationUploadRequestBuilderFactory.get()
                                                    .addTag(OTInformationUploadWorker.INFORMATION_USERNAME)
                                                    .setInputData(Data.Builder().putString(OTInformationUploadWorker.KEY_TYPE, OTInformationUploadWorker.INFORMATION_USERNAME)
                                                            .build()
                                                    ).build()
                                    )
                                }
                            }
                        }
                    }
                }
            }.show()
        }*/
    }

    private fun refreshUsername() {
        val userName = authManager.userName
        if (userName != null) {
            view.ui_username.text = userName

        } else {
            view.ui_username.text = "User signed out."
        }

        val email = authManager.email
        if (email != null) {
            view.ui_email.text = email
        } else {
            view.ui_email.text = "E-mail was not set."
        }
    }

    fun onDestroy() {
        subscriptions.clear()
    }

    fun onShowSidebar() {
        println("sidebar showed")
        userWatchDisposable.set(
                authManager.authTokenChanged.subscribe { token ->
                    refreshUsername()
                }
        )
    }

    fun onHideSidebar() {
        println("sidebar hidden")
        userWatchDisposable.set(null)
    }

    inner class SidebarMenuAdapter : RecyclerViewMenuAdapter() {

        private val menus = arrayListOf(

                MenuItem(R.drawable.ic_person_black_24dp, parentActivity.getString(R.string.activity_title_my_account), null, {
                    val intent = Intent(parentActivity, MyAccountActivity::class.java)
                    parentActivity.startActivity(intent)
                }, true),

                MenuItem(R.drawable.settings_dark, parentActivity.getString(R.string.msg_settings), null, {
                    val intent = Intent(parentActivity, SettingsActivity::class.java)
                    parentActivity.startActivity(intent)
                }, true),

                MenuItem(R.drawable.help_dark, parentActivity.getString(R.string.msg_about), null, {
                    val intent = Intent(parentActivity, AboutActivity::class.java)
                    parentActivity.startActivity(intent)
                }, true),

                MenuItem(R.drawable.icon_refresh, "Refresh", null, {
                    syncManager.get().queueFullSync(ignoreFlags = false)
                    syncManager.get().refreshWorkers()
                    binaryUploadManager.get().refreshWorkers()
                }, true)
        ).apply {

            if (BuildConfig.DEFAULT_EXPERIMENT_ID.isNullOrBlank()) {
                /*
                add(
                        RecyclerViewMenuAdapter.MenuItem(R.drawable.icon_plask, "Research", null, {
                            val intent = Intent(parentActivity, ResearchActivity::class.java)
                            parentActivity.startActivity(intent)
                        }, true)
                )*/

                add(
                        MenuItem(R.drawable.icon_package_dark, parentActivity.getString(R.string.msg_export_tracking_plan), null, {
                            val intent = Intent(parentActivity, PackageExportActivity::class.java)
                            parentActivity.startActivity(intent)
                        }, true)
                )
            }

            if (BuildConfig.ENABLE_DYNAMIC_API_KEY_MODIFICATION == true) {
                add(
                        MenuItem(R.drawable.icon_key, "API Keys", null, {
                            val intent = Intent(parentActivity, ApiKeySettingsActivity::class.java)
                            parentActivity.startActivity(intent)
                        }, true)
                )
            }


            if (BuildConfig.BUILD_TYPE.toLowerCase() == "debug") {
                //get access to the console log screen on debug mode
                add(
                        MenuItem(R.drawable.icon_clipnote, "Debug Logs", null, {
                            val intent = Intent(parentActivity, SystemLogActivity::class.java)
                            parentActivity.startActivity(intent)
                        }, true))
            }
        }

        init {
        }

        override fun getLayout(viewType: Int): Int {
            return R.layout.simple_menu_element_with_icon_title_description_small
        }

        override fun getMenuItemAt(index: Int): MenuItem {
            return menus[index]
        }

        override fun getItemCount(): Int {
            return menus.size
        }


    }
}