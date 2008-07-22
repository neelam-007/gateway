/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import org.apache.ws.policy.Policy;
import org.apache.ws.policy.PrimitiveAssertion;

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
        bindings.put("TransportBinding", new PrimitiveAssertionConverter() {
            public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
                TransportBinding binding = new TransportBinding(v, p);
                return binding.toLayer7Policy();
            }
        });
        bindings.put("SymmetricBinding", new UnsupportedPrimitiveAssertion("Security binding yet supported"));
    }

    static Map topLevel = new HashMap();
    static {
        topLevel.putAll(bindings);
        topLevel.put("SignedSupportingTokens", new PrimitiveAssertionConverter() {
            public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
                SignedSupportingTokens sst = new SignedSupportingTokens(v, p);
                return sst.toLayer7Policy();
            }
        });
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
            return collapse(context.recursiveConvertAll(p));
        }
    }

    /**
     * Convert the specified Apache WS-SecurityPolicy policy into a Layer 7 policy, specifying request decroation
     * rules.
     *
     * @param requestPolicy  the input operation WS-Policy tree including WS-SecurityPolicy assertions that is to be
     *                       converted into L7 form.  Must already be in normal form.  Must not be null.
     * @param responsePolicy the output operation WS-Policy tree including WS-SecurityPolicy assertions that is to be
     *                       converted into L7 form.  Must already be in normal form.  Must not be null.
     * @return the converted Layer 7 policy tree.  Never null
     * @throws PolicyConversionException if the specified wssp policy cannot be expressed in Layer 7 form
     */
    public Assertion convertFromWssp(Policy requestPolicy, Policy responsePolicy) throws PolicyConversionException {
        if (!requestPolicy.isNormalized()) throw new IllegalArgumentException("Input policy must be normalized");
        if (!responsePolicy.isNormalized()) throw new IllegalArgumentException("Input policy must be normalized");

        WsspVisitor v = new TopLevelVisitor();
        v.setSimpleProperty(WsspVisitor.IS_REQUEST, true);
        Assertion convertedReq = v.recursiveConvert(requestPolicy);

        v = new TopLevelVisitor();
        v.setSimpleProperty(WsspVisitor.IS_REQUEST, false);
        Assertion convertedResp = v.recursiveConvert(responsePolicy);

        AllAssertion combined = new AllAssertion();
        if (convertedReq != null)
            combined.addChild(convertedReq);
        if (convertedResp != null)
            combined.addChild(convertedResp);

        Assertion converted = WsspVisitor.collapse(combined);
        return converted == null ? new TrueAssertion() : converted;
    }
}
