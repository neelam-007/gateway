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
 * Handles the properties for a Service.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class ServiceProperties {
    private Element element = null;
    private Vector elementTypes = null;

    /**
     * Constructor for the service properties.
     *
     * @param the element that contains the name and location children.
     */
    public ServiceProperties(Element element) {
        super();

        this.element = element;

        List list = element.elements("element-type");
        elementTypes = new Vector();

        Iterator i = list.iterator();

        while (i.hasNext()) {
            elementTypes.addElement(new ElementTypeProperties((Element)i.next()));
        }
    }

    /**
     * Constructor for the service element.
     *
     * @param the name element of the service.
     * @param the location element of the service.
     */
    public ServiceProperties(String name, String description, String iconPath, String location, String argument) {
        super();

        elementTypes = new Vector();

        this.element = new DefaultElement("service");

        Element nameElement = new DefaultElement("name");
        nameElement.setText(name);

        Element descriptionElement = new DefaultElement("description");
        descriptionElement.setText(description);

        Element iconElement = new DefaultElement("icon");
        iconElement.setText(iconPath);

        Element locationElement = new DefaultElement("location");
        locationElement.setText(location);

        Element argumentElement = new DefaultElement("argument");
        Element propertiesElement = new DefaultElement("properties");
        argumentElement.setText(argument);

        element.add(nameElement);
        element.add(descriptionElement);
        element.add(iconElement);
        element.add(argumentElement);
        element.add(locationElement);
        element.add(propertiesElement);
    }

    /**
     * Returns the name of the service.
     *
     * @return the name.
     */
    public String getName() {
        return element.element("name").getText();
    }

    /**
     * Changes the name of the service.
     *
     * @param name the name of the service.
     */
    public void setName(String name) {
        element.element("name").setText(name);
    }

    /**
     * Returns the description of the service.
     *
     * @return the description.
     */
    public String getDescription() {
        return element.element("description").getText();
    }

    /**
     * Changes the description of the service.
     *
     * @param description the description of the service.
     */
    public void setDescription(String description) {
        element.element("description").setText(description);
    }

    /**
     * Returns the argument of the service.
     *
     * @return the argument.
     */
    public String getArgument() {
        return element.element("argument").getText();
    }

    /**
     * Changes the argument for the service.
     *
     * @param argument the argument for the service.
     */
    public void setArgument(String argument) {
        element.element("argument").setText(argument);
    }

    /**
     * Returns the location of the service.
     *
     * @return the location.
     */
    public String getLocation() {
        return element.element("location").getText();
    }

    /**
     * Returns the location of the icon.
     *
     * @return the icon location.
     */
    public String getIconLocation() {
        return element.element("icon").getText();
    }

    /**
     * Sets the location of the icon.
     *
     * @param location the icon location.
     */
    public void setIconLocation(String location) {
        element.element("icon").setText(location);
    }

    /**
     * Sets the properties for this service.
     * String[X][0] = name
     * String[X][1] = value
     *
     * @param strings the properties.
     */
    public void setProperties(String[][] strings) {
        Element properties = element.element("properties");
        properties.clearContent();

        if (strings != null) {
            for (int i = 0; i < strings.length; i++) {
                Element prop = new DefaultElement(strings[i][0]);
                prop.setText(strings[i][1]);
                properties.add(prop);
            }
        }
    }

    /**
     * Gets the properties for this service.
     * String[X][0] = name
     * String[X][1] = value
     *
     * @return the properties.
     */
    public String[][] getProperties() {
        String[][] strings = null;
        Element properties = element.element("properties");
        List list = properties.elements();

        if (list.size() != 0) {
            strings = new String[list.size()][2];
            Iterator props = list.iterator();

            for (int i = 0; i < list.size(); i++) {
                Element prop = (Element)props.next();
                strings[i][0] = prop.getName();
                strings[i][1] = prop.getText();
            }
        }

        return strings;
    }

    /**
     * Returns the Element representation of this services element.
     *
     * @return the element.
     */
    public Element getElement() {
        return element;
    }

    /**
     * Returns a vector of associations.
     *
     * @return the list.
     */
    public Vector getElementTypes() {
        return elementTypes;
    }

    /**
     * Adds an association to the service.
     *
     * @param isDefault wether this is the default association.
     * @param localname the (local) name of the element-type.
     * @param namespace the namespace (URI) of the element-type.
     * @return the newly created element type properties.
     */
    public ElementTypeProperties addElementType(boolean isdefault, String localname, String namespace) {
        ElementTypeProperties elementType = new ElementTypeProperties(isdefault, localname, namespace);
        element.add(elementType.getElement());
        elementTypes.add(elementType);

        return elementType;
    }

    /**
     * Removes an association from the service.
     *
     * @param elementType the associated element.
     */
    public void removeElementType(ElementTypeProperties elementType) {
        element.remove(elementType.getElement());
        elementTypes.remove(elementType);
    }
} 
