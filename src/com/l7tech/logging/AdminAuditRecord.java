/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.logging;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class AdminAuditRecord extends AuditRecord {
    public AdminAuditRecord( Level level, String message ) {
        super( level, message );
    }
}
