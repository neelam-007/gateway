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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * The base class for the document properties.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class DocumentProperties {
    private Element element = null;
    private URL url = null;

    /**
     * Constructor for the document properties.
     *
     * @param element the element that contains the name and
     *                location of the document.
     */
    public DocumentProperties(Element element) {
        super();

        this.element = element;

        // ED: Remove for version 1.0
        if (element.element("validate") == null) {
            Element validateElement = new DefaultElement("validate");
            validateElement.setText("false");
            element.add(validateElement);
        }
    }

    /**
     * Constructor for the document properties.
     *
     * @param location the URL of the document.
     * @param validate validate when opening the document.
     */
    public DocumentProperties(URL location, boolean validate) {
        this(location, getFilename(location), validate);
    }

    /**
     * Constructor for the document properties.
     *
     * @param location the URL of the document.
     * @param name     the name of the document.
     * @param validate validate when opening the document.
     */
    public DocumentProperties(URL location, String name, boolean validate) {
        super();

        url = location;

        this.element = new DefaultElement("document");

        Element locationElement = new DefaultElement("location");
        locationElement.setText(location.toExternalForm());

        Element nameElement = new DefaultElement("name");
        nameElement.setText(name);

        Element validateElement = new DefaultElement("validate");
        validateElement.setText("" + validate);

        element.add(locationElement);
        element.add(nameElement);
        element.add(validateElement);
    }

    /**
     * Returns the URL of the document.
     *
     * @return the location.
     */
    public URL getURL() {
        if (url == null) {
            try {
                url = new URL(element.element("location").getText());
            } catch (MalformedURLException e) {
                // This should not be possible...
                e.printStackTrace();
            }
        }

        return url;
    }

    /**
     * Sets the URL of the document.
     *
     * @param url the document URL (The param should not be null!).
     */
    public void setURL(URL url) {
        this.url = url;
        element.element("location").setText(url.toExternalForm());
    }

    /**
     * Returns the location of the document.
     *
     * @return the location.
     */
    public String getLocation() {
        return element.element("location").getText();
    }

    /**
     * Returns the name of the document.
     *
     * @return the name.
     */
    public String getName() {
        return element.element("name").getText();
    }

    /**
     * Sets the name of the document.
     *
     * @param name 
     */
    public void setName(String name) {
        element.element("name").setText(name);
    }

    /**
     * Returns wether this document should be validated.
     *
     * @return true when the document should be validated.
     */
    public boolean validate() {
        return element.element("validate").getText().equals("true");
    }

    /**
     * Sets wether this document should be validated.
     *
     * @param enabled true when the document should be validated.
     */
    public void setValidate(boolean enabled) {
        element.element("validate").setText("" + enabled);
    }

    /**
     * Returns the dom4j Element representation of this document.
     *
     * @return the element.
     */
    public Element getElement() {
        return element;
    }

    // Gets the file part of the URL.
    private static String getFilename(URL url) {
        String file = url.toExternalForm();
        int index = file.lastIndexOf("/") + 1;

        return file.substring(index);
    }
} 
