package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.RequiresSOAP;

/**
 * Assertion for WSI-BSP compliance.
 *
 * <p>There are 2 aspects to this assertion:</p>
 *
 * <ul>
 *   <li>Trigger policy validation for checking selected algorithms.</li>
 *   <li>Perform runtime checks for compliance of incoming messages.</li>
 * </ul>
 *
 * <p>By default the runtime checks are disabled.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
@RequiresSOAP()
public class WsiBspAssertion extends Assertion {

    //- PUBLIC

    /**
     * Bean constructor
     */
    public WsiBspAssertion() {
        checkRequestMessages = false;
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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});

        meta.put(AssertionMetadata.SHORT_NAME, "Enforce WS-I BSP Compliance");
        meta.put(AssertionMetadata.DESCRIPTION, "Check the request or response for compliance with the WS-I Basic Security Profile 1.0 specification.");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/policy16.gif");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.WsiBspAssertionPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "WS-I BSP Compliance Properties");
        
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
