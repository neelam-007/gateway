/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.alert;

import com.l7tech.objectmodel.*;

import java.util.List;

/**
 * Interface for administering persistent instances of {@link AlertEvent} and {@link Notification}.
 */
public interface AlertAdmin {
    EntityHeader[] findAllEventHeaders() throws FindException;
    List findAllEvents() throws FindException;
    AlertEvent findEventByPrimaryKey(long oid) throws FindException;
    long saveEvent(AlertEvent event) throws SaveException, UpdateException;
    void deleteEvent(long oid) throws DeleteException, FindException;

    EntityHeader[] findAllNotificationHeaders() throws FindException;
    List findAllNotifications() throws FindException;
    Notification findNotificationByPrimaryKey(long oid) throws FindException;
    long saveNotification(Notification action) throws SaveException, UpdateException;
    void deleteNotification(long oid) throws DeleteException, FindException;
}
