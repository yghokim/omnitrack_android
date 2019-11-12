package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.event

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.connection_source_list_element.view.*
import kr.ac.snu.hcil.android.common.view.container.decoration.HorizontalDividerItemDecoration
import kr.ac.snu.hcil.android.common.view.getActivity
import kr.ac.snu.hcil.android.common.view.wizard.AWizardPage
import kr.ac.snu.hcil.android.common.view.wizard.AWizardViewPagerAdapter
import kr.ac.snu.hcil.android.common.view.wizard.WizardView
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.event.OTEventFactory
import kr.ac.snu.hcil.omnitrack.core.system.OTEventFactoryManager
import org.jetbrains.anko.activityManager
import org.jetbrains.anko.dip
import javax.inject.Inject

class EventTriggerWizardView : WizardView{


    @Inject
    lateinit var eventFactoryManager: OTEventFactoryManager

    var currentFactory: OTEventFactory? = null
        private set

    private lateinit var pendingEvent: OTEventFactory.OTAttachableEvent
    val generatedEvent: OTEventFactory.OTAttachableEvent
        get() = this.pendingEvent

    private val pageAdapter = PageAdapter(context)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onLeavePage(page: AWizardPage, position: Int) {

    }

    init{
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
        this.setAdapter(pageAdapter)
    }


    fun onSelectedFactory(factory: OTEventFactory): Boolean{
        if(factory!!.isPermissionGranted){
            return onPermissionGranted()
        }else{
            pageAdapter.pages.add(EventTriggerPermissionCheckPage())
            pageAdapter.notifyDataSetChanged()
            return true
        }
    }

    fun onPermissionGranted(): Boolean{
        //TODO check custom configuration page.
        complete()
        return false
    }

    inner class PageAdapter(context: Context) : AWizardViewPagerAdapter(context) {

        val pages = ArrayList<AWizardPage>()

        init{
            pages.add(EventTriggerEventSelectionPage())
        }

        override fun getCount(): Int {
            return pages.size
        }

        override fun getPageAt(position: Int): AWizardPage {
            return pages[position]
        }
    }


    inner class EventTriggerEventSelectionPage: AWizardPage(this){

        override val canGoBack: Boolean = false

        override val canGoNext: Boolean = true

        override val getTitleResourceId: Int = R.string.msg_event_wizard_source_title

        override fun onLeave() {
        }

        override fun onEnter() {
        }

        override fun makeViewInstance(context: Context): View {
            val view = RecyclerView(context)
            view.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            view.addItemDecoration(HorizontalDividerItemDecoration(ContextCompat.getColor(context, R.color.separator_Light),
                    dip(1.5f),
                    resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)))


            view.adapter = object: RecyclerView.Adapter<EventViewHolder>(){

                val factories = eventFactoryManager.availableFactories

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
                    return EventViewHolder(parent)
                }

                override fun getItemCount(): Int {
                    return factories.size
                }

                override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
                    holder.bind(factories.get(position))
                }

            }


            return view
        }

        inner class EventViewHolder(parent: ViewGroup): RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.connection_source_list_element, parent, false)), OnClickListener{

            private var factory :OTEventFactory? = null

            init{
                itemView.setOnClickListener(this)
            }

            fun bind(factory: OTEventFactory){
                this.factory = factory
                itemView.title.setText(factory.nameResourceId)
                itemView.description.setText(factory.descResourceId)
                itemView.category.setText(factory.getCategoryName())
            }

            override fun onClick(view: View?) {
                if(view === itemView){
                    if(factory != null) {
                        this@EventTriggerWizardView.currentFactory = factory
                        if(onSelectedFactory(factory!!)) {
                            requestGoNextPage()
                        }
                    }
                }
            }

        }

    }

    inner class EventTriggerPermissionCheckPage: AWizardPage(this){

        override val canGoBack: Boolean = false

        override val canGoNext: Boolean = true

        override val getTitleResourceId: Int = R.string.msg_event_wizard_permission_title

        private val compositeSubscription = CompositeDisposable()

        init{
        }

        override fun onLeave() {
            compositeSubscription.clear()
        }

        override fun onEnter() {
        }

        override fun makeViewInstance(context: Context): View {

            val pm = context.packageManager
            val permissionInfos = currentFactory?.unGrantedPermissions?.mapNotNull{
                try {
                    return@mapNotNull pm.getPermissionInfo(it, PackageManager.GET_META_DATA)
                }catch(ex: PackageManager.NameNotFoundException){
                    return@mapNotNull null
                }
            }


            val view = LinearLayout(context)
            view.orientation = LinearLayout.VERTICAL
            val recyclerView = RecyclerView(context)
            recyclerView.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)

            recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

            recyclerView.adapter = object: RecyclerView.Adapter<PermissionViewHolder>(){
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
                    return PermissionViewHolder(parent)
                }

                override fun getItemCount(): Int {
                    return permissionInfos?.count()?:0
                }

                override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
                    val permission = permissionInfos?.get(position)
                    if(permission!=null){
                        holder.bind(permission)
                    }
                }

            }

            val button = MaterialButton(context)
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPointed))
            button.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            button.text = "Approve Permissions to Proceed"

            button.setOnClickListener {
                val rxPermissions = RxPermissions(getActivity()!!)
                compositeSubscription.add(
                rxPermissions.request(*currentFactory!!.requiredPermissions).subscribe {
                    approvedAll->
                    if(approvedAll){
                        if(onPermissionGranted()){
                            requestGoNextPage()
                        }
                    }else{

                    }
                })
            }

            view.addView(recyclerView)
            view.addView(button)

            return view
        }

    }


    class PermissionViewHolder(parent: ViewGroup): RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.connection_source_list_element, parent, false)){

        fun bind(permissionInfo: PermissionInfo){
            itemView.category.setText(permissionInfo.group)
            itemView.title.setText(permissionInfo.labelRes)
            itemView.description.setText(permissionInfo.descriptionRes)
        }
    }
}