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
    private long firstCompilerGeneration = -1;

    /**
     * Create a handle for tracking a compilable xpath.
     *
     * @param index   index into the expression array.  This is the zero-based Java index, not the 1-based Tarari index.
     * @param expr    the XPath expression to track.  Must not be null.
     */
    public XpathHandle(int index, String expr) {
        if (expr == null) throw new NullPointerException();
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

    /**
     * @param compilerGeneration the first GlobalTarariContext compiler generation number when this expression went live.
     *        MessageContexts evaluated against earlier compiler generations will not include this expression.
     */
    public void setInstalled(long compilerGeneration) {
        if (firstCompilerGeneration == -1)
            firstCompilerGeneration = compilerGeneration;
    }

    /** @return true iff. this xpath has been installed in the hardware already. */
    private boolean isInstalled() {
        return firstCompilerGeneration >= 0;
    }

    /** @return true iff. this xpath was present in the specified GlobalTarariContext compiler generation number. */
    public boolean isInstalled(long targetCompilerGeneration) {
        return targetCompilerGeneration >= firstCompilerGeneration;
    }
}
