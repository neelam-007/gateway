/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.tarari;

import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.XpathHandle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A data structure containing:
 * - the array of all xpath expressions to be compiled for the Tarari RAX compilation run
 * - the array of indices in the expression array that correspond to the expressions used by isSoap
 * - the Map&lt;Integer,String&gt; of array indices to xpath expressions
 * - the Map&lt;String,Integer&gt; of xpath expressions to array indices
 */
class Xpaths {
    /** The lowest-numbered empty element in the {@link #expressions} list */
    private volatile int nextHole;

    /** The current list of XPath expressions.  The built-in expressions live at the bottom of the list */
    private ArrayList expressions;

    /** The indices of the XPath expressions used by {@link com.tarari.xml.xpath.RAXContext#isSoap} */
    private final int[] soapUriIndices;

    /** A Map&lt;Integer,XpathHandle&gt; */
    private final Map indicesToHandles = new HashMap();
    /** A Map&lt;String,XpathHandle&gt; */
    private final Map expressionsToHandles = new HashMap();
    private static final String UNUSED = "/UNUSED";

    /**
     * Constructs the initial Xpaths containing just the built-in expressions used by isSoap etc.
     * <p>
     * Note that if no policy contains any hardware-friendly {@link com.l7tech.policy.assertion.XpathBasedAssertion}
     * this will become the runtime Xpaths object.
     *
     * @param builtInXpaths
     * @param soapUriIndices
     */
    Xpaths(ArrayList builtInXpaths, int[] soapUriIndices) {
        this.expressions = builtInXpaths;
        this.soapUriIndices = soapUriIndices;

        // The next hole is one higher than the current top element
        this.nextHole = builtInXpaths.size();
    }

    synchronized void add(String expr) throws InvalidXpathException {
        if (expr == null) throw new InvalidXpathException("Expression must not be null");
        XpathHandle handle = (XpathHandle)expressionsToHandles.get(expr);
        if (handle == null) {
/*
            try {
                new DOMXPath(expr);
            } catch (Exception e) {
                throw new InvalidXpathException(e);
            }
*/
            int index = findNextHole();
            handle = new XpathHandle(index, expr);
            expressionsToHandles.put(expr, handle);
            indicesToHandles.put(new Integer(index), handle);
            expressions.set(index, expr);
        }
        handle.ref();
    }

    /** Caller must hold lock */
    private int findNextHole() {
        while (expressions.size() < nextHole+1) {
            expressions.add(UNUSED);
            return nextHole++;
        }

        for (int i = nextHole; i < expressions.size(); i++) {
            if (expressions.get(nextHole) == UNUSED) {
                nextHole = i+1;
                return i;
            }
        }

        nextHole = expressions.size();
        expressions.add(UNUSED);
        return nextHole++;
    }

    synchronized void remove(String expr) {
        XpathHandle handle = (XpathHandle)expressionsToHandles.get(expr);
        if (handle == null) return;
        handle.unref();
        if (!handle.inUse()) {
            expressionsToHandles.remove(expr);
            indicesToHandles.remove(new Integer(handle.getIndex()));
            expressions.set(handle.getIndex(), UNUSED);
            if (nextHole > handle.getIndex()) {
                nextHole = handle.getIndex();
            }
        }
    }

    synchronized int[] getSoapUriIndices() {
        return soapUriIndices;
    }

    synchronized String[] getExpressions() {
        return (String[])expressions.toArray(new String[0]);
    }

    /**
     * Get the specified expression's zero-based index into the XPath result array in the specified compiler generation.
     *
     * @return the zero-based index if this xpath was present in the specified
     *         GlobalTarariContext compiler generation number,
     *         or NO_SUCH_EXPRESSION otherwise.  Note that this is the zero-based Java index rather than the one-based
     *         Tarari index.
     */
    synchronized int getIndex(String expression, long targetCompilerGeneration) {
        XpathHandle handle = (XpathHandle)expressionsToHandles.get(expression);
        if (handle == null || !handle.isInstalled(targetCompilerGeneration)) {
            return GlobalTarariContext.NO_SUCH_EXPRESSION;
        } else {
            return handle.getIndex();
        }
    }

    synchronized String getExpression(int index) {
        return (String)expressions.get(index);
    }

    synchronized XpathHandle getHandle(String expression) {
        return (XpathHandle)expressionsToHandles.get(expression);
    }

    /**
     * Mark the specified expression as installed and available in the hardware as of the specified compiler
     * generation count.
     *
     * @param expressions   the expression strings that have been installed.
     * @param compilerGeneration  the compiler generation in which they were installed.
     */
    synchronized void installed(String[] expressions, long compilerGeneration) {
        for (int i = 0; i < expressions.length; i++) {
            String expression = expressions[i];
            XpathHandle handle = (XpathHandle)expressionsToHandles.get(expression);
            if (handle != null) handle.setInstalled(compilerGeneration);
        }
    }
}
