/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.spring.remoting.rmi;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;

import javax.security.auth.Subject;
import java.io.Serializable;
import java.security.AccessController;
import java.security.Principal;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author emil
 * @version Dec 6, 2004
 */
final class AdminSessionInvocationFactory implements RemoteInvocationFactory, Serializable {
    protected final Log logger = LogFactory.getLog(getClass());

    public RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
        logger.debug("createRemoteInvocation for method: '"+methodInvocation.getMethod().getName()+"'");
        final Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject == null) {
            logger.debug("No Subject in the current calling context (subject == null)");
        } else {
            logger.debug("Using Subject(s) : "+getPrincipalNames(subject.getPrincipals()));
        }
        return new AdminSessionRemoteInvocation(methodInvocation, subject);
    }

    private String getPrincipalNames(Set principals) {
        Collection principalNames = new ArrayList();
        for (Iterator iterator = principals.iterator(); iterator.hasNext();) {
            Object o = (Object)iterator.next();
            if (o instanceof Principal) {
                Principal principal = (Principal)o;
                principalNames.add(principal.getName());
            } else {
                principalNames.add(o.toString());
            }
        }
        return principalNames.toString();
    }

}
