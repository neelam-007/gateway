/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.xml.WsTrustRequestType;
import com.l7tech.util.Functions;

/**
 * An assertion that sends the current request's credentials to a WS-Trust token service and replaces them with
 * the new credentials received from it.
 */
@ProcessesRequest
@RequiresSOAP(wss=true)
public class WsTrustCredentialExchange extends Assertion {
    public WsTrustCredentialExchange() {
    }

    public WsTrustCredentialExchange(String tokenServiceUrl, String appliesTo, WsTrustRequestType requestType) {
        this.tokenServiceUrl = tokenServiceUrl;
        this.appliesTo = appliesTo;
        this.requestType = requestType;
    }

    public boolean isCredentialModifier() {
        return true;
    }    

    public String getTokenServiceUrl() {
        return tokenServiceUrl;
    }

    public void setTokenServiceUrl(String tokenServiceUrl) {
        this.tokenServiceUrl = tokenServiceUrl;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public WsTrustRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(WsTrustRequestType requestType) {
        this.requestType = requestType;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});

        final String baseName = "Exchange Credentials using WS-Trust";
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "This assertion takes credentials gathered by a preceding credential source assertion and sends them via a WS-Trust RequestSecurityToken  SOAP request to a WS-Trust Security Token Service.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlWithCert16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Binary<String, WsTrustCredentialExchange, Boolean>() {
            @Override
            public String call(WsTrustCredentialExchange assertion, Boolean decorate) {
                if(!decorate) return baseName;

                return baseName + " Request to " + assertion.getTokenServiceUrl();
            }
        });

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.console.tree.policy.advice.AddWsTrustCredentialExchangeAdvice");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.EditWsTrustCredentialExchangeAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "WS-Trust Credential Exchange Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Edit16.gif");
        return meta;
    }

    private String tokenServiceUrl;
    private String appliesTo;
    private String issuer;
    private WsTrustRequestType requestType;

}
