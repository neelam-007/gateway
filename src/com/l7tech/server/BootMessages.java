/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.common.audit.Messages;

import java.util.logging.Level;

/**
 * Message catalog for {@link BootProcess} and associated classes
 */
public class BootMessages extends Messages {
    public static final M DELETING_ATTACHMENT   = m(1000, Level.INFO,    "Deleting leftover attachment cache file: {0}");
    public static final M XMLHARDWARE_INIT      = m(1001, Level.INFO,    "Initializing Hardware XML Acceleration");
    public static final M XMLHARDWARE_ERROR     = m(1002, Level.WARNING, "Error initializing Tarari board");
    public static final M XMLHARDWARE_DISABLED  = m(1003, Level.INFO,    "Hardware XML Acceleration Disabled");
    public static final M NO_IP                 = m(1004, Level.SEVERE,  "Couldn't get local IP address. Will use 127.0.0.1 in audit records.");
    public static final M CRYPTO_INIT           = m(1005, Level.INFO,    "Initializing cryptography subsystem");
    public static final M CRYPTO_ASYMMETRIC     = m(1006, Level.INFO,    "Using asymmetric cryptography provider: {0}");
    public static final M CRYPTO_SYMMETRIC      = m(1007, Level.INFO,    "Using symmetric cryptography provider: {0}");
    public static final M COMPONENT_INIT_FAILED = m(1008, Level.SEVERE,  "Couldn't initialize server component '{0}'");
}
