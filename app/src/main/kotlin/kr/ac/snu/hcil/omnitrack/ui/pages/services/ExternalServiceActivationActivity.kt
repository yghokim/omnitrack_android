package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.markushi.ui.RevealColorView
import butterknife.bindView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.SerialDisposable
import kr.ac.snu.hcil.android.common.TextHelper
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.dependency.DependencyControlView
import kr.ac.snu.hcil.omnitrack.ui.components.common.dependency.DependencyControlViewModel
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalDividerItemDecoration
import mehdi.sakout.fancybuttons.FancyButton
import org.jetbrains.anko.dip
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 5. 25..
 */
class ExternalServiceActivationActivity : OTActivity(false, false) {

    companion object {

        const val INTENT_EXTRA_SERVICE_IDENTIFIER = "service_identifier"

        fun makeIntent(context: Context, service: OTExternalService): Intent {
            return Intent(context, ExternalServiceActivationActivity::class.java)
                    .apply {
                        this.putExtra(INTENT_EXTRA_SERVICE_IDENTIFIER, service.identifier)
                    }
        }
    }

    @Inject
    protected lateinit var externalServiceManager: OTExternalServiceManager

    private var serviceIdentifier: String = ""

    private lateinit var viewModel: ServiceActivationViewModel

    private val imageView: ImageView by bindView(R.id.ui_thumb)
    private val progressBar: ProgressBar by bindView(R.id.ui_progress_bar)

    private val nameView: TextView by bindView(R.id.ui_name)
    private val descriptionView: TextView by bindView(R.id.ui_description)

    private val activateButton: FancyButton by bindView(R.id.ui_button_activate)

    private val dependencyListView: RecyclerView by bindView(R.id.ui_dependency_list)

    private val buttonColorRevealView: RevealColorView by bindView(R.id.ui_reveal_color_view)

    private var dependencyList: List<DependencyControlViewModel>? = null
    private val dependencyAdapter = DependencyAdapter()


    override fun onInject(app: OTAndroidApp) {
        super.onInject(app)
        app.applicationComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_activation_wizard)

        dependencyListView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        dependencyListView.adapter = dependencyAdapter
        dependencyListView.addItemDecoration(HorizontalDividerItemDecoration(ContextCompat.getColor(this, R.color.separator_Light),
                dip(1),
                resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
        ))

        viewModel = ViewModelProviders.of(this).get(ServiceActivationViewModel::class.java)

        serviceIdentifier = if (savedInstanceState == null) {
            intent.getStringExtra(INTENT_EXTRA_SERVICE_IDENTIFIER)
        } else {
            savedInstanceState.getString(INTENT_EXTRA_SERVICE_IDENTIFIER)
        }

        viewModel.attachedService = externalServiceManager.findServiceByIdentifier(serviceIdentifier)

        if (savedInstanceState != null) {
            when (viewModel.currentState.value) {
                ServiceActivationViewModel.State.Satisfied ->
                    activateButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPointed))
                else ->
                    activateButton.setBackgroundColor(ContextCompat.getColor(this, R.color.textColorLight))
            }
        }

        creationSubscriptions.add(
                viewModel.serviceNameResId.subscribe { nameId ->
                    activateButton.text = TextHelper.formatWithResources(this, R.string.msg_format_service_activate_button, nameId)
                }
        )

        creationSubscriptions.add(
                viewModel.serviceThumbnailResId.subscribe {
                    id ->
                    imageView.setImageResource(id)
                }
        )

        creationSubscriptions.add(
                viewModel.serviceNameResId.subscribe {
                    id ->
                    nameView.setText(id)
                }
        )

        creationSubscriptions.add(
                viewModel.serviceDescResId.subscribe {
                    id ->
                    descriptionView.setText(id)
                }
        )


        creationSubscriptions.add(
                viewModel.serviceDependencyViewModels.subscribe {
                    models ->
                    dependencyList = models
                    dependencyAdapter.notifyDataSetChanged()
                }
        )

        creationSubscriptions.add(
                viewModel.currentState.observeOn(AndroidSchedulers.mainThread()).subscribe {
                    state ->
                    println("Service Activation ViewModel state changed to $state")
                    when (state) {
                        ServiceActivationViewModel.State.Satisfied -> {
                            progressBar.visibility = View.GONE
                            activateButton.isEnabled = true
                            buttonColorRevealView.reveal(buttonColorRevealView.width / 2, buttonColorRevealView.height / 2, ContextCompat.getColor(this, R.color.colorPointed), 0, 500, null)
                        }

                        ServiceActivationViewModel.State.Checking -> {
                            progressBar.visibility = View.VISIBLE
                            activateButton.isEnabled = false
                            buttonColorRevealView.reveal(buttonColorRevealView.width / 2, buttonColorRevealView.height / 2, ContextCompat.getColor(this, R.color.textColorLight), 0, 500, null)
                        }

                        ServiceActivationViewModel.State.IdleNotSatistified -> {

                            progressBar.visibility = View.GONE
                            activateButton.isEnabled = false
                            buttonColorRevealView.reveal(buttonColorRevealView.width / 2, buttonColorRevealView.height / 2, ContextCompat.getColor(this, R.color.textColorLight), 0, 500, null)
                        }
                    }
                }
        )

        activateButton.setOnClickListener {
            println("activate")
            creationSubscriptions.add(
                    viewModel.activateService().subscribe { activated: Boolean ->
                        if (activated) {
                            eventLogger.get().logServiceActivationChangeEvent(viewModel.attachedService?.identifier ?: "", true)
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        setResult(Activity.RESULT_CANCELED)
    }

    override fun onResume() {
        super.onResume()
        viewModel.startDependencyCheck(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(INTENT_EXTRA_SERVICE_IDENTIFIER, serviceIdentifier)
    }

    inner class DependencyAdapter : RecyclerView.Adapter<DependencyAdapter.DependencyAdapterViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DependencyAdapterViewHolder {
            return DependencyAdapterViewHolder(parent.inflateContent(R.layout.dependency_list_element, false))
        }

        override fun getItemCount(): Int {
            return dependencyList?.size ?: 0
        }

        override fun onBindViewHolder(holder: DependencyAdapterViewHolder, position: Int) {
            val viewModel = dependencyList?.get(position)
            holder.dependencyView.viewModel = viewModel

            holder.subscription.dispose()
        }


        inner class DependencyAdapterViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val subscription = SerialDisposable()
            val dependencyView: DependencyControlView by bindView(R.id.ui_dependency_view)
        }
    }
}