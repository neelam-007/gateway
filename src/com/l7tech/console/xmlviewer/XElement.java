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


/**
 * The interface that represents an XML element in the eXchaNGeR
 * application. Use the factory method <code>creatElement()</code> to
 * create a new element.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public interface XElement {
    /**
     * Adds an attribute to the list of attributes or
     * overwrites the attribute if the attribute name already
     * exists.
     *
     * @param name  the name of the attribute.
     * @param value the value of the attribute.
     */
    public void putAttribute(String name, String value);

    /**
     * Returns a 2 dimensional array of attributes.
     * where attribute[X][0] = name and
     * where attribute[X][1] = value.
     *
     * @return a 2 dimesnional array of Strings.
     */
    public String[][] getAttributes();

    /**
     * Returns the value of the attribute with the name.
     *
     * @param name the name of the attribute.
     * @return the attribute value.
     */
    public String getAttribute(String name);

    /**
     * Returns the first element for the given name.
     *
     * @param name the name of the element.
     * @return the element.
     */
    public XElement getElement(String name);

    /**
     * Returns all the child elements of this element.
     *
     * @return a list of elements.
     */
    public XElement[] getElements();

    /**
     * Returns all the child elements with a given name
     * for this element.
     *
     * @param name the name of the element.
     * @return a list of elements.
     */
    public XElement[] getElements(String name);

    /**
     * Returns the namespace for this element.
     *
     * @return a namespace representation.
     */
    public String namespace();

    /**
     * Returns the universal name for this element.
     * The name is in the form:
     * {namespace}localname
     *
     * @return a universal name representation.
     */
    public String getUniversalName();

    /**
     * Returns the (local)name for this element.
     *
     * @return a name for the element.
     */
    public String getName();

    /**
     * A check wether this element is the root element.
     *
     * @return true when the element is the root element.
     */
    public boolean isRoot();

    /**
     * Returns the parent of this element.
     *
     * @return the parent element.
     */
    public XElement parent();

    /**
     * Gets the text value of this element.
     *
     * @return the value of this element.
     */
    public String getValue();

    /**
     * Sets the text value of this element.
     *
     * @param value the value of this element.
     */
    public void setValue(String value);

    /**
     * Adds a child element to this element.
     *
     * @param child the child element.
     */
    public void add(XElement child);

    /**
     * Removes a child element from this element.
     *
     * @param child the child element.
     */
    public void remove(XElement child);

    /**
     * Returns an XPath result, uniquely identifying this element.
     *
     * @return the XPath result identifying this element.
     */
    public String path();

    /**
     * Returns the document for this element.
     * Null if this element does not have a document associated.
     *
     * @return the document.
     */
    public XDocument document();

    /**
     * Returns the type of this element.
     *
     * @return the type.
     */
    public XElementType getType();

    /**
     * Returns the eXchaNGeR url of this element,
     * This url consists of the document URL and the
     * unique XPath expression to this element.
     * This method returns null if the element does
     * not have an associated document.
     *
     * @return the xurl.
     */
    public XUrl getXUrl();
} 
