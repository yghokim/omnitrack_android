/*
package kr.ac.snu.hcil.omnitrack.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.ui.VerticalSpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper

@Deprecated("no longer used")
class ProjectsActivity : UserSyncedActivity() {

    lateinit private var listView : RecyclerView

    lateinit private var projectListAdapter : ProjectListAdapter

    lateinit private var popupMessages : Array<String>
    companion object{
        const val CHANGE_PROJECT_SETTINGS = 0
        const val REMOVE_PROJECT = 1
    }

    override fun onStart(){
        super.onStart()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projects)
        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)

        val fab = findViewById(R.id.fab) as FloatingActionButton?
        fab!!.setOnClickListener { view ->
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show()
           //(application as OmniTrackApplication).syncUserToDb()
            user.projects.add(OTProject("Hihi"))
        }

        //attach events
        user.projectAdded += onProjectAddedHandler
        user.projectRemoved += onProjectRemovedHandler

        val content = this.findViewById(R.id.layout_projects) as ViewGroup


        listView = findViewById(R.id.ui_project_list_view) as RecyclerView
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        listView.layoutManager = layoutManager
        projectListAdapter = ProjectListAdapter(user)

        listView.adapter = projectListAdapter

        popupMessages = arrayOf(getString(R.string.msg_change_project_settings), getString(R.string.msg_remove_project))
        listView.itemAnimator = SlideInRightAnimator()
        listView.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.list_element_vertical_space)));
    }

    override fun onResume() {
        super.onResume()
        projectListAdapter.notifyDataSetChanged();
    }

    override fun onDestroy() {
        super.onDestroy()
        //dettach events
        user.projectAdded -= onProjectAddedHandler
        user.projectRemoved -= onProjectRemovedHandler
    }

    private val onProjectAddedHandler = {
        sender: Any, args: Pair<OTProject, Int>->
        projectListAdapter.notifyItemInserted(args.second)
    }

    private val onProjectRemovedHandler = {
        sender: Any, args: Pair<OTProject, Int>->
        projectListAdapter.notifyItemRemoved(args.second)
    }

    private fun handleProjectClick(project: OTProject)
    {
    }

    private fun handleProjectLongClick(project: OTProject)
    {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(project.name)
        builder.setItems(popupMessages){
            dialog, which ->
                when(which) {
                    CHANGE_PROJECT_SETTINGS -> {
                        intent = Intent(this, ProjectSettingsActivity::class.java)
                        intent.putExtra("projectId", project.objectId)
                        startActivity(intent)
                    }
                    REMOVE_PROJECT -> DialogHelper.makeYesNoDialogBuilder(this, project.name, getString(R.string.msg_confirm_remove_project), {->user.projects.remove(project)}).show()
                }
        }
        builder.show()
    }


    inner class ProjectListAdapter(val user: OTUser) : RecyclerView.Adapter<ProjectListAdapter.ViewHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.project_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindProject(user.projects[position])
        }

        override fun getItemCount(): Int {
            return user.projects.size
        }

        override fun getItemId(position: Int): Long {
            return position as Long;
        }


        inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view){
            lateinit var name: TextView

            init{
                name = view.findViewById(R.id.name) as TextView

                view.setOnClickListener {
                    handleProjectClick(user.projects[adapterPosition])
                }

                view.setOnLongClickListener {
                    view->
                        handleProjectLongClick(user.projects[adapterPosition])
                        true
                }
            }

            fun bindProject(project: OTProject){
                name.text = project.name
            }
        }
    }

}
*/
