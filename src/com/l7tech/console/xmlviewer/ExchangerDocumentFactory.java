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
package com.l7tech.console.xmlviewer;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.QName;

/**
 * Makes sure the ExchangerElement is created instead of the
 * org.dom4j.Element
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class ExchangerDocumentFactory extends DocumentFactory {
    private static transient DocumentFactory singleton = new ExchangerDocumentFactory();

    /**
     * Access to singleton implementation of DocumentFactory which
     * is used if no DocumentFactory is specified when building using the
     * standard builders.
     *
     * @return the default singleon instance
     */
    public static DocumentFactory getInstance() {
        return singleton;
    }

    // The public constructor for the factory
    public ExchangerDocumentFactory() {
        super();
    }

    /**
     * Creates the ExchangerElement.
     *
     * @param qname, the name of the element.
     * @return the Exchanger Element.
     */
    public Element createElement(QName qname) {
        return new ExchangerElement(qname);
    }

//  Entities are only created when the entity value can be found in the 
//  lookup table!
//  public Entity createEntity( String name, String text) {
//		System.out.println( "createEntity( "+name+", "+text+")");
//		return super.createEntity( name, text);
//  }
} 
