/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * An abstract skeleton of an HttpResponseKnob implementation.
 */
public abstract class AbstractHttpResponseKnob implements HttpResponseKnob {
    private static final Logger logger = Logger.getLogger(AbstractHttpResponseKnob.class.getName());
    protected final List headersToSend = new ArrayList();
    protected final List challengesToSend = new ArrayList();
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

    public void addChallenge(String value) {
        challengesToSend.add(value);
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
        final String name;
        final Object value;
    }
}
