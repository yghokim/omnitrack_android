package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.content.Intent
import android.util.Patterns
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.LifecycleObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import kotlinx.android.synthetic.main.layout_home_sidebar.view.*
import kr.ac.snu.hcil.android.common.view.DialogHelper
import kr.ac.snu.hcil.android.common.view.container.adapter.RecyclerViewMenuAdapter
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.di.global.InformationUpload
import kr.ac.snu.hcil.omnitrack.core.di.global.ResearchSync
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.workers.OTResearchSynchronizationWorker
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.AboutActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.SettingsActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.diagnostics.SystemLogActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.export.PackageExportActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.services.ApiKeySettingsActivity
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by Young-Ho Kim on 2017-01-31.
 */
class SidebarWrapper(val view: View, val parentActivity: OTActivity) : PopupMenu.OnMenuItemClickListener, LifecycleObserver {

    @Inject
    lateinit var authManager: OTAuthManager

    @Inject
    lateinit var syncManager: Lazy<OTSyncManager>

    @Inject
    lateinit var eventLogger: Lazy<IEventLogger>


    @field:[Inject InformationUpload]
    lateinit var informationUploadRequestBuilderFactory: Factory<OneTimeWorkRequest.Builder>

    @field:[Inject ResearchSync]
    lateinit var researchSyncRequest: Provider<OneTimeWorkRequest>


    private val userWatchDisposable = SerialDisposable()

    private val profileMenuButton: AppCompatImageButton = view.findViewById(R.id.ui_button_profile_menu)

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


        val popupMenu = PopupMenu(parentActivity, profileMenuButton, Gravity.TOP or Gravity.END)
        popupMenu.inflate(R.menu.menu_sidebar_profile)
        popupMenu.setOnMenuItemClickListener(this)

        profileMenuButton.setOnClickListener {
            popupMenu.show()
        }

        menuList.layoutManager = LinearLayoutManager(parentActivity, RecyclerView.VERTICAL, false)
        menuList.adapter = SidebarMenuAdapter()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_unlink_with_this_device -> {
                DialogHelper.makeNegativePhrasedYesNoDialogBuilder(parentActivity, null, parentActivity.getString(R.string.msg_profile_unlink_account_confirm), R.string.msg_logout, onYes = {
                    eventLogger.get().logEvent(IEventLogger.NAME_AUTH, IEventLogger.SUB_SIGNED_OUT)
                    this.subscriptions.add(
                            authManager.signOut().subscribe()
                    )
                }).show()
                return true
            }
            else -> {
                return false
            }
        }
    }

    fun onCreate() {
        refreshUsername()

        userWatchDisposable.set(
                authManager.authTokenChanged.subscribe { token ->
                    refreshUsername()
                }
        )
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
            if (Patterns.EMAIL_ADDRESS.matcher(userName).matches()) {
                //email
                val emailSplit = userName.split("@")
                view.ui_username_main.text = emailSplit.first()
                view.ui_username_sub.text = emailSplit.last()
            } else {
                //plain string
                view.ui_username_main.text = userName
                view.ui_username_sub.visibility = View.GONE
            }
        } else {
            view.ui_username_sub.text = "User signed out."
        }
    }

    fun onDestroy() {
        userWatchDisposable.set(null)
        subscriptions.clear()
    }

    fun onShowSidebar() {
        println("sidebar showed")
    }

    fun onHideSidebar() {
        println("sidebar hidden")
    }

    inner class SidebarMenuAdapter : RecyclerViewMenuAdapter() {

        private val menus = arrayListOf(
                RecyclerViewMenuAdapter.MenuItem(R.drawable.settings_dark, parentActivity.getString(R.string.msg_settings), null, {
                    val intent = Intent(parentActivity, SettingsActivity::class.java)
                    parentActivity.startActivity(intent)
                }, true),

                RecyclerViewMenuAdapter.MenuItem(R.drawable.help_dark, parentActivity.getString(R.string.msg_about), null, {
                    val intent = Intent(parentActivity, AboutActivity::class.java)
                    parentActivity.startActivity(intent)
                }, true),

                RecyclerViewMenuAdapter.MenuItem(R.drawable.icon_refresh, "Refresh", null, {
                    //OTApp.instance.syncManager.performSynchronizationOf(ESyncDataType.ITEM)
                    syncManager.get().queueFullSync(ignoreFlags = false)
                    syncManager.get().reserveSyncServiceNow()
                    WorkManager.getInstance().enqueueUniqueWork(OTResearchSynchronizationWorker.TAG, ExistingWorkPolicy.REPLACE, researchSyncRequest.get())
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
                        RecyclerViewMenuAdapter.MenuItem(R.drawable.icon_package_dark, parentActivity.getString(R.string.msg_export_tracking_plan), null, {
                            val intent = Intent(parentActivity, PackageExportActivity::class.java)
                            parentActivity.startActivity(intent)
                        }, true)
                )
            }

            if (BuildConfig.ENABLE_DYNAMIC_API_KEY_MODIFICATION == true) {
                add(
                        RecyclerViewMenuAdapter.MenuItem(R.drawable.icon_key, "API Keys", null, {
                            val intent = Intent(parentActivity, ApiKeySettingsActivity::class.java)
                            parentActivity.startActivity(intent)
                        }, true)
                )
            }


            if (BuildConfig.BUILD_TYPE.toLowerCase() == "debug") {
                //get access to the console log screen on debug mode
                add(
                        RecyclerViewMenuAdapter.MenuItem(R.drawable.icon_clipnote, "Debug Logs", null, {
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