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
 * Handles the properties for the Explorer.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class ExplorerProperties extends ViewProperties {
    private Element element = null;

    /**
     * Constructor for the explorer properties.
     *
     * @param element the element that contains the properties
     *                for the explorer.
     */
    public ExplorerProperties(Element element) {
        super(element);

        this.element = element;
    }

    public int getDividerLocation() {
        return Integer.parseInt(element.element("divider-location").getText());
    }

    public void setDividerLocation(int pos) {
        element.element("divider-location").setText("" + pos);
    }

    public Font getFont() {
        Element elem = element.element("font-name");

        if (elem == null) {
//			System.out.println( "ExplorerProperties.getFont() [default]");
            return getDefaultFont();
        }

        String name = elem.getText();
		
//		System.out.println( "ExplorerProperties.getFont() ["+name+"]");
        int style = Integer.parseInt(element.element("font-style").getText());
        int size = Integer.parseInt(element.element("font-size").getText());

        return new Font(name, style, size);
    }

    public void setFont(Font font) {
        Element elem = element.element("font-name");

        if (elem == null) {
            element.addElement("font-name").setText("" + font.getName());
        } else {
            elem.setText("" + font.getName());
        }

        elem = element.element("font-size");

        if (elem == null) {
            element.addElement("font-size").setText("" + font.getSize());
        } else {
            elem.setText("" + font.getSize());
        }

        elem = element.element("font-style");

        if (elem == null) {
            element.addElement("font-style").setText("" + font.getStyle());
        } else {
            elem.setText("" + font.getStyle());
        }
    }

    /**
     * Get the default font.
     *
     * @return the default font.
     */
    public static Font getDefaultFont() {
        Font defaultFont = null;

        if (System.getProperty("mrj.version") != null) {
            defaultFont = new Font("Courier", Font.PLAIN, 12);
        } else {
            defaultFont = new Font("monospaced", Font.PLAIN, 12);
        }

        return defaultFont;
    }
} 
