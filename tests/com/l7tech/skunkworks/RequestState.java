/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

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
    public static final RequestState CLIENT_RECEIVING_WAIT  = new RequestState(n++, "Waiting for request from client");
    public static final RequestState CLIENT_RECEIVING_READ  = new RequestState(n++, "Receiving request from client");
    public static final RequestState SSG_PROCESSING         = new RequestState(n++, "Processing policy");
    public static final RequestState SERVICE_SENDING_WAIT   = new RequestState(n++, "Waiting to send request to service");
    public static final RequestState SERVICE_SENDING_WRITE  = new RequestState(n++, "Sending request to service");
    public static final RequestState SERVICE_RECEIVING_WAIT = new RequestState(n++, "Waiting for service to respond");
    public static final RequestState SERVICE_RECEIVING_READ = new RequestState(n++, "Reading response from service");
    public static final RequestState CLIENT_SENDING_WAIT    = new RequestState(n++, "Waiting to send response to client");
    public static final RequestState CLIENT_SENDING_WRITE   = new RequestState(n++, "Sending response to client");

    private static final RequestState[] VALUES = { CLIENT_RECEIVING_WAIT,
                                                   CLIENT_RECEIVING_READ,
                                                   SSG_PROCESSING,
                                                   SERVICE_SENDING_WAIT,
                                                   SERVICE_SENDING_WRITE,
                                                   SERVICE_RECEIVING_WAIT,
                                                   SERVICE_RECEIVING_READ,
                                                   CLIENT_SENDING_WAIT,
                                                   CLIENT_SENDING_WRITE };

    private RequestState(int num, String name) {
        this.num = num;
        this.name = name;
    }

    private int num;
    private String name;

    // Pro forma
    protected Object readResolve() throws ObjectStreamException {
        return VALUES[num];
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
