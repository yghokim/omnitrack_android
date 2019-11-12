package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.github.salomonbrys.kotson.*
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.internal.Factory
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_new_item.*
import kr.ac.snu.hcil.android.common.net.WebServiceLoginActivity
import kr.ac.snu.hcil.android.common.view.DialogHelper
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTriggerReminderEntry
import kr.ac.snu.hcil.omnitrack.core.di.Backend
import kr.ac.snu.hcil.omnitrack.core.di.ForGeneric
import kr.ac.snu.hcil.omnitrack.services.OTReminderService
import javax.inject.Inject
import javax.inject.Provider

class NewItemActivity : AItemDetailActivity<NewItemCreationViewModel>(NewItemCreationViewModel::class.java) {
    companion object {

        const val INTENT_EXTRA_REMINDER_TIME = "reminderTime"

        fun makeNewItemPageIntent(trackerId: String, context: Context): Intent {
            val intent = Intent(context, NewItemActivity::class.java)
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            return intent
        }

        fun makeReminderOpenIntent(trackerId: String, reminderTime: Long, metadata: JsonObject, context: Context): Intent {
            val intent = Intent(context, NewItemActivity::class.java)
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            intent.putExtra(INTENT_EXTRA_REMINDER_TIME, reminderTime)
            intent.putExtra(OTApp.INTENT_EXTRA_METADATA, metadata.toString())
            return intent
        }

    }

    private lateinit var redirectedPageCanceledSnackbar: Snackbar


    @field:[Inject ForGeneric]
    protected lateinit var genericGson: Provider<Gson>

    @field:[Inject Backend]
    protected lateinit var realmProvider: Factory<Realm>


    override fun onInject(app: OTAndroidApp) {
        app.applicationComponent.inject(this)
    }

    override fun initViewModel(viewModel: NewItemCreationViewModel, savedInstanceState: Bundle?) {
        val trackerId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)

        val metadata: JsonObject? = if (savedInstanceState == null) {
            val payloadMetadataJson = intent.getStringExtra(OTApp.INTENT_EXTRA_METADATA)
            if (payloadMetadataJson != null) {
                val m = genericGson.get().fromJson(payloadMetadataJson, JsonObject::class.java)
                m.addProperty("accessedDirectlyFromReminder", true)
                m.addProperty("pairedToReminder", true)
                m
            } else {
                //if the tracker was manually opened, get metadata from current reminder.
                val realm = realmProvider.get()
                val entries = realm.where(OTTriggerReminderEntry::class.java)
                        .equalTo("dismissed", false)
                        .equalTo("trackerId", trackerId)
                        .findAll()
                if (entries.size > 0) {
                    val m = entries.last()?.serializedMetadata?.let { genericGson.get().fromJson(it, JsonObject::class.java) }
                            ?: JsonObject()
                    m.addProperty("accessedDirectlyFromReminder", false)
                    m.addProperty("pairedToReminder", true)
                    m
                } else jsonObject("pairedToReminder" to false)
            }
        } else null


        if (savedInstanceState == null) {
            metadata?.addProperty("screenAccessedAt", System.currentTimeMillis())
        }

        viewModel.init(trackerId, metadata, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        redirectedPageCanceledSnackbar = Snackbar.make(ui_root, resources.getText(R.string.msg_redirected_page_canceled), Snackbar.LENGTH_INDEFINITE)
        redirectedPageCanceledSnackbar.setAction(resources.getText(R.string.msg_reopen_redirected_page)) { view ->
            redirectedPageCanceledSnackbar.dismiss()
            tryOpenRedirectPage()
        }

        creationSubscriptions.add(
                viewModel.redirectedPageVisitStatusObservable.subscribe {
                    ui_button_next.text = resources.getString(
                            when (it) {
                                NewItemCreationViewModel.RedirectedPageStatus.NotVisited -> R.string.msg_visit_survey_website
                                NewItemCreationViewModel.RedirectedPageStatus.Canceled -> R.string.msg_save_item_anyway
                                else -> R.string.msg_save_current_input
                            }
                    )
                }
        )

    }

    override fun getTitle(trackerName: String): String {
        return String.format(resources.getString(R.string.title_activity_new_item), trackerName)
    }

