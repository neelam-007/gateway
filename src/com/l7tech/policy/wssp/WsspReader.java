/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import org.apache.ws.policy.PrimitiveAssertion;
import org.apache.ws.policy.Policy;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts WS-SecurityPolicy into Layer 7 policy.
 */
public class WsspReader {

    static Map bindings = new HashMap();
    static {
        bindings.put("AsymmetricBinding", new PrimitiveAssertionConverter() {
            public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
                AsymmetricBinding binding = new AsymmetricBinding(v, p);
                return binding.toLayer7Policy();
            }
        });
        bindings.put("TransportBinding", new UnsupportedPrimitiveAssertion("Security binding yet supported"));
        bindings.put("SymmetricBinding", new UnsupportedPrimitiveAssertion("Security binding yet supported"));
    }

    static Map topLevel = new HashMap();
    static {
        topLevel.putAll(bindings);
        topLevel.put("SignedSupportingTokens", new UnsupportedPrimitiveAssertion("Assertion yet supported"));
        topLevel.put("Wss10", new PrimitiveAssertionIgnorer());
        topLevel.put("Trust10", new PrimitiveAssertionIgnorer());
        topLevel.put("SignedParts", new NamedPartsConverter("Sign"));
        topLevel.put("EncryptedParts", new NamedPartsConverter("Encrypt"));
    }

    /** WsspVisitor that recognizes only top-level WS-SecurityPolicy elements. */
    private class TopLevelVisitor extends WsspVisitor {
        boolean isRequest = false;

        protected boolean maybeSetSimpleProperty(String propName, boolean propValue) {
            if (IS_REQUEST.equals(propName)) {
                isRequest = propValue;
                return true;
            }
            return super.maybeSetSimpleProperty(propName, propValue);
        }

        public boolean isSimpleProperty(String propName) throws PolicyConversionException {
            if (IS_REQUEST.equals(propName))
                return isRequest;
            return super.isSimpleProperty(propName);
        }

        protected TopLevelVisitor() {
            super(null); // No parent - top level
        }

        protected Map getConverterMap() {
            return topLevel;
        }

        // As a special case, each top-level All gets an attached TopLevelAll context to gather state info.
        protected Assertion recursiveConvertAll(org.apache.ws.policy.Assertion p) throws PolicyConversionException {
            TopLevelAll context = new TopLevelAll(this);
            return (AllAssertion)context.recursiveConvertAll(p);
        }
    }

    /**
     * Convert the specified Apache WS-SecurityPolicy policy into a Layer 7 policy, specifying request decroation
     * rules.
     *
     * @param wsspPolicy  the WS-Policy tree including WS-SecurityPolicy assertions that is to be converted into L7 form.  Must already be in normal form.
     *                    Must not be null.
     * @return the converted Layer 7 policy tree.  Never null
     * @throws PolicyConversionException if the specified wssp policy cannot be expressed in Layer 7 form
     */
    public Assertion convertFromWsspForRequest(Policy wsspPolicy) throws PolicyConversionException {
        if (!wsspPolicy.isNormalized()) throw new IllegalArgumentException("Input policy must be normalized");

        WsspVisitor v = new TopLevelVisitor();
        v.setSimpleProperty(WsspVisitor.IS_REQUEST, false);
        return v.recursiveConvert(wsspPolicy);
    }

    /**
     * Convert the specified Apache WS-SecurityPolicy policy into a Layer 7 policy, specifying response decroation
     * rules.
     *
     * @param wsspPolicy  the WS-Policy tree including WS-SecurityPolicy assertions that is to be converted into L7 form.  Must already be in normal form.
     *                    Must not be null.
     * @return the converted Layer 7 policy tree.  Never null
     * @throws PolicyConversionException if the specified wssp policy cannot be expressed in Layer 7 form
     */
    public Assertion convertFromWsspForResponse(Policy wsspPolicy) throws PolicyConversionException {
        if (!wsspPolicy.isNormalized()) throw new IllegalArgumentException("Input policy must be normalized");

        WsspVisitor v = new TopLevelVisitor();
        v.setSimpleProperty(WsspVisitor.IS_REQUEST, true);
        return v.recursiveConvert(wsspPolicy);
    }

}
