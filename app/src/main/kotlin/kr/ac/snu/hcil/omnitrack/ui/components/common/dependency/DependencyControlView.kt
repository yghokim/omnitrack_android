package kr.ac.snu.hcil.omnitrack.ui.components.common.dependency

import android.content.Context
import android.graphics.PorterDuff
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import mehdi.sakout.fancybuttons.FancyButton
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

/**
 * Created by younghokim on 2017. 5. 24..
 */
class DependencyControlView : RelativeLayout {

    private val subscriptions = CompositeSubscription()

    var viewModel: DependencyControlViewModel? = null
        set(value) {
            if (field != value) {
                val old = field
                if (old != null) {
                    old.dispose()
                    subscriptions.clear()
                }
                unbindViewModel()

                field = value

                if (value != null) {
                    bindViewModel()
                }
            }
        }

    private var viewModelBound = false

    private val descriptionView: TextView
    private val resolveButton: FancyButton
    private val busyIndicator: View
    private val checkedView: View
    private val resolveButtonContainer: ViewGroup
    private val resolveTypeWappen: TextView

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        inflateContent(R.layout.layout_dependency_control_view, true)

        descriptionView = findViewById(R.id.ui_description) as TextView
        resolveButton = findViewById(R.id.ui_button) as FancyButton
        busyIndicator = findViewById(R.id.ui_loading_indicator)
        checkedView = findViewById(R.id.ui_checked)
        resolveButtonContainer = findViewById(R.id.ui_resolve_button_container) as ViewGroup
        resolveTypeWappen = findViewById(R.id.ui_resolve_type_wappen) as TextView

        resolveButton.setOnClickListener {
            val activity = getActivity()
            if (activity != null)
                viewModel?.resolveDependency(activity)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bindViewModel()
    }

    private fun bindViewModel() {
        if (!viewModelBound) {
            viewModel?.let {
                subscriptions.add(
                        it.onStatusChanged.observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    state ->
                                    when (state) {
                                        DependencyControlViewModel.State.FAILED_FATAL, DependencyControlViewModel.State.FAILED_NON_FATAL -> {
                                            checkedView.visibility = View.GONE
                                            busyIndicator.visibility = View.GONE
                                            resolveButtonContainer.visibility = View.VISIBLE
                                        }
                                        DependencyControlViewModel.State.SATISFIED -> {

                                            checkedView.visibility = View.VISIBLE
                                            busyIndicator.visibility = View.GONE
                                            resolveButtonContainer.visibility = View.GONE
                                        }

                                        DependencyControlViewModel.State.CHECKING, DependencyControlViewModel.State.RESOLVING -> {
                                            descriptionView.setText(R.string.msg_checking)
                                            busyIndicator.visibility = View.VISIBLE
                                            resolveButtonContainer.visibility = View.GONE
                                        }
                                    }

                                    if(state == DependencyControlViewModel.State.FAILED_FATAL)
                                    {
                                        resolveTypeWappen.setText(R.string.msg_mandatory)
                                        resolveTypeWappen.background.setColorFilter(ContextCompat.getColor(context, R.color.colorRed_Light), PorterDuff.Mode.SRC_ATOP)
                                        //.setTextColor(ContextCompat.getColor(context, R.color.colorRed_Light))
                                    }
                                    else if(state == DependencyControlViewModel.State.FAILED_NON_FATAL)
                                    {
                                        resolveTypeWappen.setText(R.string.msg_optional)
                                        resolveTypeWappen.background.setColorFilter(ContextCompat.getColor(context, R.color.colorNoticeable), PorterDuff.Mode.SRC_ATOP)
                                        //descriptionView.setTextColor(ContextCompat.getColor(context, R.color.colorNoticeable))
                                    }
                                }
                )

                subscriptions.add(
                        it.onDependencyCheckResult.observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    (_, message, resolveText) ->
                                    resolveButton.setText(resolveText)
                                    descriptionView.text = message
                                }
                )
            }

            viewModelBound = true
        }
    }

    private fun unbindViewModel() {
        subscriptions.clear()

        viewModelBound = false
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unbindViewModel()
    }

}