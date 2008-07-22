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

    //- PRIVATE

    private boolean checkRequestMessages;
    private boolean checkResponseMessages;
    private boolean auditRequestNonCompliance;
    private boolean auditResponseNonCompliance;
    private boolean failOnNonCompliantRequest;
    private boolean failOnNonCompliantResponse;
}
