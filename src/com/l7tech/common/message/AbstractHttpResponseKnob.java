/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An abstract skeleton of an HttpResponseKnob implementation.
 */
public abstract class AbstractHttpResponseKnob implements HttpResponseKnob {
    protected final List headersToSend = new ArrayList();
    protected int statusToSet;

    public void setDateHeader(String name, long date) {
        headersToSend.add(new HttpServletResponseKnob.Pair(name, new Long(date)));
    }

    public void addDateHeader(String name, long date) {
        headersToSend.add(new HttpServletResponseKnob.Pair(name, new Long(date)));
    }

    public void setHeader(String name, String value) {
        // Clear out any previous value
        for (Iterator i = headersToSend.iterator(); i.hasNext();) {
            HttpServletResponseKnob.Pair pair = (HttpServletResponseKnob.Pair)i.next();
            if (name.equals(pair.name)) i.remove();
        }
        headersToSend.add(new HttpServletResponseKnob.Pair(name, value));
    }

    public void addHeader(String name, String value) {
        headersToSend.add(new HttpServletResponseKnob.Pair(name, value));
    }

    public boolean containsHeader(String name) {
        for (Iterator i = headersToSend.iterator(); i.hasNext();) {
            HttpServletResponseKnob.Pair pair = (HttpServletResponseKnob.Pair)i.next();
            if (name.equals(pair.name)) return true;
        }

        return false;
    }

    public void setStatus(int code) {
        statusToSet = code;
    }

    public int getStatus() {
        return statusToSet;
    }

    protected static final class Pair {
        Pair(String name, Object value) {
            this.name = name;
            this.value = value;
        }
        String name;
        Object value;
    }
}
