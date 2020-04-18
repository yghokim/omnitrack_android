package kr.ac.snu.hcil.omnitrack.views.recording

import android.net.Uri
import kr.ac.snu.hcil.android.common.events.Event
import java.io.File

interface IAudioRecorderView {
    var audioFileUri: Uri
    var recordingOutputDirectoryPathOverride: File?
    val audioFileUriChanged: Event<Uri>
}