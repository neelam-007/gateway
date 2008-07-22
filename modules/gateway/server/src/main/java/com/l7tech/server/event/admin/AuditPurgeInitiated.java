/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event.admin;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class AuditPurgeInitiated extends AdminEvent {
    public AuditPurgeInitiated(Object source) {
        super(source, "Audit purge requested");
    }

    public Level getMinimumLevel() {
        return Level.WARNING;
    }
}
