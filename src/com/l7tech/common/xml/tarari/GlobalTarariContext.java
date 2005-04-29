/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.InvalidSchemaException;
import com.l7tech.common.xml.InvalidXpathException;

import java.io.IOException;

/**
 * Implementations manage the server-wide state of the RAX API
 */
public interface GlobalTarariContext {
    int NO_SUCH_EXPRESSION = -1;

    /**
     * Adds an XPath expression to the context.
     * <p>
     * If this expression is valid, after {@link #compile} is called, subsequent calls to {@link #getXpathIndex}
     * will return a positive result.
     * @param expression the XPath expression to add to the context.
     */
    void addXpath(String expression) throws InvalidXpathException;

    /**
     * Indicates that the caller is no longer interested in the specified expression.
     */
    void removeXpath(String expression);

    /**
     * Adds a new XML Schema to the context.
     *
     * @param nsUri the Namespace URI for the schema
     * @param schemaDoc the contents of the schema document
     * @throws IOException
     */
    void addSchema(String nsUri, String schemaDoc) throws IOException, InvalidSchemaException;

    /**
     * Indicates that the caller is no longer interested in the schema with the specified Namespace URI.
     * @param nsUri the Namespace URI of the schema.
     */
    void removeSchema(String nsUri);

    /**
     * @return the indices corresponding to the xpath expressions that match namespace URIs for isSoap
     */
    int[] getSoapNamespaceUriIndices();

    /**
     * @param expression the expression to look for.
     * @param targetCompilerGeneration the GlobalTarariContext compiler generation count that was in effect when your
     *                                 {@link TarariMessageContext} was produced.  This is a mandatory parameter.
     *                                 See {@link TarariMessageContext#getCompilerGeneration}.                                 
     * @return the 1-based Tarari index for the given expression, or a number less than 1
     *         if the given expression was not included in the specified compiler generation count.
     */
    int getXpathIndex(String expression, long targetCompilerGeneration);

    /**
     * Compiles the list of XPath expressions that have been gathered so far onto the Tarari card.
     */
    void compile();

    /**
     * Get the compiler generation count of the most recently installed set of xpaths.  This is used to check whether
     * a particular xpath was present in the hardware when a particular TarariMessageContext was created.
     *
     * @return the compiler generation count of the most recently installed set of xpaths.
     */
    long getCompilerGeneration();
}
