package com.l7tech.policy.validator;

import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Context for use with policy validation.
 *
 * <p>A new context should be created for each validation operation.</p>
 */
public class PolicyValidationContext implements Serializable {
    private final @NotNull PolicyType policyType;
    private final @Nullable String policyInternalTag;
    private final @Nullable Wsdl wsdl;
    private final boolean soap;
    private final @Nullable SoapVersion soapVersion;
    private transient Map<Assertion,AssertionValidator> validatorMap = new HashMap<Assertion,AssertionValidator>();

    /**
     * @param policyType   policy type.  Generally required.
     * @param policyInternalTag  policy internal tag, if applicable and available.  May be null.
     * @param wsdl  policy WSDL, if a soap policy and if available.  May be null.
     * @param soap  true if this is known to be a SOAP policy
     * @param soapVersion if a specific SOAP version is in use and, if so, which version that is; or null if not relevant.
     */
    public PolicyValidationContext(@NotNull PolicyType policyType, @Nullable String policyInternalTag, @Nullable Wsdl wsdl, boolean soap, @Nullable SoapVersion soapVersion) {
        this.policyType = policyType;
        this.policyInternalTag = policyInternalTag;
        this.wsdl = wsdl;
        this.soap = soap;
        this.soapVersion = soapVersion;
    }

    /**
     * @return PolicyType if known and available, otherwise null.
     */
    @NotNull
    public PolicyType getPolicyType() {
        return policyType;
    }

    /**
     * @return the policy internal tag name if applicable and available, otherwise null.
     */
    @Nullable
    public String getPolicyInternalTag() {
        return policyInternalTag;
    }

    /**
     * @return  Service WSDL if known and available, otherwise null.
     */
    @Nullable
    public Wsdl getWsdl() {
        return wsdl;
    }

    /**
     * @return true if the assertion instance is being used within a SOAP service.
     */
    public boolean isSoap() {
        return soap;
    }

    /**
     * @return if a specific SOAP version is in use and, if so, what version that is.  May be null if not known or relevant.
     */
    @Nullable
    public SoapVersion getSoapVersion() {
        return soapVersion;
    }

    AssertionValidator getValidator( final Assertion assertion ) {
        AssertionValidator validator = validatorMap.get( assertion );
        if ( validator == null ) {
            validator = ValidatorFactory.getValidator( assertion );
            validatorMap.put( assertion, validator );
        }
        return validator;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        validatorMap = new HashMap<Assertion,AssertionValidator>();
    }
}
