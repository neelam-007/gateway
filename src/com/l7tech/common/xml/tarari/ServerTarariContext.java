/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.InvalidXpathException;

/**
 * Implementations manage the server-wide state of the RAX API
 */
public interface ServerTarariContext {
    int NO_SUCH_EXPRESSION = -1;

    /**
     * Adds an XPath expression to the context.
     * <p>
     * If this expression is valid, after {@link #compile} is called, subsequent calls to {@link #getIndex}
     * will return a positive result.
     * @param expression the XPath expression to add to the context.
     */
    void add(String expression) throws InvalidXpathException;

    /**
     * Indicates that the caller is no longer interested in the specified expression.
     */
    void remove(String expression);

    /**
     * @return the indices corresponding to the xpath expressions that match namespace URIs for isSoap
     */
    int[] getSoapNamespaceUriIndices();

    /**
     * @return the 1-based Tarari index for the given expression, or -1 if it could not be compiled.
     */
    int getIndex(String expression);

    /**
     * Compiles the list of XPath expressions that have been gathered so far onto the Tarari card.
     */
    void compile();
}
