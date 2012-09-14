/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event.system;

import com.l7tech.identity.User;
import com.l7tech.server.event.admin.DetailedAdminEvent;

import java.util.logging.Level;

/**
 * An audit event related to the Backup Service.
 *
 * @since SecureSpan 4.3
 * @author rmak
 */
public class BackupEvent extends DetailedAdminEvent {
    private final Level _level;
    private final String _clientAddr;
    private final User _user;

    /**
     * @param source        event source
     * @param level         audit level
     * @param message       audit detail message
     * @param clientAddr    IP address of client machine sending the request
     * @param user          user issuing the request; can be <code>null</code> if not available
     */
    public BackupEvent(final Object source,
                       final Level level,
                       final String message,
                       final String clientAddr,
                       final User user) {
        super(source, message);
        _level = level;
        _clientAddr = clientAddr;
        _user = user;
    }

    /**
     * @return audit level
     */
    public Level getLevel() {
        return _level;
    }

    /**
     * @return IP address of client machine sending the request
     */
    public String getClientAddr() {
        return _clientAddr;
    }

    /**
     * @return user issuing the request; can be <code>null</code> if not available
     */
    public User getUser() {
        return _user;
    }
}
