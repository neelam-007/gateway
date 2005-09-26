/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.xml.tarari;

import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.objectmodel.FindException;

import java.io.IOException;

import org.springframework.beans.factory.BeanFactory;
import org.apache.xmlbeans.XmlException;

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
     * this makes a list of all community schema in the table as well as all the schemas defined in
     * policies and makes sure the schemas loaded on the tarari card are the same. this should typically
     * be called whenever a published service is updated or saved
     */
    public void updateSchemasToCard(BeanFactory managerResolver) throws FindException, IOException, XmlException;

    /**
     * returns the number of schemas loaded on the card that refers to a targetnamespace
     */
    public int targetNamespaceLoadedMoreThanOnce(String targetNamespace);

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
