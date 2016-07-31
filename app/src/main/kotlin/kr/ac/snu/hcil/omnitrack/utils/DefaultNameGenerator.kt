package kr.ac.snu.hcil.omnitrack.utils

/**
 * Created by Young-Ho on 7/31/2016.
 */
object DefaultNameGenerator {

    fun generateName(prefix: String, existingNames: Collection<String>): String {
        var counter = 0
        while (existingNames.contains("${prefix} ${++counter}")) {
        }

        return "${prefix} ${counter}"
    }

}