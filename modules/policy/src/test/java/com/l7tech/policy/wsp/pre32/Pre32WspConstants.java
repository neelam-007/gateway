/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.xml.soap.SoapUtil;

/**
 * Contains the registry of types we can serialize to a policy.
 */
public class Pre32WspConstants {
    public static final String WSP_POLICY_NS = SoapUtil.WSP_NAMESPACE;
    public static final String L7_POLICY_NS = "http://www.layer7tech.com/ws/policy";
    public static final String POLICY_ELNAME = "Policy";

    // Valid namespaces for a root <Policy> element.
    public static final String[] POLICY_NAMESPACES = {
        WSP_POLICY_NS,
        L7_POLICY_NS,
    };

    static boolean isRecognizedPolicyNsUri(String nsUri) {
        for (int i = 0; i < POLICY_NAMESPACES.length; i++) {
            String policyNamespace = POLICY_NAMESPACES[i];
            if (policyNamespace.equals(nsUri))
                return true;
        }
        return false;
    }

    static String[] ignoreAssertionProperties = {
        "Parent", // Parent links will be reconstructed when tree is deserialized
        "Copy", // getCopy() is a utility method of Assertion; not a property
        "Class", // getClass() is a method of Object; not a property
        "Instance", // getInstance() is used by singleton assertions; not a property
        "Path", // getPath() is a utility method of Assertion; not a property
        "AssertionWithOrdinal", // getAssertionWithOrdinal() is a utility lookup method of Assertion; not a property
        "Ordinal", // ordinal is transient and is recomputed when policy is deserialized
        "CredentialSource", // computed by an assertion to signal that is a credential source
        "CredentialModifier", // computed by an assertion to signal that is a credential source
        "RequireProofOfPosession", // computed by an saml assertion to indicate that the proof of posesions has been required
        "FeatureSetName", // computed by Assertion to determine what feature set name the assertion requires
    };

    static final Pre32TypeMapping typeMappingObject = new Pre32ObjectTypeMapping(Object.class, "objectValue");
    static final Pre32TypeMapping typeMappingString = new Pre32BasicTypeMapping(String.class, "stringValue");

