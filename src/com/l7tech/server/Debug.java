/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

/**
 * @author alex
 * @version $Revision$
 */
public class Debug {
    private Debug() {
        debug = Boolean.getBoolean("com.l7tech.server.debug");
    }

    public static boolean on() {
        if ( instance == null ) instance = new Debug();
        return instance.debug;
    }

    private static Debug instance;
    private final boolean debug;
}
