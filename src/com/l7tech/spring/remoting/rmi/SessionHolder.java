/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.spring.remoting.rmi;

import java.io.Serializable;
import java.security.SecureRandom;
import java.rmi.server.UID;

/**
 * @author emil
 * @version Dec 6, 2004
 */
final class SessionHolder implements Serializable {
    private Serializable sessionToken;

    private static ThreadLocal current = new ThreadLocal() {
        protected Object initialValue() {
            return null;
        }
    };

    public static SessionHolder getCurrent() {
        return (SessionHolder)current.get();
    }

    public static void setCurrent(SessionHolder s) {
        current.set(s);
    }

    SessionHolder(Serializable sessionToken) {
        sessionToken.getClass(); // npe if invalid
        this.sessionToken = sessionToken;
    }

    Serializable getSessionToken() {
        return sessionToken;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionHolder)) return false;

        final SessionHolder sessionHolder = (SessionHolder)o;

        if (!sessionToken.equals(sessionHolder.sessionToken)) return false;

        return true;
    }

    public int hashCode() {
        return sessionToken.hashCode();
    }

    public String toString() {
        return "[ session = "+sessionToken+" ]";
    }
}