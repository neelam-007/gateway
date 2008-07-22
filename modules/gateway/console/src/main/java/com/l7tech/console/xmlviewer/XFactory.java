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

import java.io.Reader;

/**
 * Factory methods that allow for document and element creation.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public interface XFactory {

    /**
     * Creates an empty element for the name supplied.
     *
     * @param name the name of the element.
     * @return the newly created element.
     */
    public XElement createElement(String name);

    /**
     * Creates an empty element for the element type supplied.
     *
     * @param type the type of element.
     * @return the newly created element.
     */
    public XElement createElement(XElementType type);

    /**
     * Creates an element tree for the character stream Reader
     * supplied.
     *
     * @param reader the reader for the XML information.
     * @return the newly created element tree.
     * @throws org.xml.sax.SAXParseException if the information
     *                                       in the <code>reader</code> cannot be parsed.
     */
    public XElement createElement(Reader reader) throws SAXParseException;

} 
