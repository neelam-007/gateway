package com.l7tech.policy.assertion;

import javax.wsdl.BindingOperation;
import javax.wsdl.Binding;
import java.util.HashMap;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertion extends SwAAssertion {
    private String bindingName;
    private HashMap bindingOperations;

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
}
