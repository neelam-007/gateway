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

package com.l7tech.console.xmlviewer.properties;

import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * The base class for the category properties.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class CategoryProperties {
    private Element element = null;
    private Vector categories = null;
    private Vector documents = null;

    /**
     * Constructor for the category properties.
     *
     * @param the element that contains the name of the category.
     */
    public CategoryProperties(Element element) {
        super();

        this.element = element;

        documents = new Vector();
        loadDocuments();

        categories = new Vector();
        loadCategories();
    }

    /**
     * Constructor for the category properties.
     *
     * @param name the name of the category.
     */
    public CategoryProperties(String name) {
        super();

        this.element = new DefaultElement("category");

        Element nameElement = new DefaultElement("name");
        nameElement.setText(name);

        element.add(nameElement);

        documents = new Vector();
        categories = new Vector();
    }

    private void loadCategories() {
        List list = element.elements("category");

        Iterator i = list.iterator();

        while (i.hasNext()) {
            categories.addElement(new CategoryProperties((Element)i.next()));
        }
    }

    private void loadDocuments() {
        List list = element.elements("document");

        Iterator i = list.iterator();

        while (i.hasNext()) {
            documents.addElement(new DocumentProperties((Element)i.next()));
        }
    }

    /**
     * Returns the name of the category.
     *
     * @return the name.
     */
    public String getName() {
        return element.element("name").getText();
    }

    /**
     * Sets the name of the category.
     *
     * @param the name.
     */
    public void setName(String name) {
        element.element("name").setText(name);
    }

    /**
     * Adds the category properties.
     *
     * @param properties the category properties.
     */
    public void addCategory(CategoryProperties properties) {
        element.add(properties.getElement());
        categories.add(properties);
    }

    /**
     * Removes a category from the parent property.
     *
     * @param properties the category properties.
     */
    public void removeCategory(CategoryProperties properties) {
        element.remove(properties.getElement());
        categories.remove(properties);
    }

    /**
     * Returns the properties for the child categories.
     *
     * @return a list of Category Properties.
     */
    public Vector getCategories() {
        return categories;
    }

    /**
     * Adds the document properties to the category.
     *
     * @param properties the document properties.
     */
    public void addDocument(DocumentProperties properties) {
        element.add(properties.getElement());
        documents.add(properties);
    }

    /**
     * Removes a document from the category.
     *
     * @param properties the document properties.
     */
    public void removeDocument(DocumentProperties properties) {
        element.remove(properties.getElement());
        documents.remove(properties);
    }

    /**
     * Returns the properties for the documents in
     * this category.
     *
     * @return a list of Document Properties.
     */
    public Vector getDocuments() {
        return documents;
    }

    /**
     * Returns the dom4j Element representation of this category.
     *
     * @return the element.
     */
    public Element getElement() {
        return element;
    }
} 
