package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import rx.internal.util.SubscriptionList

/**
 * Created by younghokim on 2017. 2. 21..
 */
class TriggerDetailActivity : MultiButtonActionBarActivity(R.layout.activity_trigger_detail) {

    companion object {
        const val INTENT_EXTRA_TRIGGER_TYPE = "trigger_type"
        const val INTENT_EXTRA_TRIGGER_ACTION = "trigger_action"

        fun makeNewTriggerIntent(context: Context, triggerType: Int, triggerAction: Int): Intent {
            val intent = Intent(context, TriggerDetailActivity::class.java)
                    .putExtra(INTENT_EXTRA_TRIGGER_TYPE, triggerType)
                    .putExtra(INTENT_EXTRA_TRIGGER_ACTION, triggerAction)
            return intent
        }

        fun makeEditTriggerIntent(context: Context, trigger: OTTrigger): Intent {
            val intent = Intent(context, TriggerDetailActivity::class.java)
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER, trigger.objectId)
            return intent
        }
    }

    private var triggerId: String? = null
    private var attachedTrigger: OTTrigger? = null
    private val currentTriggerPojo: FirebaseHelper.MutableTriggerPOJO = FirebaseHelper.MutableTriggerPOJO()

    private var user: OTUser? = null

    private val isEditMode: Boolean get() = triggerId != null

    private var startSubscriptions = SubscriptionList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.SaveCancel)
    }

    override fun onToolbarLeftButtonClicked() {

    }

    override fun onToolbarRightButtonClicked() {

    }

    override fun onStart() {
        super.onStart()

        startSubscriptions.add(
                signedInUserObservable.subscribe {
                    user ->
                    this.user = user
                    if (intent.hasExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER)) {
                        val triggerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER)
                        attachedTrigger = user.triggerManager.getTriggerWithId(triggerId)
                        this.triggerId = triggerId

                        attachedTrigger?.dumpDataToPojo(null)?.toMutable(currentTriggerPojo)
                    } else {

                    }

                    onUserLoaded(user)
                }
        )
    }

    fun onUserLoaded(user: OTUser) {

    }

    override fun onStop() {
        super.onStop()
        user = null
        attachedTrigger = null
        startSubscriptions.clear()
    }


}