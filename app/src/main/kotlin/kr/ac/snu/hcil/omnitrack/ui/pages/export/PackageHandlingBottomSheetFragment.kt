package kr.ac.snu.hcil.omnitrack.ui.pages.export

import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.bottomsheet_handle_extracted_package.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.DismissingBottomSheetDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.components.common.viewholders.RecyclerViewMenuAdapter
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import org.jetbrains.anko.support.v4.act
import java.text.SimpleDateFormat
import java.util.*

class PackageHandlingBottomSheetFragment : DismissingBottomSheetDialogFragment(R.layout.bottomsheet_handle_extracted_package) {

    companion object {
        const val KEY_JSON_CONTENT_STRING: String = "jsonContentString"
        const val REQUEST_CODE_PICK_FILE_LOCATION = 12

        fun makeInstance(jsonString: String): PackageHandlingBottomSheetFragment {
            return PackageHandlingBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_JSON_CONTENT_STRING, jsonString)
                }
            }
        }
    }

    private lateinit var viewModel: PackageExportViewModel

    fun showJsonDialog(json: String, fragmentManager: FragmentManager, tag: String) {
        arguments = Bundle().apply {
            putString(KEY_JSON_CONTENT_STRING, json)
        }
        show(fragmentManager, tag)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(act).get(PackageExportViewModel::class.java)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        println("code: $requestCode, resultCode: $resultCode")
        if (requestCode == REQUEST_CODE_PICK_FILE_LOCATION) {
            if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
                val exportUri = data.data
                val success = try {
                    val outputStream = act.contentResolver.openOutputStream(exportUri)
                    outputStream.write((arguments?.getString(KEY_JSON_CONTENT_STRING)
                            ?: "").toByteArray())
                    outputStream.close()
                    true
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    false
                }
                if (success) {
                    Toast.makeText(act, "Saved JSON to file.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(act, "Failed to save file.", Toast.LENGTH_LONG).show()
                }
                dismiss()
            }
        }
    }

    override fun setupDialogAndContentView(dialog: Dialog, contentView: View) {
        contentView.ui_list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        val jsonString = arguments?.getString(KEY_JSON_CONTENT_STRING)

        val menus = listOf(
                RecyclerViewMenuAdapter.MenuItem(null,
                        "Save to file",
                        "Save the package into JSON file in local storage.",
                        {
                            val intent = FileHelper.makeSaveLocationPickIntent("omnitrack_package_${SimpleDateFormat("yyyyMMddHHmmss").format(Date())}.json")
                            startActivityForResult(intent, REQUEST_CODE_PICK_FILE_LOCATION)
                        }),
                RecyclerViewMenuAdapter.MenuItem(null,
                        "Share..",
                        "Share the package content in a built-in share popup.",
                        {
                            val intent = Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, jsonString)
                            val chooser = Intent.createChooser(intent, "Export the JSON string")
                            startActivity(chooser)
                            dismissAllowingStateLoss()
                        }),
                RecyclerViewMenuAdapter.MenuItem(null,
                        "Instant share to Research Kit",
                        "Share the package content to research platform",
                        {
                            if (jsonString != null) {
                                UploadTemporaryPackageDialogFragment.makeInstance(jsonString).show(act.supportFragmentManager, "InstantSharePackage")
                                dismissAllowingStateLoss()
                            }
                        })
        )

        contentView.ui_list.adapter = object : RecyclerViewMenuAdapter() {
            override fun getMenuItemAt(index: Int): MenuItem {
                return menus[index]
            }

            override fun getItemCount(): Int {
                return menus.size
            }

        }
    }
}