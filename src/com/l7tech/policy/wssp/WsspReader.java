/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
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
                throw new PolicyConversionException("Not yet supported: " + p.getName());
            }
        });
        bindings.put("SymmetricBinding", new PrimitiveAssertionConverter() {
            public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
                throw new PolicyConversionException("Not yet supported: " + p.getName());
            }
        });
    }

    static Map topLevel = new HashMap();
    static {
        topLevel.putAll(bindings);
        topLevel.put("SignedSupportingTokens", new PrimitiveAssertionConverter() {
            public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
                throw new PolicyConversionException("Assertion not yet supported: " + p.getName());
            }
        });
    }

    /** WsspVisitor that recognizes only top-level WS-SecurityPolicy elements. */
    private class TopLevelVisitor extends WsspVisitor {
        protected TopLevelVisitor() {
            super(null); // No parent - top level
        }

        protected Map getConverterMap() {
            return topLevel;
        }

        // As a special case, each top-level All gets an attached TopLevelAll context to gather state info.
        protected Assertion recursiveConvertAll(org.apache.ws.policy.Assertion p) throws PolicyConversionException {
            TopLevelAll context = new TopLevelAll(this);
            AllAssertion all = (AllAssertion)context.recursiveConvertAll(p);
            // TODO add any needed assertions to implement properties gathered by the TopLevelAll
            return all;
        }
    }

    /**
     * Convert the specified Apache WS-SecurityPolicy policy into a Layer 7 policy.
     *
     * @param wsspPolicy  the WS-Policy tree including WS-SecurityPolicy assertions that is to be converted into L7 form.  Must already be in normal form.
     *                    Must not be null.
     * @return the converted Layer 7 policy tree.  Never null
     * @throws PolicyConversionException if the specified wssp policy cannot be expressed in Layer 7 form
     */
    public com.l7tech.policy.assertion.Assertion convertFromWssp(org.apache.ws.policy.Policy wsspPolicy) throws PolicyConversionException {
        if (!wsspPolicy.isNormalized()) throw new IllegalArgumentException("Input policy must be normalized");

        WsspVisitor v = new TopLevelVisitor();
        return v.recursiveConvert(wsspPolicy);
    }
}
