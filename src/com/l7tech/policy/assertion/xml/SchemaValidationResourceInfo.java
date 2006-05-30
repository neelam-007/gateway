/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xml;

import com.l7tech.policy.MessageUrlResourceInfo;

/**
 * Additional rules for attempting to retrieve XML schema documents over the network.
 * (see <a href="http://www.w3.org/TR/xmlschema-1/#schema-loc">http://www.w3.org/TR/xmlschema-1/#schema-loc</a>)
 * @author alex
 */
public class SchemaValidationResourceInfo extends MessageUrlResourceInfo {
    private boolean useSchemaLocation = true;
    private boolean useNamespaceUri = false;

    /**
     * <code>true</code> if the SSG should look for an xsl:schemaLocation attribute to indicate where
     * the XML schema(s) for a given namespace URI can be found; <code>false</code> otherwise.
     */
    public boolean isUseSchemaLocation() {
        return useSchemaLocation;
    }

    public void setUseSchemaLocation(boolean useSchemaLocation) {
        this.useSchemaLocation = useSchemaLocation;
    }

    /**
     * <code>true</code> if the SSG should try to do an HTTP GET on a namespace URI to try to find
     * an XML Schema for that namespace; <code>false</code> otherwise.
     */
    public boolean isUseNamespaceUri() {
        return useNamespaceUri;
    }

    public void setUseNamespaceUri(boolean useNamespaceUri) {
        this.useNamespaceUri = useNamespaceUri;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SchemaValidationResourceInfo that = (SchemaValidationResourceInfo) o;

        if (useNamespaceUri != that.useNamespaceUri) return false;
        if (useSchemaLocation != that.useSchemaLocation) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (useSchemaLocation ? 1 : 0);
        result = 31 * result + (useNamespaceUri ? 1 : 0);
        return result;
    }
}
