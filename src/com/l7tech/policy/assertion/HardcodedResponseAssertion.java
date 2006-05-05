package com.l7tech.policy.assertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.variable.ExpandVariables;

import java.io.IOException;

/**
 * A pseudo-routing assertion that returns a hardcoded response message, with a hardcoded
 * response status and content type.
 */
public class HardcodedResponseAssertion extends RoutingAssertion implements UsesVariables {
    private int responseStatus = 200;
    private String responseContentType = ContentTypeHeader.XML_DEFAULT.getFullValue();
    private String base64ResponseBody = "";

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

    public String responseBodyString() {
        try {
            return new String(HexUtils.decodeBase64(base64ResponseBody, true));
        } catch (IOException e) {
            return base64ResponseBody;
        }
    }

    public void responseBodyString(String responseBody) {
        setBase64ResponseBody(HexUtils.encodeBase64(responseBody.getBytes(), true));;
    }

    public String[] getVariablesUsed() {
        return ExpandVariables.getReferencedNames(responseBodyString() + responseContentType);
    }

    public String getBase64ResponseBody() {
        return base64ResponseBody;
    }

    public void setBase64ResponseBody(String body) {
        if (body == null) body = "";
        base64ResponseBody = body;
    }
}
