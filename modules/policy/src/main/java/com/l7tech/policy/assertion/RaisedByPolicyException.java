package com.l7tech.policy.assertion;


public class RaisedByPolicyException extends PolicyAssertionException  {

    public RaisedByPolicyException(Assertion ass) {
        super(ass);
    }

    public RaisedByPolicyException(Assertion ass, String message) {
        super(ass, message);
    }

    public RaisedByPolicyException(Assertion ass, Throwable cause) {
        super(ass, cause);
    }

    public RaisedByPolicyException(Assertion ass, String message, Throwable cause) {
        super(ass, message, cause);
    }
}
