/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.remote.jini.l7code;

import com.l7tech.remote.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * @author emil
 * @version Mar 25, 2004
 */
public class CodeServerImpl extends RemoteService implements CodeServer {
    public CodeServerImpl(String[] options, LifeCycle lifeCycle) throws ConfigurationException, IOException {
        super(options, lifeCycle);
    }

    public byte[] geResource(String resource) throws RemoteException {
        return new byte[0];
    }
}