    /**
     * This is our master list of supported type mappings.
     */
    static Pre32TypeMapping[] typeMappings = new Pre32TypeMapping[]{
        // Generic mapper, will look up the real type
        typeMappingObject,

        // Basic types
        typeMappingString,
        new Pre32BasicTypeMapping(long.class, "longValue") {
            protected Object stringToObject(String in) {
                return new Long(in);
            }
        },
        new Pre32BasicTypeMapping(Long.class, "boxedLongValue"),
        new Pre32BasicTypeMapping(int.class, "intValue") {
            protected Object stringToObject(String in) {
                return new Integer(in);
            }
        },
        new Pre32BasicTypeMapping(Integer.class, "boxedIntegerValue"),
        new Pre32BasicTypeMapping(boolean.class, "booleanValue") {
            protected Object stringToObject(String in) {
                return new Boolean(in);
            }
        },
        new Pre32BasicTypeMapping(Boolean.class, "boxedBooleanValue"),

        // Typesafe enumerations
        new Pre32BasicTypeMapping(FakeAssertion.class, "optionValue"),

        // Container types
        new Pre32ArrayTypeMapping(new Object[0], "arrayValue"),
        new Pre32ArrayTypeMapping(new String[0], "stringArrayValue"),
        new Pre32MapTypeMapping(),

        // Composite assertions
        new Pre32CompositeAssertionMapping(new OneOrMoreAssertion(), "OneOrMore"),
        new Pre32CompositeAssertionMapping(new AllAssertion(), "All"),
        new Pre32CompositeAssertionMapping(new ExactlyOneAssertion(), "ExactlyOne"),

        // Leaf assertions
        new Pre32AssertionMapping(new FakeAssertion(), "HttpBasic"),
        new Pre32AssertionMapping(new FakeAssertion(), "HttpDigest"),
        new Pre32AssertionMapping(new FakeAssertion(), "WssBasic"),
        new Pre32AssertionMapping(new FakeAssertion(), "FalseAssertion"),
        new Pre32AssertionMapping(new FakeAssertion(), "SslAssertion"),
        new Pre32AssertionMapping(new FakeAssertion(), "JmsRoutingAssertion"),
        new Pre32AssertionMapping(new FakeAssertion(), "HttpRoutingAssertion"),
        new Pre32AssertionMapping(new FakeAssertion(), "RoutingAssertion"), // backwards compatibility with pre-3.0
        new Pre32AssertionMapping(new FakeAssertion(), "BridgeRoutingAssertion"),
        new Pre32AssertionMapping(new FakeAssertion(), "TrueAssertion"),
        new Pre32AssertionMapping(new FakeAssertion(), "MemberOfGroup"),
        new Pre32AssertionMapping(new FakeAssertion(), "SpecificUser"),
        new Pre32AssertionMapping(new FakeAssertion(), "RequestWssIntegrity"),
        new Pre32AssertionMapping(new FakeAssertion(), "RequestWssConfidentiality"),
        new Pre32AssertionMapping(new FakeAssertion(), "ResponseWssIntegrity"),
        new Pre32AssertionMapping(new FakeAssertion(), "ResponseWssConfidentiality"),
        new Pre32AssertionMapping(new FakeAssertion(), "RequestWssX509Cert"),
        new Pre32AssertionMapping(new FakeAssertion(), "RequestSwAAssertion"),
        new Pre32AssertionMapping(new FakeAssertion(), "SecureConversation"),
        new Pre32AssertionMapping(new FakeAssertion(), "RequestWssReplayProtection"),
        new Pre32AssertionMapping(new FakeAssertion(), "RequestWssSaml"),
        new Pre32AssertionMapping(new FakeAssertion(), "RequestXpathAssertion"),
        new Pre32AssertionMapping(new FakeAssertion(), "ResponseXpathAssertion"),
        new Pre32AssertionMapping(new FakeAssertion(), "SchemaValidation"),
        new Pre32AssertionMapping(new FakeAssertion(), "XslTransformation"),
        new Pre32AssertionMapping(new FakeAssertion(), "TimeRange"),
        new Pre32AssertionMapping(new FakeAssertion(), "RemoteIpAddressRange"),
        new Pre32AssertionMapping(new FakeAssertion(), "AuditAssertion"),
        new Pre32AssertionMapping(new FakeAssertion(), "WsTrustCredentialExchange"),
        new Pre32FakeMapping(FakeAssertion.class, "CustomAssertion"),
        new Pre32AssertionMapping(new FakeAssertion(), "Regex"),
        new Pre32AssertionMapping(new FakeAssertion(), "SnmpTrap"),

        // Special mapping for UnknownAssertion which attempts to preserve original XML element, if any
        new Pre32UnknownAssertionMapping(),

        // Special types
        new Pre32FakeMapping(FakeAssertion.class, "xpathExpressionValue"),
        new Pre32FakeMapping(FakeAssertion.class, "wsdlBindingInfo"),
        new Pre32FakeMapping(FakeAssertion.class, "wsdlBindingOperationInfo"),
        new Pre32FakeMapping(FakeAssertion.class, "wsdlMimePartInfo"),
        new Pre32FakeMapping(FakeAssertion.class, "samlAttributeInfo"),
        new Pre32FakeMapping(FakeAssertion.class, "samlAuthenticationInfo"),
        new Pre32FakeMapping(FakeAssertion.class, "samlAuthorizationInfo"),
        new Pre32FakeMapping(FakeAssertion.class, "samlAttributeElementInfo"),
        new Pre32ArrayTypeMapping(new FakeAssertion[0], "samlAttributeElementInfoArray"),
        new Pre32FakeMapping(FakeAssertion.class, "xmlSecurityRecipientContext"),
        new Pre32FakeMapping(FakeAssertion.class, "timeOfDayRange"),
        new Pre32FakeMapping(FakeAssertion.class, "timeOfDay"),
    };

    public static class FakeAssertion extends Assertion {
        public FakeAssertion() {};
    };
}
