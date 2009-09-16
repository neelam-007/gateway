/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.FaultLevel;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.xml.soap.SoapVersion;

/**
 * Node in the policy tree that represents the {@link FaultLevel} assertion
 */
public class FaultLevelTreeNode extends DefaultAssertionPolicyNode<FaultLevel> {
    public FaultLevelTreeNode(FaultLevel assertion) {
        super(assertion);
    }


    @Override
    public void serviceChanged(PublishedService service) {
        if ( service != null ){
            assertion.setSoap12(service.getSoapVersion() == SoapVersion.SOAP_1_2);
        }
    }
}
