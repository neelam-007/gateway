/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An abstract skeleton of an HttpResponseKnob implementation.
 */
public abstract class AbstractHttpResponseKnob implements HttpResponseKnob {
    protected final List<Pair<String, Object>> headersToSend = new ArrayList<Pair<String, Object>>();
    protected final List<String> challengesToSend = new ArrayList<String>();
    protected int statusToSet;

    public void setDateHeader(String name, long date) {
        headersToSend.add(new Pair<String, Object>(name, date));
    }

    public void addDateHeader(String name, long date) {
        headersToSend.add(new Pair<String, Object>(name, date));
    }

    public void setHeader(String name, String value) {
        // Clear out any previous value
        for (Iterator<Pair<String, Object>> i = headersToSend.iterator(); i.hasNext();) {
            Pair<String, Object> pair = i.next();
            if (name.equals(pair.left)) i.remove();
        }
        headersToSend.add(new Pair<String, Object>(name, value));
    }

    public void addChallenge(String value) {
        challengesToSend.add(value);
    }

    public void addHeader(String name, String value) {
        headersToSend.add(new Pair<String, Object>(name, value));
    }

    public String[] getHeaderValues(String name) {
        ArrayList<Pair<String,Object>> tmp = new ArrayList<Pair<String,Object>>();
        for (Pair<String,Object> pair : headersToSend) {
            if (name.compareToIgnoreCase(pair.left) == 0) {
                tmp.add(pair);
            }
        }
        String[] output = new String[tmp.size()];
        int i = 0;
        for (Pair pair : tmp) {
            output[i] = pair.right.toString();
            i++;
        }
        return output;
    }

    public boolean containsHeader(String name) {
        for (Pair<String, Object> pair : headersToSend) {
            if (name.equals(pair.left)) return true;
        }

        return false;
    }

    public void setStatus(int code) {
        statusToSet = code;
    }

    public int getStatus() {
        return statusToSet;
    }
}
