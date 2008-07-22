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

    //- PRIVATE

    private boolean checkRequestMessages;
    private boolean checkResponseMessages;
    private boolean auditRequestNonCompliance;
    private boolean auditResponseNonCompliance;
    private boolean failOnNonCompliantRequest;
    private boolean failOnNonCompliantResponse;
}
