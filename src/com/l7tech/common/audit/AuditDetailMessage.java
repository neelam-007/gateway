/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.audit;

import com.l7tech.objectmodel.imp.EntityImp;

/**
 * A record pertaining to the string table for AuditDetail records
 */
public class AuditDetailMessage extends EntityImp {
    /** @deprecated */
    public AuditDetailMessage() {
    }

    public AuditDetailMessage(String level, String message) {
        this.level = level;
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    /** @deprecated */
    public void setLevel(String level) {
        this.level = level;
    }

    /** @deprecated */
    public void setMessage(String message) {
        this.message = message;
    }

    private String level;
    private String message;
}
