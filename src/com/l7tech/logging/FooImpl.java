/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.logging;

import java.rmi.RemoteException;
import java.util.logging.Logger;

/**
 * @author emil
 * @version Oct 20, 2004
 */
public class FooImpl implements Foo {
    private static final Logger logger = Logger.getLogger(FooImpl.class.getName());

    public void echo(String msg) throws RemoteException {
        logger.info("msg is" + msg);
    }
}