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
 * Handles the properties for the Editor.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class EditorProperties extends ViewProperties {
    private static final int MAX_SEARCHES = 10;
    private static final int DEFAULT_SPACES = 4;

    private Element element = null;
    private Vector searches = null;

    /**
     * Constructor for the editor properties.
     *
     * @param element the element that contains the properties,
     *                for the editor.
     */
    public EditorProperties(Element element) {
        super(element);

        this.element = element;

        List list = element.elements("search");
        searches = new Vector();

        Iterator i = list.iterator();

        while (i.hasNext()) {
            Element e = (Element)i.next();
            searches.addElement(e.getText());
            element.remove(e);
        }
    }

    /**
     * Check to find out if the search matches the case.
     *
     * @return true when the search matches case.
     */
    public boolean isMatchCase() {
        Element elem = element.element("search-match-case");

        return "true".equals(elem.getText());
    }

    /**
     * Set the match-case search property.
     *
     * @param matchCase the search property.
     */
    public void setMatchCase(boolean matchCase) {
        Element elem = element.element("search-match-case");
        elem.setText("" + matchCase);
    }

    /**
     * Set the number of spaces to substitute for a tab.
     *
     * @param spaces the number of spaces.
     */
    public void setSpaces(int spaces) {
        Element elem = element.element("spaces");

        // ED: Remove for version 1.0
        if (elem != null) {
            elem.setText("" + spaces);
        } else {
            element.addElement("spaces").setText("" + spaces);
        }
    }

    /**
     * Gets the number of spaces to substitute for a tab.
     *
     * @return the number of spaces.
     */
    public int getSpaces() {
        int result = DEFAULT_SPACES;
        Element elem = element.element("spaces");

        // ED: Remove for version 1.0
        if (elem != null) {
            try {
                result = Integer.parseInt(elem.getText());
            } catch (Exception e) {
                result = DEFAULT_SPACES;
            }
        }

        return result;
    }

    /**
     * Check to find out if the tag completion is enabled.
     *
     * @return true when the tag completion is enabled.
     */
    public boolean isTagCompletion() {
        Element elem = element.element("tag-completion");

        // ED: Remove for version 1.0
        if (elem != null) {
            return "true".equals(elem.getText());
        } else {
            return true;
        }
    }

    /**
     * Set the tag completion property.
     *
     * @param complete the tag completion property.
     */
    public void setTagCompletion(boolean complete) {
        Element elem = element.element("tag-completion");

        // ED: Remove for version 1.0
        if (elem != null) {
            elem.setText("" + complete);
        } else {
            element.addElement("tag-completion").setText("" + complete);
        }
    }

    /**
     * Check to find out if the mixed content should be
     * indented when formatting.
     *
     * @return true when the mixed content should be indented.
     */
    public boolean indentMixedContent() {
        Element elem = element.element("indent-mixed-content");

        // ED: Remove for version 1.0
        if (elem != null) {
            return "true".equals(elem.getText());
        } else {
            return true;
        }
    }

    /**
     * Set wether the mixed content should be indented when formatting.
     *
     * @param indent true when the mixed content should be indented.
     */
    public void setIndentMixedContent(boolean indent) {
        Element elem = element.element("indent-mixed-content");

        // ED: Remove for version 1.0
        if (elem != null) {
            elem.setText("" + indent);
        } else {
            element.addElement("indent-mixed-content").setText("" + indent);
        }
    }

    /**
     * Check to find out if the search direction is down.
     *
     * @return true when the search direction is down.
     */
    public boolean isDirectionDown() {
        Element elem = element.element("search-direction-down");

        return "true".equals(elem.getText());
    }

    /**
     * Set the search direction.
     *
     * @param downward the search direction.
     */
    public void setDirectionDown(boolean downward) {
        Element elem = element.element("search-direction-down");
        elem.setText("" + downward);
    }

    /**
     * Adds a Search string to the Editor.
     *
     * @param search the search.
     */
    public void addSearch(String search) {
        if (search != null && search.length() > 0) {
            int index = -1;

            for (int i = 0; (i < searches.size()) && (index == -1); i++) {
                if (((String)searches.elementAt(i)).equals(search)) {
                    index = i;
                }
            }

            if (index != -1) {
                searches.removeElementAt(index);
            }

            searches.insertElementAt(search, 0);

            if (searches.size() > MAX_SEARCHES) {
                searches.removeElementAt(MAX_SEARCHES);
            }
        }
    }

    /**
     * Returns the list of searches.
     *
     * @return the list of searches.
     */
    public Vector getSearches() {
        return new Vector(searches);
    }

    /**
     * Updates the searches elements.
     */
    public void update() {
        for (int i = 0; i < searches.size(); i++) {
            element.addElement("search").setText((String)searches.elementAt(i));
        }
    }
} 
