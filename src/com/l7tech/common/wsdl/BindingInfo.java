package com.l7tech.common.wsdl;

import java.util.HashMap;
import java.io.Serializable;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class BindingInfo implements Serializable {

    protected String bindingName;
    protected HashMap bindingOperations = new HashMap();   // a list of BindingOperationInfo

    public BindingInfo(String bindingName, HashMap bindingOperations) {
        this.bindingName = bindingName;
        this.bindingOperations = bindingOperations;
    }

    public String getBindingName() {
        return bindingName;
    }

    public void setBindingName(String bindingName) {
        this.bindingName = bindingName;
    }

    public HashMap getBindingOperations() {
        return bindingOperations;
    }

    public void setBindingOperations(HashMap bindingOperations) {
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
