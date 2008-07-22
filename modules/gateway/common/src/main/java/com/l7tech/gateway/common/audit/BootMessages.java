/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.gateway.common.audit;

import java.util.logging.Level;

/**
 * Message catalog for {@link com.l7tech.server.BootProcess} and associated classes.
 * The ID range 1000-1999 inclusive is reserved for these messages.
 */
public class BootMessages extends Messages {
    public static final M DELETING_ATTACHMENT   = m(1000, Level.INFO,    "Deleting leftover attachment cache file: {0}");
    public static final M XMLHARDWARE_INIT      = m(1001, Level.INFO,    "Initializing hardware XML acceleration");
    public static final M XMLHARDWARE_ERROR     = m(1002, Level.WARNING, "Error initializing Tarari board");
    public static final M XMLHARDWARE_DISABLED  = m(1003, Level.INFO,    "Hardware XML acceleration disabled");
    public static final M NO_IP                 = m(1004, Level.WARNING, "Unable to retrieve local IP address - 127.0.0.1 will be used in audit records");
    public static final M CRYPTO_INIT           = m(1005, Level.INFO,    "Initializing cryptography subsystem");
    public static final M CRYPTO_ASYMMETRIC     = m(1006, Level.INFO,    "Using asymmetric cryptography provider: {0}");
    public static final M CRYPTO_SYMMETRIC      = m(1007, Level.INFO,    "Using symmetric cryptography provider: {0}");
    public static final M COMPONENT_INIT_FAILED = m(1008, Level.WARNING, "Could not initialize server component ''{0}''");
    public static final M UPGRADE_TASK_IGNORED  = m(1009, Level.WARNING, "Ignoring upgrade task: {0}");
    public static final M UPGRADE_TASK_NONFATAL = m(1010, Level.WARNING, "Upgrade task failed, but will attempt to boot anyway: {0}");
    public static final M UPGRADE_TASK_FATAL    = m(1011, Level.WARNING, "Upgrade task failed; unable to proceed: {0}");
    public static final M UPGRADE_TASK_WARNING  = m(1012, Level.WARNING, "Upgrade task warning: {0}");
    // MAX -                                      m(1999
}
