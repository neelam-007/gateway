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

/**
 * The base class for the element-type properties.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class ElementTypeProperties {
    private Element element = null;

    /**
     * Constructor for the element-type properties.
     *
     * @param the dom4j element that contains the properties.
     */
    public ElementTypeProperties(Element element) {
        super();

        this.element = element;
    }

    /**
     * Constructor for the service element.
     *
     * @param isDefault indicates wether the associated service
     *                  is the default service.
     * @param the       (local)name for the element-type.
     * @param the       namespace (URI) for the element-type.
     */
    public ElementTypeProperties(boolean isdefault, String localname, String namespace) {
        super();

        this.element = new DefaultElement("element-type");
        element.addAttribute("default", "" + isdefault);

        element.add(createElement("localname", localname));
        element.add(createElement("namespace", namespace));
    }

    /**
     * Returns wether the association is the default association.
     *
     * @return true when a default association.
     */
    public boolean isDefault() {
        boolean isdefault = false;

        if (element.attribute("default").getValue().equals(Boolean.TRUE.toString())) {
            isdefault = true;
        }

        return isdefault;
    }

    /**
     * Sets (resets) wether the association is the default association.
     *
     * @param enabled true when the association is the default association.
     */
    public void setDefault(boolean enabled) {
        String attribute = Boolean.TRUE.toString();

        if (!enabled) {
            attribute = Boolean.FALSE.toString();
        }

        element.attribute("default").setValue(attribute);
    }

    /**
     * Gets the (local)name of the element-type.
     *
     * @retrun the (local)name of the element-type.
     */
    public String getLocalname() {
        return element.element("localname").getText();
    }

    /**
     * Gets the namespace (URI) of the element-type.
     *
     * @retrun the namespace (URI) of the element-type.
     */
    public String getNamespace() {
        return element.element("namespace").getText();
    }

    /**
     * Gets the dom4j element.
     *
     * @retrun the element.
     */
    public Element getElement() {
        return element;
    }

    private static final Element createElement(String name, String value) {
        Element element = new DefaultElement(name);
        element.setText(value);
        return element;
    }
} 
