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
 * The Original Code is eXchaNGeR browser code. (org.xngr.browser.*)
 *
 * The Initial Developer of the Original Code is Cladonia Ltd.. Portions created 
 * by the Initial Developer are Copyright (C) 2002 the Initial Developer. 
 * All Rights Reserved. 
 *
 * Contributor(s): Edwin Dankert <edankert@cladonia.com>
 */
package com.l7tech.console.xmlviewer;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.tree.DefaultElement;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

/**
 * The default implementation of the XElement interface.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class ExchangerElement extends DefaultElement implements XElement {
    private XDocument document = null;
    private XElementType type = null;

    /**
     * Constructs a default element with an initial name.
     *
     * @param name the unmutable name.
     */
    public ExchangerElement(String name) {
        this(new QName(name));

        type = new XElementType(name, null);
    }

    /**
     * Constructs a default element with an initial type.
     *
     * @param name the unmutable name.
     */
    public ExchangerElement(XElementType type) {
        this(new QName(type.getName(), Namespace.get(type.getNamespace())));
        this.type = type;
    }

    /**
     * Constructs a default element with a dom4j element.
     *
     * @param the dom4j element.
     */
    public ExchangerElement(QName name) {
        super(name);
        type = new XElementType(name.getName(), name.getNamespaceURI());

        outputFormat.setIndent("  ");
        outputFormat.setNewlines(true);
    }

    /**
     * Adds an attribute to the list of attributes or
     * overwrites the attribute if the attribute name already
     * exists.
     *
     * @param name  the name of the attribute.
     * @param value the value of the attribute.
     */
    public void putAttribute(String name, String value) {
        super.addAttribute(name, value);

        if (document() != null) {
            ((ExchangerDocument)document()).fireUpdate();
        }
    }

    /**
     * Returns a 2 dimensional array of attributes.
     * where attribute[X][0] = name and
     * where attribute[X][1] = value.
     *
     * @return a 2 dimesnional array of Strings.
     */
    public String[][] getAttributes() {
        Attribute[] attributes = (Attribute[])super.attributes().toArray();
        String[][] result = new String[attributes.length][2];

        for (int i = 0; i < attributes.length; i++) {
            result[i][0] = attributes[i].getName();
            result[i][1] = attributes[i].getValue();
        }

        return result;
    }

    /**
     * Returns the value of the attribute with the name.
     *
     * @param name the name of the attribute.
     * @return the attribute value.
     */
    public String getAttribute(String name) {
        return super.attributeValue(name);
    }

    /**
     * Returns the first element for the given name.
     *
     * @param name the name of the element.
     * @return the element.
     */
    public XElement getElement(String name) {
        return (XElement)super.element(name);
    }

    /**
     * Returns all the child elements of this element.
     *
     * @return an array of elements.
     */
    public XElement[] getElements() {
        return convert(super.elements());
    }

    /**
     * Returns all the child elements with a given name
     * for this element.
     *
     * @param name the name of the element.
     * @return an array of elements.
     */
    public XElement[] getElements(String name) {
        return convert(super.elements(name));
    }

    /**
     * Returns the namespace for this element.
     *
     * @return a namespace representation.
     */
    public String namespace() {
        return super.getNamespaceURI();
    }

    /**
     * Returns the universal name for this element.
     * The name is in the form:
     * {namespace}localname
     *
     * @return a universal name representation.
     */
    public String getUniversalName() {
        return type.getUniversalname();
    }

    /**
     * Returns the (local)name for this element.
     *
     * @return a name for the element.
     */
    public String getName() {
        return super.getName();
    }

    /**
     * A check wether this element is the root element.
     *
     * @return true when the element is the root element.
     */
    public boolean isRoot() {
        return super.isRootElement();
    }

    /**
     * Returns the parent of this element.
     *
     * @return the parent element.
     */
    public XElement parent() {
        return (XElement)super.getParent();
    }

    /**
     * Gets the text value of this element.
     *
     * @return the value of this element.
     */
    public String getValue() {
        return super.getText();
    }

    /**
     * Sets the text value of this element.
     *
     * @param the value of this element.
     */
    public void setValue(String text) {
        super.setText(text);

        if (document() != null) {
            ((ExchangerDocument)document()).fireUpdate();
        }
    }

    /**
     * Adds a child element to this element.
     *
     * @param the child element.
     */
    public void add(XElement child) {
        super.add((Element)child);

        if (document() != null) {
            ((ExchangerDocument)document()).fireUpdate();
        }
    }

    /**
     * Removes a child element from this element.
     *
     * @param the child element.
     */
    public void remove(XElement child) {
        super.remove((Element)child);

        if (document() != null) {
            ((ExchangerDocument)document()).fireUpdate();
        }
    }

    /**
     * Returns an XPath result, uniquely identifying this element.
     *
     * @return the XPath result identifying this element.
     */
    public String path() {
        return super.getUniquePath();
    }

    /**
     * Sets the document for this element.
     *
     * @param the document.
     */
    public void document(XDocument document) {
        this.document = document;
    }

    /**
     * Search through the tree for the document.
     */
    private XDocument findDocument(ExchangerElement element) {
        if (element != null) {
            document = element.document();

            if (document == null) {
                document = findDocument((ExchangerElement)element.parent());
            }
        }

        return document;
    }

    /**
     * Returns the document for this element.
     * Will return null if the element does not
     * have a document.
     *
     * @return the document.
     */
    public XDocument document() {
        if (document == null) {
            document = findDocument((ExchangerElement)this.parent());
        }

        return document;
    }

    /**
     * Returns the type of this element.
     *
     * @return the type.
     */
    public XElementType getType() {
        return type;
    }

    /**
     * Returns the url of this element.
     *
     * @return the xurl.
     */
    public XUrl getXUrl() {
        XDocument doc = document();
        XUrl url = null;

        if (doc != null) {
            url = new XUrl(doc.getURL(), path());
        }

        return url;
    }

    // See super...
    public String asXML() {
        StringWriter writer = new StringWriter();

        try {
            write(writer);
        } catch (Exception e) {
            // Should never happen...
            e.printStackTrace();
        }

        return writer.toString();
    }

    /**
     * Returns the contents of this element as an XML
     * formatted String.
     *
     * @return the XML formatted String.
     */
    public String toString() {
        return asXML();
    }

    private XElement[] convert(List list) {
        XElement[] result = null;

        if (list != null && list.size() > 0) {
            Iterator elements = list.iterator();
            result = new XElement[list.size()];

            for (int i = 0; i < list.size(); i++) {
                result[i] = (XElement)elements.next();
            }
        }

        return result;
    }
} 
