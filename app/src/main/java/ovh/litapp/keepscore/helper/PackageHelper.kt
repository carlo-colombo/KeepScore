package ovh.litapp.keepscore.helper

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import ovh.litapp.keepscore.util.UtilLogger

object PackageHelper {

    private val log = UtilLogger(PackageHelper::class.java)

    @JvmStatic
    fun getVersionName(context: Context): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: NameNotFoundException) {
            // should never happen
            log.d(e, "unexpected exception")
            ""
        }

    @JvmStatic
    fun isDonateVersionInstalled(context: Context): Boolean =
        context.packageManager.checkSignatures(context.packageName, DONATE_PACKAGE) >= 0

    private const val DONATE_PACKAGE = "ovh.litapp.keepscore.donate"
}
