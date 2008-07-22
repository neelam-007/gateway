package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.RequiresSOAP;

/**
 * Asserts that a message being processed is identifiable as for a specific operation based on a service's WSDL.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 17, 2006<br/>
 */
@RequiresSOAP()
public class Operation extends Assertion {
    private String operationName;

    public Operation() {
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}
