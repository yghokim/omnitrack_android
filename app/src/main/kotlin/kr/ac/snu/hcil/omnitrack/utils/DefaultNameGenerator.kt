package kr.ac.snu.hcil.omnitrack.utils

/**
 * Created by Young-Ho on 7/31/2016.
 */
object DefaultNameGenerator {

    fun generateName(prefix: String, existingNames: Collection<String>, ignoreCounterAtFirst: Boolean = true): String {


        var counter = 0

        if (ignoreCounterAtFirst) {
            if (!existingNames.contains(prefix)) {
                return prefix
            } else {
                counter = 1
            }
        }


        while (existingNames.contains("${prefix} ${++counter}")) {
        }

        return "${prefix} ${counter}"
    }

}