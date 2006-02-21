/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.audit;

import java.util.logging.Level;

/**
 * A record pertaining to the string table for AuditDetail records
 */
public class AuditDetailMessage {

    public AuditDetailMessage(int id, Level level, String message, boolean saveRequest, boolean saveResponse) {
        this.id = id;
        this.level = level;
        this.message = message;
        this.saveRequest = saveRequest;
        this.saveResponse = saveResponse;
    }

    public int getId() {
        return id;
    }

    public String getLevelName() {
        return level.getName();
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSaveRequest() {
        return saveRequest;
    }

    public boolean isSaveResponse() {
        return saveResponse;
    }

    private final int id;
    private final Level level;
    private final String message;
    private final boolean saveRequest;
    private final boolean saveResponse;
}
