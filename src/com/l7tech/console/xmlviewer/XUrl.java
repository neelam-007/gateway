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

import java.net.URL;

/**
 * The representaion of a URL in the Xngr system. This url allows
 * to uniquely specify an element within the 'browser application'.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class XUrl {
    private URL url = null;
    private String xpath = null;

    /**
     * Constructs a Exchanger Url with the document-url and
     * element-xpath supplied.
     *
     * @param url   the URL for the document.
     * @param xpath the XPath expression for the element.
     */
    public XUrl(URL url, String xpath) {
        this.url = url;
        this.xpath = xpath;
    }

    /**
     * Returns the url to the document on disk.
     *
     * @return the url to the document.
     */
    public URL getURL() {
        return url;
    }

    /**
     * Returns the path to the element within a document.
     *
     * @return the path to the element.
     */
    public String getXPath() {
        return xpath;
    }

    /**
     * Checks wether this XUrl is equal to
     * the object supplied.
     *
     * @return true when equal.
     */
    public boolean equals(Object object) {
        boolean result = false;

        if (object instanceof XUrl) {
            XUrl xurl = (XUrl)object;

            if (xurl == this) {
                result = true;
            } else if (compare(xurl.getURL(), url) && compare(xurl.getXPath(), xpath)) {
                result = true;
            }
        }

        return result;
    }

    // Compares 2 objects, taking care of null pointer stuff...
    private boolean compare(Object object1, Object object2) {
        boolean result = false;

        if (object1 != null && object2 != null) {
            if (object1 == object2) {
                result = true;
            } else if (object1.equals(object2)) {
                result = true;
            }
        } else if (object1 == null && object2 == null) {
            result = true;
        }

        return result;
    }
} 
