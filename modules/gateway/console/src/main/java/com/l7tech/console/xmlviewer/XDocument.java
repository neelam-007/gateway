/*
 * $Id$
 *
 * The contents of this file are subject to the Mozilla Public License 
 * Version 1.1 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the License at 
 * http://www.mozilla.org/MPL/ 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License 
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is eXchaNGeR code. (org.xngr.*)
 *
 * The Initial Developer of the Original Code is Cladonia Ltd. Portions created 
 * by the Initial Developer are Copyright (C) 2002 the Initial Developer. 
 * All Rights Reserved. 
 *
 * Contributor(s): Edwin Dankert <edankert@cladonia.com>
 */
package com.l7tech.console.xmlviewer;

import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.net.URL;

/**
 * The representation of a document for the xngr application.
 * The implementation of this interface gives the service a reference
 * to a document within the system and allows the service to save and
 * get document specific parameters like the URL and the root element.
 * It also allows for a listener to be added to listen for changes in
 * the document.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public interface XDocument {

    /**
     * Gets an element from this document for the specified
     * XPath expression.
     * Returns Null, if the element could not be found.
     *
     * @param xpath the XPath expression to the element.
     * @return the element.
     * @throws java.lang.Exception throws an Exception when
     *                             the XML information in the document could not been parser,
     *                             this can be an IOException or SAXParseException.
     */
    public XElement getElement(String xpath) throws Exception;

    /**
     * Gets a list of elements from this document for the specified
     * XPath expression. Returns Null, if no element could be found.
     *
     * @param xpath the XPath expression to the element(s).
     * @return the element(s).
     * @throws java.lang.Exception throws an Exception when
     *                             the XML information in the document could not been parser,
     *                             this can be an IOException or SAXParseException.
     */
    public XElement[] getElements(String xpath) throws Exception;

    /**
     * Returns the root element for this document.
     *
     * @return the root element.
     * @throws java.lang.Exception throws an Exception when
     *                             the XML information in the document could not been parser,
     *                             this can be an IOException or SAXParseException.
     */
    public XElement getRoot() throws Exception;

    /**
     * Returns the URL for this document.
     * Can be used to get file specific information.
     *
     * @return the URL for this document.
     */
    public URL getURL();

    /**
     * Returns the name for this document as
     * set by the user.
     *
     * @return the name for this document.
     */
    public String getName();

    /**
     * Saves the document to disk and informs the listeners about
     * the update.
     *
     * @throws java.io.IOException if the document cannot be saved.
     */
    public void save() throws IOException;

    /**
     * Loads the document from disk and informs the listeners about
     * the update.
     *
     * @throws java.io.IOException           if the document cannot be found.
     * @throws org.xml.sax.SAXParseException if the information
     *                                       in the document cannot be parsed.
     */
    public void load() throws IOException, SAXParseException;

    /**
     * Find out wether the document in memory is different to the
     * document on disk.
     *
     * @return true when the document has been updated since
     *         the last save/load operation!
     */
    public boolean isUpdated();

    /**
     * Adds a document listener to the document.
     * The listener is invoked when the document has been updated
     * or deleted.
     *
     * @param listener the listener for changes in the document.
     */
    public void addListener(XDocumentListener listener);

    /**
     * Removes a document listener from the document.
     *
     * @param listener the listener for changes in the document.
     */
    public void removeListener(XDocumentListener listener);
} 
