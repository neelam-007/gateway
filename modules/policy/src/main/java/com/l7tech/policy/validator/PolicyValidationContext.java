package com.l7tech.policy.validator;

import com.l7tech.policy.PolicyType;
import com.l7tech.wsdl.Wsdl;

import java.io.Serializable;

public class PolicyValidationContext implements Serializable {
    private final PolicyType policyType;
    private final String policyInternalTag;
    private final Wsdl wsdl;
    private final boolean soap;

    /**
     * @param policyType   policy type.  Generally required.
     * @param policyInternalTag  policy internal tag, if applicable and available.  May be null.
     * @param wsdl  policy WSDL, if a soap policy and if available.  May be null.
     * @param soap  true if this is known to be a SOAP policy
     */
    public PolicyValidationContext(PolicyType policyType, String policyInternalTag, Wsdl wsdl, boolean soap) {
        this.policyType = policyType;
        this.policyInternalTag = policyInternalTag;
        this.wsdl = wsdl;
        this.soap = soap;
    }

    /**
     * @return PolicyType if known and available, otherwise null.
     */
    public PolicyType getPolicyType() {
        return policyType;
    }

    /**
     * @return the policy internal tag name if applicable and available, otherwise null.
     */
    public String getPolicyInternalTag() {
        return policyInternalTag;
    }

    /**
     * @return  Service WSDL if known and available, otherwise null.
     */
    public Wsdl getWsdl() {
        return wsdl;
    }

    /**
     * @return true if the assertion instance is being used within a SOAP service.
     */
    public boolean isSoap() {
        return soap;
    }
}
