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
 * The Original Code is eXchaNGeR code. (org.xngr.*)
 *
 * The Initial Developer of the Original Code is Cladonia Ltd. Portions created 
 * by the Initial Developer are Copyright (C) 2002 the Initial Developer. 
 * All Rights Reserved. 
 *
 * Contributor(s): Edwin Dankert <edankert@cladonia.com>
 */
package com.l7tech.console.xmlviewer;

import java.util.EventObject;

/**
 * The event that is fired to a document listener when the document
 * has been saved, changed or deleted.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class XDocumentEvent extends EventObject {
    private XElement element = null;

    /**
     * The constructor for the event.
     *
     * @param document the document that fired the event.
     * @param element  the element that is the root element for
     *                 all updated elements.
     */
    public XDocumentEvent(XDocument document, XElement element) {
        super(document);

        this.element = element;
    }

    /**
     * Returns the document that is responsible for firing this event.
     *
     * @return the document.
     */
    public XDocument getDocument() {
        return (XDocument)super.getSource();
    }

    /**
     * Returns the root element that identifies the changed elements
     * in the document.
     *
     * @return the root element for all the changed elements.
     */
    public XElement getElement() {
        return element;
    }
} 
