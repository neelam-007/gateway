package com.l7tech.server.event.admin;

import com.l7tech.objectmodel.Goid;

import java.util.logging.Level;

/**
 * Event published when a private key is exported.
 */
public class KeyExportedEvent extends AdminEvent {
    public KeyExportedEvent(Object source, Goid keystoreId, String keyAlias, String keyDn) {
        super(source, "Exported private key " + keystoreId + ":" + keyAlias + " (" + keyDn + ")");
    }

    public Level getMinimumLevel() {
        return Level.WARNING;
    }
}
