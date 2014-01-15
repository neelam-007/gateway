package com.l7tech.skunkworks.rest.tools;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.util.Functions;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

/**
 * This is the rest response object, it encapsulates a response from the rest api.
 */
public class RestResponse implements Serializable {
    private String body;
    private int status;
    private Map<String, String[]> headers;
    private int assertionStatusId;

    public RestResponse(AssertionStatus assertionStatus, String body, int status, Map<String, String[]> headers) {
        this.assertionStatusId = assertionStatus.getNumeric();
        this.body = body;
        this.status = status;
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public int getStatus() {
        return status;
    }

    public String toString() {
        return "Status: " + status + " headers: " + printHeaders(headers) + " Body:\n" + body;
    }

    private String printHeaders(Map<String, String[]> headers) {
        return new StringBuilder(Functions.reduce(headers.entrySet(), "{", new Functions.Binary<String, String, Map.Entry<String, String[]>>() {
            @Override
            public String call(String s, Map.Entry<String, String[]> header) {
                return s + header.getKey() + "=" + Arrays.asList(header.getValue()).toString() + ", ";
            }
        })) + "}";
    }

    public AssertionStatus getAssertionStatus() {
        return AssertionStatus.fromInt(assertionStatusId);
    }

    public Map<String, String[]> getHeaders() {
        return headers;
    }
}