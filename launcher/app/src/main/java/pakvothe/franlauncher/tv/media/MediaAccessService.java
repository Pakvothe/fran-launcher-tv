package pakvothe.franlauncher.tv.media;

import android.service.notification.NotificationListenerService;

/**
 * Vacío a propósito: no lee ni guarda notificaciones. Existe solo porque getActiveSessions() exige un
 * listener habilitado por el usuario para ver qué se está reproduciendo. Nada sale de la tele.
 */
public final class MediaAccessService extends NotificationListenerService {
    /** Lo pone el Stage: si la Activity arrancó antes de que el sistema conectara el listener, reintenta acá. */
    public static Runnable onConnected;

    @Override
    public void onListenerConnected() {
        Runnable r = onConnected;
        if (r != null) r.run();
    }
}
