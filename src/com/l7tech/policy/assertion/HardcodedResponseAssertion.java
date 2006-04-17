package com.l7tech.policy.assertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.variable.ExpandVariables;

/**
 * A pseudo-routing assertion that returns a hardcoded response message, with a hardcoded
 * response status and content type.
 */
public class HardcodedResponseAssertion extends RoutingAssertion implements UsesVariables {
    private int responseStatus = 200;
    private String responseContentType = ContentTypeHeader.XML_DEFAULT.getFullValue();
    private String responseBody = null;

    public HardcodedResponseAssertion() {
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String[] getVariablesUsed() {
        return ExpandVariables.getReferencedNames(responseBody + responseContentType);
    }
}
