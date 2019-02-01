package kr.ac.snu.hcil.omnitrack.ui.pages.research

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.experiment_invitation_list_element.view.*
import kotlinx.android.synthetic.main.fragment_recyclerview_and_fab.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.net.ExperimentInvitation
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.TopBottomHorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import org.jetbrains.anko.verticalMargin

/**
 * Created by younghokim on 2018. 1. 4..
 */
class InvitationListFragment : OTFragment() {

    private lateinit var viewModel: ResearchViewModel

    private val currentInvitationList = ArrayList<ExperimentInvitation>()

    private val adapter = InvitationListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_recyclerview_and_fab, container, false)

        rootView.ui_empty_list_message.setText(R.string.msg_empty_invitations)
        rootView.ui_recyclerview_with_fallback.emptyView = rootView.ui_empty_list_message

        rootView.ui_recyclerview_with_fallback.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        rootView.ui_recyclerview_with_fallback.adapter = this.adapter

        val shadowDecoration = TopBottomHorizontalImageDividerItemDecoration(context = requireActivity(), heightMultiplier = 0.8f)
        rootView.ui_recyclerview_with_fallback.addItemDecoration(shadowDecoration)
        (rootView.ui_recyclerview_with_fallback.layoutParams as CoordinatorLayout.LayoutParams).verticalMargin = -shadowDecoration.upperDividerHeight

        rootView.fab.hide()

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(ResearchViewModel::class.java)
        creationSubscriptions.add(
                viewModel.invitationListSubject.subscribe { invitations ->
                    val diffResult = DiffUtil.calculateDiff(ExperimentInvitation.DiffCallback(this.currentInvitationList, invitations), true)
                    currentInvitationList.clear()
                    currentInvitationList.addAll(invitations)
                    diffResult.dispatchUpdatesTo(adapter)
                }
        )
    }


    inner class InvitationViewHolder(parent: ViewGroup, @LayoutRes layoutId: Int) : RecyclerView.ViewHolder(parent.inflateContent(layoutId, false)) {
        private var invitation: ExperimentInvitation? = null

        init {
            itemView.btn_join.setOnClickListener {
                this.invitation?.let {
                    viewModel.insertInvitationCode(it.code)
                }
            }
        }

        fun bind(invitation: ExperimentInvitation) {
            this.invitation = invitation
            itemView.name.text = invitation.experiment.name
            itemView.description.text = invitation.code
            itemView.btn_join.visibility = View.VISIBLE
        }
    }

    inner class InvitationListAdapter : RecyclerView.Adapter<InvitationViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
            return InvitationViewHolder(parent, R.layout.experiment_invitation_list_element)
        }

        override fun getItemCount(): Int {
            return currentInvitationList.size
        }

        override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
            holder.bind(currentInvitationList[position])
        }

    }

}