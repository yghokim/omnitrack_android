package kr.ac.snu.hcil.omnitrack.ui.pages.items

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.github.salomonbrys.kotson.set
import kotlinx.android.synthetic.main.activity_new_item.*
import kr.ac.snu.hcil.android.common.view.setPaddingBottom
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import org.jetbrains.anko.notificationManager

class ItemEditActivity : AItemDetailActivity<ItemEditingViewModel>(ItemEditingViewModel::class.java) {
    companion object {
        fun makeItemEditPageIntent(itemId: String, trackerId: String, context: Context): Intent {
            val intent = Intent(context, ItemEditActivity::class.java)
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, itemId)
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            return intent
        }
    }

    override fun initViewModel(viewModel: ItemEditingViewModel, savedInstanceState: Bundle?) {
        val trackerId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)
        val itemId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM)
        viewModel.init(trackerId, itemId, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        this.rightActionBarTextButton?.visibility = View.VISIBLE
        this.ui_attribute_list.setPaddingBottom(0)
        this.ui_button_next_container.visibility = View.GONE

        if (intent.hasExtra(OTApp.INTENT_EXTRA_NOTIFICATION_ID)) {
            val notificationID = intent.getIntExtra(OTApp.INTENT_EXTRA_NOTIFICATION_ID, -1)
            val tag = intent.getStringExtra(OTApp.INTENT_EXTRA_NOTIFICATON_TAG)
            if (tag != null) {
                notificationManager.cancel(tag, notificationID)
            } else notificationManager.cancel(notificationID)
        }

        creationSubscriptions.add(
                viewModel.hasItemRemovedOutside.subscribe {
                    invalidOutsideDialogBuilder.content(R.string.msg_format_removed_outside_return_home, getString(R.string.msg_text_item))
                    invalidOutsideDialogBuilder.show()
                }
        )
    }

    override fun getTitle(trackerName: String): String {
        return String.format(getString(R.string.title_activity_edit_item), trackerName)
    }

    override fun onToolbarLeftButtonClicked() {
        super.onToolbarLeftButtonClicked()
        finish()
    }

    override fun onToolbarRightButtonClicked() {
        creationSubscriptions.add(
                checkInputComplete().andThen(viewModel.applyEditingToDatabase()).subscribe({ itemId ->
                    eventLogger.get().logItemEditEvent(itemId) { content ->
                        content[IEventLogger.CONTENT_IS_INDIVIDUAL] = false
                    }
                    setResult(RESULT_OK, Intent().putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, itemId))
                    finish()
                }, { ex ->
                    if (ex is RequiredFieldsNotCompleteException) {

                    } else throw ex
                })
        )
    }
}