package com.l7tech.wsdl;

import com.l7tech.util.XmlSafe;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
@XmlSafe
public class BindingInfo implements Cloneable, Serializable {

    protected String bindingName = "";
    protected Map<String,BindingOperationInfo> bindingOperations = new LinkedHashMap();   // Map of operation names (String) to BindingOperationInfos

    @XmlSafe
    public BindingInfo() {
    }

    @XmlSafe
    public BindingInfo(String bindingName, Map<String,BindingOperationInfo> bindingOperations) {
        this.bindingName = bindingName;
        this.bindingOperations.putAll(bindingOperations);
    }

    /** @return the Binding name.  Never null. */
    @XmlSafe
    public String getBindingName() {
        return bindingName;
    }

    /** @param bindingName the new binding name.  May not be null. */
    @XmlSafe
    public void setBindingName(String bindingName) {
        if (bindingName == null)
            throw new IllegalArgumentException("binding name may not be null");
        this.bindingName = bindingName;
    }

    /** @return the binding operations map.  Never null. */
    @XmlSafe
    public Map<String,BindingOperationInfo> getBindingOperations() {
        return bindingOperations;
    }

    /** @param bindingOperations the new binding operations map.  May not be null. */
    @XmlSafe
    public void setBindingOperations(Map<String,BindingOperationInfo> bindingOperations) {
        if (bindingOperations == null)
            throw new IllegalArgumentException("bindingOperations may not be null.");
        this.bindingOperations = bindingOperations;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BindingInfo)) return false;

        final BindingInfo bindingInfo = (BindingInfo) o;

        if (bindingName != null ? !bindingName.equals(bindingInfo.bindingName) : bindingInfo.bindingName != null) return false;
        if (bindingOperations != null ? !bindingOperations.equals(bindingInfo.bindingOperations) : bindingInfo.bindingOperations != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (bindingName != null ? bindingName.hashCode() : 0);
        result = 29 * result + (bindingOperations != null ? bindingOperations.hashCode() : 0);
        return result;
    }

    public Object clone() {
        try {
            BindingInfo clone = (BindingInfo) super.clone();
            clone.bindingOperations = new LinkedHashMap(bindingOperations);

            for( Map.Entry<String,BindingOperationInfo> entry : clone.bindingOperations.entrySet() ) {
                entry.setValue((BindingOperationInfo)entry.getValue().clone());
            }
            return clone;
        }
        catch(CloneNotSupportedException cnse) {
            throw new RuntimeException("Clone not supported", cnse);
        }
    }
}
