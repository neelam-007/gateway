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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The class that can be used by the service to set properties.
 * <p/>
 * A fixed set of properties are already set by the eXchaNGeR
 * application, <br/>
 * <br/>
 * <code>"service.home"</code>, the home/installation directory of the service.<br/>
 * <br/>
 * The following properties are the same as defined in the service.xml file.<br/>
 * <br/>
 * <code>"service.version"</code>, the version of the service.<br/>
 * <code>"service.author"</code>, the author of the service.<br/>
 * <code>"service.copyright"</code>, the copyright information for the service.<br/>
 * <code>"service.description"</code>, the service description.<br/>
 * <code>"service.reference"</code>, the service reference, normally a URL.<br/>
 * <code>"service.title"</code>, the service title.<br/>
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class XProperties {
    private Hashtable properties = null;

    /**
     * Creates an empty properties list.
     */
    public XProperties() {
        properties = new Hashtable();
    }

    /**
     * Creates a properties list with initial property values.
     * <p/>
     * Where properties[X][0] is the key and
     * properties[X][1] is the value for the key.
     *
     * @param properties a list of properties.
     */
    public XProperties(String[][] properties) {
        setProperties(properties);
    }

    /**
     * Gets a property for a specific key.
     *
     * @param key the key for the property.
     * @return the value of the property, will return null
     *         if the key cannot be found
     */
    public String get(String key) {
        return (String)properties.get(key);
    }

    /**
     * Sets a property for a specific key.
     *
     * @param key   the key for the property.
     * @param value the value of the property.
     */
    public void put(String key, String value) {
        properties.put(key, value);
    }

    /**
     * Sets all the properties as an array of key,
     * value pairs.
     * <p/>
     * Where properties[X][0] is the key and
     * properties[X][1] is the value for the key.
     *
     * @param props the array of key value pairs.
     */
    public void setProperties(String[][] props) {
        this.properties = new Hashtable();

        if (props != null) {
            for (int i = 0; i < props.length; i++) {
                properties.put(props[i][0], props[i][1]);
            }
        }
    }

    /**
     * Returns all the properties as an array of
     * key value pairs.
     * where properties[X][0] is the key and
     * properties[X][1] is the value for the key.
     *
     * @return an array of key value pairs.
     */
    public String[][] getProperties() {
        Set set = properties.entrySet();
        String result[][] = new String[set.size()][2];

        Iterator props = set.iterator();
        int index = 0;

        while (props.hasNext()) {
            Entry entry = (Entry)props.next();
            result[index][0] = (String)entry.getKey();
            result[index][1] = (String)entry.getValue();
            index++;
        }

        return result;
    }
} 
