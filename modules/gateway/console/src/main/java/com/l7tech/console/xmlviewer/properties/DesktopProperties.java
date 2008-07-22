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

/**
 * Handles the properties for the Desktop.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class DesktopProperties extends ViewProperties {
    private Element element = null;

    /**
     * Constructor for the desktop properties.
     *
     * @param element the element that contains properties for the
     *                desktop.
     */
    public DesktopProperties(Element element) {
        super(element);

        this.element = element;
    }
} 
