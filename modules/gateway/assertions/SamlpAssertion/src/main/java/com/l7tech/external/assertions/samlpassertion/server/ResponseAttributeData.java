package com.l7tech.external.assertions.samlpassertion.server;

import java.util.ArrayList;
import java.util.List;

/**
 * User: vchan
 */
public class ResponseAttributeData {

    private String name;
    private String namespaceOrFormat; // depending on saml version
    private String friendlyName;
    private List<Object> attributeValues;

    public ResponseAttributeData() {
        this.attributeValues = new ArrayList<Object>();
    }

    public ResponseAttributeData(String name, String namespaceOrFormat) {
        this(name, namespaceOrFormat, null);
    }

    public ResponseAttributeData(String name, String namespaceOrFormat, String friendlyName) {
        this();
        this.name = name;
        this.namespaceOrFormat = namespaceOrFormat;
        this.friendlyName = friendlyName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getName() {
        return name;
    }

    public String getNamespaceOrFormat() {
        return namespaceOrFormat;
    }

    public List<Object> getAttributeValues() {
        return attributeValues;
    }

    public Object[] getAttributeValuesAsArray() {
        Object[] valuesArr = new Object[attributeValues.size()];
        attributeValues.toArray(valuesArr);
        return valuesArr;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("ResponseAttributeData[");
        sb.append("name=").append(name).append(";");
        sb.append("namespaceOrFormat=").append(namespaceOrFormat).append(";");
        if (friendlyName != null)
            sb.append("friendlyName=").append(friendlyName).append(";");
        sb.append("values=");
        for (Object obj : attributeValues) {
            sb.append(obj.toString()).append(", ");
        }
        sb.append("]");

        return sb.toString();
    }
}
