/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.async;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * SSG message processing states (typesafe enum)
 *
 * @author alex
 * @version $Revision$
 */
public class RequestState implements Serializable {
    private static int n = 0;
    public static final RequestState IDLE                   = new RequestState(n++, "Idle");
    public static final RequestState CLIENT_RECEIVE_WAIT    = new RequestState(n++, "Waiting for request data from client");
    public static final RequestState CLIENT_RECEIVE_READ    = new RequestState(n++, "Receiving request data from client");
    public static final RequestState POLICY_WAIT            = new RequestState(n++, "Waiting for policy processing");
    public static final RequestState POLICY_PROCESS         = new RequestState(n++, "Processing policy");
    public static final RequestState   SERVICE_SEND_WAIT    = new RequestState(n++, "Waiting to send request to service");
    public static final RequestState   SERVICE_SEND_WRITE   = new RequestState(n++, "Sending request to service");
    public static final RequestState   SERVICE_RECEIVE_WAIT = new RequestState(n++, "Waiting for service to respond");
    public static final RequestState   SERVICE_RECEIVE_READ = new RequestState(n++, "Reading response from service");
    public static final RequestState POLICY_DONE            = new RequestState(n++, "Policy processing completed");
    public static final RequestState CLIENT_SEND_WAIT       = new RequestState(n++, "Waiting to send response to client");
    public static final RequestState CLIENT_SEND_WRITE      = new RequestState(n++, "Sending response to client");
    public static final RequestState DONE                   = new RequestState(n++, "Done");

    private static final RequestState[] STATES = {
        IDLE,
        CLIENT_RECEIVE_WAIT,
        CLIENT_RECEIVE_READ,
        POLICY_WAIT,
        POLICY_PROCESS,
          SERVICE_SEND_WAIT,
          SERVICE_SEND_WRITE,
          SERVICE_RECEIVE_WAIT,
          SERVICE_RECEIVE_READ,
        POLICY_DONE,
        CLIENT_SEND_WAIT,
        CLIENT_SEND_WRITE
    };

    private static final RequestState[][] TRANSITIONS = {
        /* IDLE to */
        { CLIENT_RECEIVE_WAIT, CLIENT_RECEIVE_READ },
        /* CLIENT_RECEIVE_WAIT to */
        { CLIENT_RECEIVE_READ, POLICY_WAIT },
        /* CLIENT_RECEIVE_READ to */
        { CLIENT_RECEIVE_WAIT, POLICY_WAIT },
        /* POLICY_WAIT to */
        { POLICY_PROCESS },
        /* POLICY_PROCESS to */
        { POLICY_WAIT, POLICY_DONE, SERVICE_SEND_WAIT, SERVICE_SEND_WRITE, SERVICE_RECEIVE_WAIT, SERVICE_RECEIVE_READ },
        /* SERVICE_SEND_WAIT to */
        { SERVICE_SEND_WRITE },
        /* SERVICE_SEND_WRITE to */
        { SERVICE_SEND_WAIT, SERVICE_RECEIVE_WAIT },

    };

    private RequestState(int num, String name) {
        this.num = num;
        this.name = name;
    }

    private int num;
    private String name;

    // Pro forma
    protected Object readResolve() throws ObjectStreamException {
        return STATES[num];
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestState)) return false;

        final RequestState state = (RequestState)o;

        if (num != state.num) return false;
        if (name != null ? !name.equals(state.name) : state.name != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = num;
        result = 29 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

}
