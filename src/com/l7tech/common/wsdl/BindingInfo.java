package com.l7tech.common.wsdl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class BindingInfo implements Serializable {

    protected String bindingName = "";
    protected Map bindingOperations = new HashMap();   // a list of BindingOperationInfo

    public BindingInfo() {
    }

    public BindingInfo(String bindingName, Map bindingOperations) {
        this.bindingName = bindingName;
        this.bindingOperations = bindingOperations;
    }

    /** @return the Binding name.  Never null. */
    public String getBindingName() {
        return bindingName;
    }

    /** @param bindingName the new binding name.  May not be null. */
    public void setBindingName(String bindingName) {
        if (bindingName == null)
            throw new IllegalArgumentException("binding name may not be null");
        this.bindingName = bindingName;
    }

    /** @return the binding operations map.  Never null. */
    public Map getBindingOperations() {
        return bindingOperations;
    }

    /** @param bindingOperations the new binding operations map.  May not be null. */
    public void setBindingOperations(Map bindingOperations) {
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
}
