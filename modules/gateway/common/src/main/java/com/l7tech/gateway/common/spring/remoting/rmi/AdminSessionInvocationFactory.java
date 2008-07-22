/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.gateway.common.spring.remoting.rmi;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;

import javax.security.auth.Subject;
import java.io.Serializable;
import java.security.AccessController;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for SSM/SSG remote invocation with security context.
 *
 * @author emil, $Author$
 * @version Dec 6, 2004, $Revision$
 */
final class AdminSessionInvocationFactory implements RemoteInvocationFactory, Serializable {

    //- PUBLIC

    public RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
        if(logger.isLoggable(Level.FINER)) {
            logger.finer("In createRemoteInvocation for method: '"+methodInvocation.getMethod().getName()+"'");
        }
        final Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject == null) {
            logger.fine("No Subject in the current calling context (subject == null)");
        } else {
            if(logger.isLoggable(Level.FINER))
                logger.finer("Using Subject(s) : "+getPrincipalNames(subject.getPrincipals()));
        }
        return new AdminSessionRemoteInvocation(methodInvocation, subject);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AdminSessionInvocationFactory.class.getName());
    private static final long serialVersionUID = -8545036635669595404L;

    private String getPrincipalNames(Set<Principal> principals) {
        Collection<String> principalNames = new ArrayList<String>();
        for (Principal principal : principals) {
            principalNames.add(principal.getName());
        }
        return principalNames.toString();
    }
}
