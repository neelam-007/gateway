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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Handles the properties for the Viewer.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class ViewerProperties extends ViewProperties {
    private static final int MAX_XPATHS = 10;

    private Element element = null;

    private Vector xpaths = null;

    /**
     * Constructor for the viewer properties.
     *
     * @param element the element that contains the properties,
     *                for the viewer.
     */
    public ViewerProperties(Element element) {
        super(element);

        this.element = element;

        List list = element.elements("xpath");
        xpaths = new Vector();

        Iterator i = list.iterator();

        while (i.hasNext()) {
            Element e = (Element)i.next();
            xpaths.addElement(e.getText());
            element.remove(e);
        }
    }

    /**
     * Check to find out if the namespaces should be visible.
     *
     * @return true when the namespaces should be visible.
     */
    public boolean isShowNamespaces() {
        Element elem = element.element("show-namespaces");

        return "true".equals(elem.getText());
    }

    /**
     * Set the namespaces (in)visible.
     *
     * @param visible true when the namespaces should be visible.
     */
    public void showNamespaces(boolean visible) {
        Element elem = element.element("show-namespaces");
        elem.setText("" + visible);
    }

    /**
     * Check to find out if the attributes should be visible.
     *
     * @return true when the attributes should be visible.
     */
    public boolean isShowAttributes() {
        Element elem = element.element("show-attributes");

        return "true".equals(elem.getText());
    }

    /**
     * Set the attributes (in)visible.
     *
     * @param visible true when the attributes should be visible.
     */
    public void showAttributes(boolean visible) {
        Element elem = element.element("show-attributes");
        elem.setText("" + visible);
    }

    /**
     * Check to find out if the element=values should be visible.
     *
     * @return true when the element-values should be visible.
     */
    public boolean isShowValues() {
        Element elem = element.element("show-values");

        return "true".equals(elem.getText());
    }

    /**
     * Set the element-values (in)visible.
     *
     * @param visible true when the values should be visible.
     */
    public void showValues(boolean visible) {
        Element elem = element.element("show-values");
        elem.setText("" + visible);
    }

    /**
     * Check to find out if the comments should be visible.
     *
     * @return true when the comments should be visible.
     */
    public boolean isShowComments() {
        Element elem = element.element("show-comments");

        // ED: remove for version 1.0
        if (elem == null) {
            element.addElement("show-comments").setText("" + true);
            elem = element.element("show-comments");
        }

        return "true".equals(elem.getText());
    }

    /**
     * Set the comments (in)visible.
     *
     * @param visible true when the comments should be visible.
     */
    public void showComments(boolean visible) {
        Element elem = element.element("show-comments");

        // ED: remove for version 1.0
        if (elem == null) {
            element.addElement("show-comments").setText("" + visible);
        } else {
            elem.setText("" + visible);
        }
    }


    /**
     * Adds a XPath string to the viewer.
     *
     * @param xpath the xpath string.
     */
    public void addXPath(String xpath) {
        if (xpath != null && xpath.length() > 0) {
            int index = -1;

            for (int i = 0; (i < xpaths.size()) && (index == -1); i++) {
                if (((String)xpaths.elementAt(i)).equals(xpath)) {
                    index = i;
                }
            }

            if (index != -1) {
                xpaths.removeElementAt(index);
            }

            xpaths.insertElementAt(xpath, 0);

            if (xpaths.size() > MAX_XPATHS) {
                xpaths.removeElementAt(MAX_XPATHS);
            }
        }
    }

    /**
     * Returns the list of xpaths.
     *
     * @return the list of xpaths.
     */
    public Vector getXPaths() {
        return new Vector(xpaths);
    }

    /**
     * Updates the xpaths elements.
     */
    public void update() {
        for (int i = 0; i < xpaths.size(); i++) {
            element.addElement("xpath").setText((String)xpaths.elementAt(i));
        }
    }
} 
