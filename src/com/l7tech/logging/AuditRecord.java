/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AuditRecord extends LogRecord {
    public AuditRecord( Level level, String message ) {
        super( level, message );
    }
}
