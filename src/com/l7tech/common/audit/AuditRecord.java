/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AuditRecord {
    public AuditRecord( Level level, String message ) {
        this.level = level;
        this.message = message;
    }

    public AuditRecord(Level level) {
        this.level = level;
    }

    protected Level level;
    protected String message;
}
