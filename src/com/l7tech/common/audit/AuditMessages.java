/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.audit;

import com.l7tech.common.Messages;
import com.l7tech.server.ServerConfig;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class AuditMessages extends Messages {
    public static final M INVALID_AGE =
            m(10000, Level.INFO,
              ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE +
              " value '{0}' is not a valid number. Using {1} instead.");
}
