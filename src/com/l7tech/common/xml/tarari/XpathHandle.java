/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.xml.tarari;

/**
 * A handle for an xpath expression managed by the current {@link com.l7tech.server.tarari.Xpaths} object.
 */
public class XpathHandle {
    private int refCount = 0;
    private final int index;
    private final String expression;
    private boolean installed = false;

    public XpathHandle(int index, String expr) {
        this.expression = expr;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String getExpression() {
        return expression;
    }

    public synchronized void ref() {
        refCount++;
    }

    public synchronized void unref() {
        if (--refCount < 1)
            refCount = 0;
    }

    public synchronized boolean inUse() {
        return (refCount > 0);
    }

    public void setInstalled() {
        this.installed = true;
    }

    public boolean isInstalled() {
        return installed;
    }
}
