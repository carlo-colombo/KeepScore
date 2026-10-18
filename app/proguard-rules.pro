# KeepScore R8 rules.
#
# The ActionBarSherlock and bundled support-v4 keeps from the legacy
# proguard.cfg are gone with those dependencies. These are the rules the app
# still needs.

-keepattributes *Annotation*

# Components instantiated by the framework.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# Custom views inflated from XML layouts.
-keep public class ovh.litapp.keepscore.widget.*
-keep public class ovh.litapp.keepscore.fragment.*

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
