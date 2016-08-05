package kr.ac.snu.hcil.omnitrack.activities.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService

/**
 * Created by Young-Ho on 7/29/2016.
 */
class ServiceListFragment : Fragment() {

    private lateinit var listView: RecyclerView

    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home_services, container, false)

        listView = rootView.findViewById(R.id.ui_list) as RecyclerView

        listView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        adapter = Adapter()

        listView.adapter = adapter

        return rootView
    }


    inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {


        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getService(position))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.external_service_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return OTExternalService.availableServices.size
        }

        private fun getService(position: Int): OTExternalService {
            return OTExternalService.availableServices[position]
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            val nameView: TextView
            val descriptionView: TextView

            val activationButton: Button

            init {
                nameView = view.findViewById(R.id.name) as TextView
                descriptionView = view.findViewById(R.id.description) as TextView

                activationButton = view.findViewById(R.id.ui_button_activate) as Button

                activationButton.setOnClickListener {
                    if (getService(adapterPosition).isActivated()) {
                        //deactivate

                    } else {
                        //activate

                    }
                }

            }

            fun setActivationMode(activated: Boolean) {

            }

            fun bind(service: OTExternalService) {
                nameView.text = context.resources.getString(service.nameResourceId)
                descriptionView.text = context.resources.getString(service.descResourceId)

                setActivationMode(service.isActivated())
            }

        }
    }
}