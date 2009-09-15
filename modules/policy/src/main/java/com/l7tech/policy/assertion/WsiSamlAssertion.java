package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.RequiresSOAP;

/**
 * Assertion for WSI-SAML Token Profile compliance.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
@RequiresSOAP()
public class WsiSamlAssertion extends Assertion {

    //- PUBLIC

    /**
     * Bean constructor
     */
    public WsiSamlAssertion() {
        checkRequestMessages = true;
        checkResponseMessages = false;
        auditRequestNonCompliance = true;
        auditResponseNonCompliance = true;
        failOnNonCompliantRequest = false;
        failOnNonCompliantResponse = false;
    }

    public boolean isCheckRequestMessages() {
        return checkRequestMessages;
    }

    public void setCheckRequestMessages(boolean checkRequestMessages) {
        this.checkRequestMessages = checkRequestMessages;
    }

    public boolean isCheckResponseMessages() {
        return checkResponseMessages;
    }

    public void setCheckResponseMessages(boolean checkResponseMessages) {
        this.checkResponseMessages = checkResponseMessages;
    }

    public boolean isAuditRequestNonCompliance() {
        return auditRequestNonCompliance;
    }

    public void setAuditRequestNonCompliance(boolean auditRequestNonCompliance) {
        this.auditRequestNonCompliance = auditRequestNonCompliance;
    }

    public boolean isAuditResponseNonCompliance() {
        return auditResponseNonCompliance;
    }

    public void setAuditResponseNonCompliance(boolean auditResponseNonCompliance) {
        this.auditResponseNonCompliance = auditResponseNonCompliance;
    }

    public boolean isFailOnNonCompliantRequest() {
        return failOnNonCompliantRequest;
    }

    public void setFailOnNonCompliantRequest(boolean failOnNonCompliantRequest) {
        this.failOnNonCompliantRequest = failOnNonCompliantRequest;
    }

    public boolean isFailOnNonCompliantResponse() {
        return failOnNonCompliantResponse;
    }

    public void setFailOnNonCompliantResponse(boolean failOnNonCompliantResponse) {
        this.failOnNonCompliantResponse = failOnNonCompliantResponse;
    }

    private final static String baseName = "Enforce SAML Compliance";
    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<WsiSamlAssertion>(){
        @Override
        public String getAssertionName( final WsiSamlAssertion assertion, final boolean decorate) {
            return baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String []{"xml"});
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Check the request or response for compliance with the WS-I SAML Token Profile 1.0 specification.");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/policy16.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.WsiSamlAssertionPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "WS-I SAML Compliance Properties");    


        return meta;
    }

    //- PRIVATE

    private boolean checkRequestMessages;
    private boolean checkResponseMessages;
    private boolean auditRequestNonCompliance;
    private boolean auditResponseNonCompliance;
    private boolean failOnNonCompliantRequest;
    private boolean failOnNonCompliantResponse;
}
