package pakvothe.franlauncher.tv.util;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

/** Intents hacia afuera. */
public final class Launch {
    private Launch() {}

    /** component = "paquete/clase". */
    public static boolean app(Context c, String component) {
        ComponentName cn = ComponentName.unflattenFromString(component);
        if (cn == null) return false;
        Intent i = new Intent(Intent.ACTION_MAIN).setComponent(cn)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return start(c, i);
    }

    public static boolean appInfo(Context c, String pkg) {
        return start(c, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @SuppressWarnings("deprecation")
    public static boolean uninstall(Context c, String pkg) {
        return start(c, new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static boolean systemSettings(Context c) {
        return start(c, new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static boolean start(Context c, Intent i) {
        try {
            c.startActivity(i);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }
}
