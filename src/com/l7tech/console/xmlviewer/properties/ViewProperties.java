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

import java.awt.*;

/**
 * Handles the generic view properties.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class ViewProperties {
    private Element element = null;

    /**
     * Constructor for the view properties.
     *
     * @param element the element that contains the position
     *                and dimension of the view.
     */
    public ViewProperties(Element element) {
        super();

        this.element = element;
    }

    /**
     * Returns the dimension of the View.
     *
     * @return the dimension.
     */
    public Dimension getDimension() {
        Element dimension = element.element("dimension");
        int width = Integer.parseInt(dimension.element("width").getText());
        int height = Integer.parseInt(dimension.element("height").getText());

        return new Dimension(width, height);
    }

    /**
     * Sets the dimension of the View.
     *
     * @param d the dimension.
     */
    public void setDimension(Dimension d) {
        Element dimension = element.element("dimension");
        dimension.element("width").setText("" + d.width);
        dimension.element("height").setText("" + d.height);
    }

    /**
     * Gets the position of the View.
     *
     * @return the position.
     */
    public Point getPosition() {
        Element dimension = element.element("position");
        int x = Integer.parseInt(dimension.element("x").getText());
        int y = Integer.parseInt(dimension.element("y").getText());

        return new Point(x, y);
    }

    /**
     * Sets the position of the View.
     *
     * @param p the position.
     */
    public void setPosition(Point p) {
        Element dimension = element.element("position");
        dimension.element("x").setText("" + p.x);
        dimension.element("y").setText("" + p.y);
    }
} 
