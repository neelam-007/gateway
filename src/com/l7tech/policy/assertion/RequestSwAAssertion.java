package com.l7tech.policy.assertion;

import com.l7tech.common.wsdl.BindingInfo;

import java.util.Map;
import java.util.HashMap;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertion extends SwAAssertion {
    private Map bindings = new HashMap();

    public RequestSwAAssertion() {
    }

    public RequestSwAAssertion(Map bindings) {
        this.bindings = bindings;
    }

    /** @return the Bindings map.  Never null. */
    public Map getBindings() {
        return bindings;
    }

    /** @param bindings the new Binding info.  May not be null. */
    public void setBindings(Map bindings) {
        if (bindings == null)
            throw new IllegalArgumentException("bindings map may not be null");
        this.bindings = bindings;
    }

    public boolean hasMimeParts(String operationName) {

        return true;
    }
}
