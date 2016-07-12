package kr.ac.snu.hcil.omnitrack.activities

import android.content.Context
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.ListAdapter
import android.widget.ListView

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTProject
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication

class ProjectsActivity : AppCompatActivity() {

    lateinit private var listView : ListView

    lateinit private var projectListAdapter : ProjectListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projects)
        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)


        val fab = findViewById(R.id.fab) as FloatingActionButton?
        fab!!.setOnClickListener { view ->
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show()
           (application as OmniTrackApplication).syncUserToDb()
        }

        //attach events
        val user = (application as OmniTrackApplication).currentUser
        user.projectAdded += onProjectChangedHandler
        user.projectRemoved += onProjectChangedHandler

        val content = this.findViewById(R.id.layout_projects) as ViewGroup


        listView = findViewById(R.id.ui_project_list_view) as ListView

        projectListAdapter = ProjectListAdapter(this, user)

        listView.adapter = projectListAdapter
    }

    override fun onDestroy() {
        super.onDestroy()

        val user = (application as OmniTrackApplication).currentUser
        //dettach events
        user.projectAdded -= onProjectChangedHandler
        user.projectRemoved -= onProjectChangedHandler
    }

    private val onProjectChangedHandler = {
        sender: Any, args: Pair<OTProject, Int>->
        listView.deferNotifyDataSetChanged()
    }


    class ProjectListAdapter(context: Context, val user: OTUser) : BaseAdapter(){

        override fun getCount(): Int {
            return user.projects.size
        }

        override fun getItem(position: Int): Any {
            return user.projects[position]
        }

        override fun getItemId(position: Int): Long {
            return position as Long;
        }

        init{

        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }


    }

}
