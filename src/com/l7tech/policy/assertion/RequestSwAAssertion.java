package com.l7tech.policy.assertion;

import com.l7tech.common.wsdl.BindingInfo;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertion extends SwAAssertion {
    private BindingInfo bindingInfo = new BindingInfo();

    public RequestSwAAssertion() {
    }

    public RequestSwAAssertion(BindingInfo bindingInfo) {
        this.bindingInfo = bindingInfo;
    }

    /** @return the BindingInfo.  Never null. */
    public BindingInfo getBindingInfo() {
        return bindingInfo;
    }

    /** @param bindingInfo the new Binding info.  May not be null. */
    public void setBindingInfo(BindingInfo bindingInfo) {
        if (bindingInfo == null)
            throw new IllegalArgumentException("bindingInfo may not be null");
        this.bindingInfo = bindingInfo;
    }
}
