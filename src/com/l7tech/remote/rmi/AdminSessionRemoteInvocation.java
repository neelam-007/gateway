/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.remote.rmi;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.RemoteAccessException;

import javax.security.auth.Subject;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.security.Principal;
import java.util.Set;
import java.util.Iterator;

/**
 * @author emil
 * @version Dec 6, 2004
 */
class AdminSessionRemoteInvocation extends RemoteInvocation {
    protected final Log logger = LogFactory.getLog(getClass());
    private Subject subject;

    AdminSessionRemoteInvocation(MethodInvocation methodInvocation, Subject subject) {
        super(methodInvocation);
        this.subject = subject;
    }

    public Object invoke(final Object targetObject)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("the subject is " + (subject == null ? "null" : "'"+extractPrincipalName(subject)+"'"));
            }
            return
              Subject.doAs(subject, new PrivilegedExceptionAction() {
                  public Object run() throws Exception {
                      return AdminSessionRemoteInvocation.super.invoke(targetObject);
                  }
              });
        } catch (PrivilegedActionException e) {
            Throwable ex = e.getCause();
            if (ex instanceof NoSuchMethodException) {
                throw (NoSuchMethodException)ex;
            } else if (ex instanceof IllegalAccessException) {
                throw (IllegalAccessException)ex;
            } else if (ex instanceof InvocationTargetException) {
                throw (InvocationTargetException)ex;
            } else if (ex instanceof RuntimeException) {
                throw (RuntimeException)ex;
            }
            throw new RemoteAccessException("invoke error", e);
        }
    }

    private String extractPrincipalName(Subject subject) {
           Set principals = subject.getPrincipals();
           for (Iterator iterator = principals.iterator(); iterator.hasNext();) {
               Object o = (Object)iterator.next();
               if (o instanceof Principal) {
                   return ((Principal)o).getName();
               } else {
                   return o.toString();
               }
           }
           return "no principal set";
       }

}