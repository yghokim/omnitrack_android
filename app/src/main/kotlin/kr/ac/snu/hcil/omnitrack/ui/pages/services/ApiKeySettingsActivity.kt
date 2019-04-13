package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_multibutton_single_recyclerview.*
import kr.ac.snu.hcil.android.common.view.setPaddingLeft
import kr.ac.snu.hcil.android.common.view.setPaddingRight
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.views.properties.ModalTextPropertyView
import javax.inject.Inject

class ApiKeySettingsActivity : MultiButtonActionBarActivity(R.layout.activity_multibutton_single_recyclerview) {


    @Inject
    protected lateinit var externalServiceManager: OTExternalServiceManager

    private lateinit var adapter: ApiKeyAdapter

    override fun onInject(app: OTAndroidApp) {
        app.applicationComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.Back)

        adapter = ApiKeyAdapter(externalServiceManager.installedServices.map { it.requiredApiKeyNames }.toTypedArray().flatten().toTypedArray())

        ui_recyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        ui_recyclerview.adapter = adapter
    }


    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {
    }

    inner class ApiKeyAdapter(val supportedKeys: Array<String>) : RecyclerView.Adapter<ApiKeyAdapter.ApiKeyEditorViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApiKeyEditorViewHolder {
            return ApiKeyEditorViewHolder(this@ApiKeySettingsActivity)
        }

        override fun getItemCount(): Int {
            return supportedKeys.size
        }

        override fun onBindViewHolder(holder: ApiKeyEditorViewHolder, position: Int) {
            holder.bind(supportedKeys[position])
        }

        inner class ApiKeyEditorViewHolder(context: Context) : RecyclerView.ViewHolder(ModalTextPropertyView(context, null)) {
            val propertyView: ModalTextPropertyView = itemView as ModalTextPropertyView

            private var key: String = ""

            init {
                val paddingHorizontal = context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
                itemView.setPaddingLeft(paddingHorizontal)
                itemView.setPaddingRight(paddingHorizontal)
                itemView.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)

                propertyView.valueChanged += { sender, newValue ->
                    if (newValue.isNotBlank()) {
                        externalServiceManager.registerApiKey(key, newValue)
                    } else {
                        externalServiceManager.removeApiKeyFromLocal(key)
                    }
                }
            }

            fun bind(key: String) {
                this.key = key
                propertyView.title = key
                propertyView.dialogTitle = "Set $key"
                val buildConfigKeyValue = externalServiceManager.getApiKeyInBuildConfig(key)
                propertyView.hint = buildConfigKeyValue
                propertyView.value = externalServiceManager.getApiKeyInLocal(key) ?: ""

            }
        }
    }

}