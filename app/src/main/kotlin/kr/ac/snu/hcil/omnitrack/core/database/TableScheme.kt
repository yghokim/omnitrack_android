package kr.ac.snu.hcil.omnitrack.core.database

/**
 * Created by younghokim on 16. 8. 20..
 */
abstract class TableScheme {

    val _ID = "_id"
    val LOGGED_AT = "logged_at"
    val UPDATED_AT = "updated_at"

    abstract val tableName: String

    abstract val intrinsicColumnNames: Array<String>

    val columnNames: Array<String> by lazy {
        Array<String>((intrinsicColumnNames.size) + 3) {
            index ->
            when (index) {
                0 -> _ID
                1 -> LOGGED_AT
                2 -> UPDATED_AT
                else -> intrinsicColumnNames[index - 3]
            }
        }
    }


    val creationQueryString: String by lazy {
        "CREATE TABLE ${tableName} (${_ID} INTEGER PRIMARY KEY, ${creationColumnContentString}, ${LOGGED_AT} INTEGER, ${UPDATED_AT} INTEGER);"
    }

    abstract val creationColumnContentString: String

    open val indexCreationQueryString: String = ""

    fun makeIndexQueryString(unique: Boolean, name: String, vararg columns: String): String {
        return "CREATE${if (unique) {
            " UNIQUE"
        } else {
            ""
        }} INDEX ${tableName}_$name ON $tableName (${columns.joinToString(", ")});"
    }

    fun makeForeignKeyStatementString(column: String, foreignTable: String): String {
        return "$column INTEGER REFERENCES $foreignTable"
    }
}