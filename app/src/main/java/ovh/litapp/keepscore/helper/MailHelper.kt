package ovh.litapp.keepscore.helper

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import ovh.litapp.keepscore.R

/**
 * Helper class for sending stuff through email.
 *
 * @author nolan
 */
object MailHelper {

    /**
     * Ask a mail app to send this uri as an attachment.
     *
     * @param context the context to start the mail app from
     * @param uri the content URI of the attachment
     * @param mimeType the MIME type of the attachment
     * @param subject the subject line
     * @param body the message body
     */
    @JvmStatic
    fun sendAsAttachment(
        context: Context,
        uri: Uri,
        mimeType: String,
        subject: String,
        body: String,
    ) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = mimeType

        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        // targetSdk 24+ forbids exposing a file:// URI; the caller passes a
        // content:// FileProvider URI, so grant read access explicitly (some
        // receivers only honour the ClipData grant).
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = ClipData.newRawUri("", uri)

        val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)

        if (resolveInfos.isEmpty()) {
            ToastHelper.showLong(context, R.string.toast_share_error_no_app)
        } else {
            context.startActivity(intent)
        }
    }
}
