package kr.ac.snu.hcil.omnitrack.core.database

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
/*
object UserTable: IntIdTable(){
    val objectId = varchar("object_id", 128).uniqueIndex()
    val name = varchar("name", 128).index()
    val email = varchar("email", 1024).uniqueIndex()
}

object ProjectTable: IntIdTable(){
    val objectId = UserTable.varchar("object_id", 128).uniqueIndex()
    val name = varchar("name", 128).index()
    val user = reference("user", UserTable)
    val position = integer("position")
}

object TrackerTable: IntIdTable(){
    val objectId = UserTable.varchar("object_id", 128).uniqueIndex()
    val name = varchar("name", 128).index()
    val project = reference("project", ProjectTable)
    val position = ProjectTable.integer("position")
}
*/

data class UserEntity(var id: Long, var name: String?, val email: String?, val projects: List<ProjectEntity>)

data class ProjectEntity(var id: Long, var objectId: String?, var name: String?, var user: UserEntity?, var position: Int?, val trackers: List<TrackerEntity>)

data class TrackerEntity(var id: Long, var objectId: String?, var name: String?, var project: ProjectEntity?, var position: Int?)
