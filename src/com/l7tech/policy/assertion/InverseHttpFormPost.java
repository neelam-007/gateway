package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.ProcessesMultipart;

/**
 * Extracts MIME parts from the current request and transforms them into fields from an HTML form submission.
 * <p>
 * <b>NOTE</b>: This assertion destroys the current request and replaces it
 * with new content!
 */
@ProcessesMultipart
@ProcessesRequest
public class InverseHttpFormPost extends Assertion {
    private String[] fieldNames = new String[0];

    public String[] getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(String[] fieldNames) {
        this.fieldNames = fieldNames;
    }
}
