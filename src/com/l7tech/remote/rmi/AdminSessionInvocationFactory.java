/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.remote.rmi;

import org.springframework.remoting.support.RemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.security.auth.Subject;
import java.security.SecureRandom;
import java.security.AccessController;
import java.io.Serializable;

/**
 * @author emil
 * @version Dec 6, 2004
 */
final class AdminSessionInvocationFactory implements RemoteInvocationFactory, Serializable {
    protected final Log logger = LogFactory.getLog(getClass());

    public RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
        logger.info("createRemoteInvocation");
        return new AdminSessionRemoteInvocation(methodInvocation, Subject.getSubject(AccessController.getContext()));
    }

}
