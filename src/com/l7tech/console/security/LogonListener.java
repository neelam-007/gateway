/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.security;

import java.util.EventListener;


/**
 * @author emil
 * @version Sep 7, 2004
 */
public interface LogonListener extends EventListener {
    /**
     * Invoked on logon event
     *
     * @param e describing the logon event
     */
    void onLogon(LogonEvent e);

    /**
     * Invoked on logoff
     *
     * @param e describing the logoff event
     */
    void onLogoff(LogonEvent e);

}