package com.mirceone.inventoryapp.ops;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Bridges Logback events into {@link MaintenanceLogRing}.
 */
public class MaintenanceRingAppender extends AppenderBase<ILoggingEvent> {

    private static volatile MaintenanceLogRing ring;

    public static void setRing(MaintenanceLogRing maintenanceLogRing) {
        MaintenanceRingAppender.ring = maintenanceLogRing;
    }

    @Override
    protected void append(ILoggingEvent event) {
        MaintenanceLogRing r = ring;
        if (r == null || !isStarted()) {
            return;
        }
        String msg = event.getFormattedMessage();
        if (event.getThrowableProxy() != null) {
            msg = msg + " | " + event.getThrowableProxy().getClassName();
        }
        r.push(msg);
    }
}
