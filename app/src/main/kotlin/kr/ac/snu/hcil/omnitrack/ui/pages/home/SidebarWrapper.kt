package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.arch.lifecycle.LifecycleObserver
import android.content.Intent
import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.Job
import dagger.Lazy
import dagger.internal.Factory
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import io.realm.Realm
import kotlinx.android.synthetic.main.layout_home_sidebar.view.*
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfiguration
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTUserDAO
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.di.configured.InformationUpload
import kr.ac.snu.hcil.omnitrack.core.di.configured.ResearchSync
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.services.OTInformationUploadService
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.viewholders.RecyclerViewMenuAdapter
import kr.ac.snu.hcil.omnitrack.ui.pages.AboutActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.SettingsActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.diagnostics.SystemLogActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.export.PackageExportActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.research.ResearchActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by Young-Ho Kim on 2017-01-31.
 */
class SidebarWrapper(val view: View, val parentActivity: OTActivity) : PopupMenu.OnMenuItemClickListener, LifecycleObserver {

    @field:[Inject Backend]
    lateinit var realmFactory: Factory<Realm>

    @Inject
    lateinit var authManager: OTAuthManager

    @Inject
    lateinit var syncManager: Lazy<OTSyncManager>

    @Inject
    lateinit var eventLogger: Lazy<IEventLogger>

    @Inject
    lateinit var jobDispatcher: FirebaseJobDispatcher

    @field:[Inject InformationUpload]
    lateinit var informationUploadJobProvider: Job.Builder

    @field:[Inject ResearchSync]
    lateinit var researchSyncJob: Provider<Job>

    @Inject
    lateinit var configuration: OTConfiguration

    private lateinit var backendRealm: Realm

    private val userWatchDisposable = SerialDisposable()

    private val photoView: CircleImageView = view.findViewById(R.id.ui_user_photo)
    private val profileMenuButton: AppCompatImageButton = view.findViewById(R.id.ui_button_profile_menu)

    private val menuList: RecyclerView = view.findViewById(R.id.ui_menu_list)

    private val subscriptions = CompositeDisposable()


    private val screenNameDialogBuilder: MaterialDialog.Builder by lazy {
        MaterialDialog.Builder(parentActivity)
                .title(R.string.msg_change_screen_name)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .inputRangeRes(1, 40, R.color.colorRed)
                .cancelable(true)
                .negativeText(R.string.msg_cancel)
    }

    init {

        (parentActivity.application as OTApp).applicationComponent.configurationController().currentConfiguredContext
                .configuredAppComponent.inject(this)

        /*
        val signOutButton = view.findViewById(R.id.ui_button_sign_out)
        signOutButton.setOnClickListener {
            if (AWSMobileClient.defaultMobileClient().identityManager.isUserSignedIn) {
                AWSMobileClient.defaultMobileClient().identityManager.signOut()
            } else {
                val intent = Intent(parentActivity, SignInActivity::class.java)
                parentActivity.startActivity(intent)
                parentActivity.finish()
            }
        }*/

        val popupMenu = PopupMenu(parentActivity, profileMenuButton, Gravity.TOP or Gravity.END)
        popupMenu.inflate(R.menu.menu_sidebar_profile)
        popupMenu.setOnMenuItemClickListener(this)

        profileMenuButton.setOnClickListener {
            popupMenu.show()
        }

        menuList.layoutManager = LinearLayoutManager(parentActivity, LinearLayoutManager.VERTICAL, false)
        menuList.adapter = SidebarMenuAdapter()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_unlink_with_this_device -> {
                DialogHelper.makeNegativePhrasedYesNoDialogBuilder(parentActivity, "OmniTrack", parentActivity.getString(R.string.msg_profile_unlink_account_confirm), R.string.msg_logout, onYes = {
                    authManager.signOut()
                    OTApp.instance.unlinkUser()

                    eventLogger.get().logEvent(IEventLogger.NAME_AUTH, IEventLogger.SUB_SIGNED_OUT)

                    parentActivity.goSignIn()
                }).show()
                return true
            }
            else -> {
                return false
            }
        }
    }

    fun onCreate() {
        backendRealm = realmFactory.get()

        userWatchDisposable.set(
                parentActivity.signedInUserObservable.toFlowable(BackpressureStrategy.LATEST).flatMap { userId ->
                    backendRealm = realmFactory.get()
                    return@flatMap backendRealm.where(OTUserDAO::class.java).equalTo("uid", userId).findFirstAsync().asFlowable<OTUserDAO>().filter { it.isValid && it.isLoaded }
                }.subscribe { user ->
                    view.ui_user_name.text = user.name
                    view.ui_user_email.text = user.email
                    Glide.with(parentActivity).load(user.photoServerPath).into(photoView)
                })


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
                                    jobDispatcher.mustSchedule(informationUploadJobProvider.setTag(
                                            OTInformationUploadService.makeTag(authManager.userId
                                                    ?: "", configuration.id, OTInformationUploadService.INFORMATION_USERNAME)
                                    ).build())
                                }
                            }
                        }
                    }
                }
            }.show()
        }
    }

    fun onDestroy() {
        userWatchDisposable.set(null)
        subscriptions.clear()

        if (this::backendRealm.isInitialized) {
            if (!backendRealm.isClosed) {
                backendRealm.close()
            }
        }
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
                    jobDispatcher.mustSchedule(researchSyncJob.get())
                }, true)
        ).apply {

            if (BuildConfig.DEFAULT_EXPERIMENT_ID.isNullOrBlank() == true) {
                add(
                        RecyclerViewMenuAdapter.MenuItem(R.drawable.icon_plask, "Research", null, {
                            val intent = Intent(parentActivity, ResearchActivity::class.java)
                            parentActivity.startActivity(intent)
                        }, true)
                )

                add(
                        RecyclerViewMenuAdapter.MenuItem(R.drawable.icon_package_dark, "Export Tracking Package", null, {
                            val intent = Intent(parentActivity, PackageExportActivity::class.java)
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