package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import at.markushi.ui.RevealColorView
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.dependency.DependencyControlView
import kr.ac.snu.hcil.omnitrack.ui.components.common.dependency.DependencyControlViewModel
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import mehdi.sakout.fancybuttons.FancyButton
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.SerialSubscription

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

    private var serviceIdentifier: String = ""

    private lateinit var viewModel: ServiceActivationViewModel

    private lateinit var imageView: ImageView
    private lateinit var statusView: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var nameView: TextView
    private lateinit var descriptionView: TextView

    private lateinit var activateButton: FancyButton

    private lateinit var dependencyListView: RecyclerView

    private lateinit var buttonColorRevealView: RevealColorView

    private var dependencyList: List<DependencyControlViewModel>? = null
    private val dependencyAdapter = DependencyAdapter()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_activation_wizard)

        imageView = findViewById(R.id.ui_thumb) as ImageView
        //statusView = findViewById(R.id.ui_total_status_text) as TextView
        dependencyListView = findViewById(R.id.ui_dependency_list) as RecyclerView
        progressBar = findViewById(R.id.ui_progress_bar) as ProgressBar
        activateButton = findViewById(R.id.ui_button_activate) as FancyButton

        nameView = findViewById(R.id.ui_name) as TextView
        descriptionView = findViewById(R.id.ui_description) as TextView

        buttonColorRevealView = findViewById(R.id.ui_reveal_color_view) as RevealColorView

        dependencyListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        dependencyListView.adapter = dependencyAdapter
        dependencyListView.addItemDecoration(HorizontalDividerItemDecoration(ContextCompat.getColor(this, R.color.separator_Light),
                (1 * resources.displayMetrics.density + .5f).toInt(),
                resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
        ))

        viewModel = ViewModelProviders.of(this).get(ServiceActivationViewModel::class.java)

        serviceIdentifier = if (savedInstanceState == null) {
            intent.getStringExtra(INTENT_EXTRA_SERVICE_IDENTIFIER)
        } else {
            savedInstanceState.getString(INTENT_EXTRA_SERVICE_IDENTIFIER)
        }

        viewModel.attachedService = OTExternalService.findServiceByIdentifier(serviceIdentifier)

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
                viewModel.currentState.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                    state ->
                    println("Service Activation ViewModel state changed to ${state}")
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
                    viewModel.activateService().subscribe {
                        activated ->
                        if (activated) {
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

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putString(INTENT_EXTRA_SERVICE_IDENTIFIER, serviceIdentifier)
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

            holder.subscription.unsubscribe()
        }


        inner class DependencyAdapterViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val subscription = SerialSubscription()
            val dependencyView: DependencyControlView by bindView(R.id.ui_dependency_view)
        }
    }
}