    override fun preOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_REDIRECT_SURVEY) {
            val redirectionStatus = when (resultCode) {
                Activity.RESULT_OK -> {
                    NewItemCreationViewModel.RedirectedPageStatus.Completed
                }
                else -> NewItemCreationViewModel.RedirectedPageStatus.Canceled
            }

            if (resultCode == Activity.RESULT_OK) {
                viewModel.modifyMetadata { metadata ->
                    metadata.removeAll(
                            metadata.keys().filter { it.startsWith("returned::") }
                    )

                    if (data?.hasExtra(WebServiceLoginActivity.EXTRA_RETURNED_PARAMETERS) == true) {
                        val returnedParameters = data.getBundleExtra(WebServiceLoginActivity.EXTRA_RETURNED_PARAMETERS)
                        for (key in returnedParameters.keySet()) {
                            metadata.set(key, returnedParameters.getString(key))
                        }
                    }
                }

            }

            (viewModel as? NewItemCreationViewModel)?.setResultOfRedirectedPage(redirectionStatus)
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Completed Qualtrics survey.", Toast.LENGTH_LONG).show()
                redirectedPageCanceledSnackbar.dismiss()
                onToolbarRightButtonClicked()
            } else {
                redirectedPageCanceledSnackbar.show()
            }
            return true
        } else return false
    }

    override fun onToolbarLeftButtonClicked() {
        super.onToolbarLeftButtonClicked()

        val needToStoreBuilder = if (viewModel.isValid) {
            if (viewModel.isViewModelsDirty()) {
                true
            } else currentAttributeViewModelList.any { it.fieldDAO.getHelper(this@NewItemActivity).isAttributeValueVolatile(it.fieldDAO) }
        } else {
            false
        }

        if (needToStoreBuilder) {
            DialogHelper.makeYesNoDialogBuilder(this, BuildConfig.APP_NAME, resources.getString(R.string.msg_confirm_item_detail_close), R.string.msg_close, R.string.msg_cancel, {
                viewModel.clearHistory()
                finish()
            }, {

            }, false).show()
        } else {
            println("remove builder")
            viewModel.clearHistory()
            finish()
        }
    }


    private fun tryOpenRedirectPage() {
        if (viewModel.trackerDao.redirectUrl != null) {
            val url = Uri.parse(viewModel.trackerDao.redirectUrl).buildUpon()
                    .appendQueryParameter("userId", authManager.userId)
                    .appendQueryParameter("itemId", (viewModel as? NewItemCreationViewModel)?.generateNewItemId())
                    .run {
                        viewModel.modifyMetadata { metadata ->
                            metadata.forEach { key, value ->
                                try {
                                    this.appendQueryParameter(key, value.asString)
                                } catch (ex: Exception) {
                                    this.appendQueryParameter(key, value.toString())
                                }
                            }
                        }
                        this
                    }
                    .build().toString()

            startActivityForResult(WebServiceLoginActivity.makeIntent(url, "Qualtrics", "Complete A Survey", this), REQUEST_CODE_REDIRECT_SURVEY)
        }
    }

    override fun onTrackerInputIncomplete() {
        super.onTrackerInputIncomplete()
        if (viewModel.isRedirectionSet)
            ui_button_next.isEnabled = false
    }

    override fun onTrackerInputComplete() {
        super.onTrackerInputComplete()
        if (viewModel.isRedirectionSet)
            ui_button_next.isEnabled = true
    }

    override fun onToolbarRightButtonClicked() {
        if (viewModel.redirectedPageVisitStatusObservable.value != NewItemCreationViewModel.RedirectedPageStatus.NotVisited) {
            creationSubscriptions.add(
                    checkInputComplete().andThen(viewModel.applyEditingToDatabase()).subscribe({ itemId ->
                        viewModel.clearHistory()
                        startService(OTReminderService.makeUserLoggedIntent(this, viewModel.trackerDao._id!!, System.currentTimeMillis()))
                        itemSaved = true
                        eventLogger.get().logItemAddedEvent(itemId, ItemLoggingSource.Manual) { content ->
                            if (this.intent.hasExtra(INTENT_EXTRA_REMINDER_TIME)) {
                                content.add("pivotReminderTime", this.intent.getLongExtra(INTENT_EXTRA_REMINDER_TIME, 0).toJson())
                            }
                        }

                        setResult(RESULT_OK, Intent().putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, itemId))
                        finish()
                    }, { ex ->
                        if (ex is RequiredFieldsNotCompleteException) {

                        } else throw ex
                    })
            )
        } else {
            tryOpenRedirectPage()
        }
    }
}