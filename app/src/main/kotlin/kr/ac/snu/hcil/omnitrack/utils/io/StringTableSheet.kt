package kr.ac.snu.hcil.omnitrack.utils.io

import com.opencsv.CSVWriter
import java.io.OutputStream

/**
 * Created by Young-Ho on 3/8/2017.
 */
class StringTableSheet {
    val columns: MutableList<String> = ArrayList<String>()
    val rows: MutableList<MutableList<String?>> = ArrayList<MutableList<String?>>()

    fun validate(): Boolean {
        return rows.find { it.size != columns.size } == null
    }

    override fun toString(): String {
        return "table with ${columns.size} columns, ${rows.size} rows."
    }

    fun storeToStream(outputStream: OutputStream) {
        val csvWriter = CSVWriter(outputStream.writer(Charsets.UTF_8), ',', '"')

        csvWriter.writeNext(columns.toTypedArray())
        csvWriter.writeAll(rows.map { it.toTypedArray() })
        csvWriter.close()
    }
}