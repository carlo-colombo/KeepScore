package ovh.litapp.keepscore.helper;

import java.util.List;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import ovh.litapp.keepscore.R;

/**
 * Helper class for sending stuff through email.
 * @author nolan
 *
 */
public class MailHelper {

    /**
     * Ask a mail app to send this uri as an attachment.
     * @param context
     * @param uri
     * @param mimeType
     * @param subject
     * @param body
     */
    public static void sendAsAttachment(Context context, Uri uri, String mimeType, String subject, String body) {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType(mimeType);
        
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        // targetSdk 24+ forbids exposing a file:// URI; the caller passes a
        // content:// FileProvider URI, so grant read access explicitly (some
        // receivers only honour the ClipData grant).
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("", uri));
        
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, 0);
        
        if (resolveInfos.isEmpty()) {
            ToastHelper.showLong(context, R.string.toast_share_error_no_app);
        } else {
            context.startActivity(intent);
        }
    }
}
