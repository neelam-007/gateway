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

/**
 * Represents an element-type in the system. An element-type is
 * defined by an element's (local)name and a namespace.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class XElementType {
    private String name = null;
    private String namespace = null;
    private String universalname = null;

    /**
     * Constructs an element-type for the given name and
     * namespace.
     *
     * @param localname the (local)name of the element-type.
     * @param namespace the namespace (URI) of the element-type.
     */
    public XElementType(String localname, String namespace) {
        this.name = localname;
        this.namespace = namespace;

        StringBuffer buffer = new StringBuffer();
        if (namespace != null && namespace.length() > 0) {
            buffer.append("{");
            buffer.append(namespace);
            buffer.append("}");
        }

        buffer.append(name);

        universalname = buffer.toString();
    }

    /**
     * Returns the namespace (URI) for this element-type.
     *
     * @return the namespace.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the (local) name for this element-type.
     *
     * @return the (local) name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the (universal) name for this element-type.
     * The name is in the form:
     * {namespace}localname
     *
     * @return the (universal) name.
     */
    public String getUniversalname() {
        return universalname;
    }

    /**
     * Checks wether this element-type is equal to
     * the object supplied.
     *
     * @param object the ElementType to check.
     * @return true when equal.
     */
    public boolean equals(Object object) {
        boolean result = false;

        if (object != null && object instanceof XElementType) {
            XElementType type = (XElementType)object;

            if (type == this) {
                result = true;
            } else {
                if (universalname == type.getUniversalname()) {
                    result = true;
                } else if (universalname != null && type.getUniversalname() != null) {
                    if (universalname.equals(type.getUniversalname())) {
                        result = true;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Overrides Object.hashCode(), because it should be
     * possible to use the XElementType as a key in a hashtable.
     *
     * @return the code.
     */
    public int hashCode() {
        return universalname.hashCode();
    }

    /**
     * Overrides Object.toString(), it returns the universal name.
     *
     * @return the universal name.
     */
    public String toString() {
        return universalname;
    }

}
