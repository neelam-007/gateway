/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.remote.jini.l7code;

import com.l7tech.remote.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * @author emil
 * @version Mar 25, 2004
 */
public class CodeServerImpl extends RemoteService implements CodeServer {
    private static final Logger logger = Logger.getLogger(CodeServer.class.getName());

    public CodeServerImpl(String[] options, LifeCycle lifeCycle)
      throws ConfigurationException, IOException {
        super(options, lifeCycle);
    }

    public byte[] geResource(String resource) throws IOException {
        logger.fine("Resource lookup "+resource);
        InputStream in = getClass().getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new FileNotFoundException(resource);
        }
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int nr = -1;
            while ((nr = in.read(buffer)) !=-1) {
                bo.write(buffer, 0, nr);
            }
            return bo.toByteArray();
        } finally {
            in.close();
        }
    }
}