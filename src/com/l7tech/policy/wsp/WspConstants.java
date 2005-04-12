/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.xml.WsTrustRequestType;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.alert.SnmpTrapAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import org.w3c.dom.Element;

/**
 * Contains the registry of types we can serialize to a policy.
 */
public class WspConstants {
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
        "RequireProofOfPosession", // computed by an saml assertion to indicate that the proof of posesions has been required
    };

    static final TypeMapping typeMappingObject = new ObjectTypeMapping(Object.class, "objectValue");
    static final TypeMapping typeMappingString = new BasicTypeMapping(String.class, "stringValue");

    /**
     * This is our master list of supported type mappings.
     */
    static TypeMapping[] typeMappings = new TypeMapping[]{
        // Generic mapper, will look up the real type
        typeMappingObject,

        // Basic types
        typeMappingString,
        new BasicTypeMapping(long.class, "longValue") {
            protected Object stringToObject(String in) {
                return new Long(in);
            }
        },
        new BasicTypeMapping(Long.class, "boxedLongValue"),
        new BasicTypeMapping(int.class, "intValue") {
            protected Object stringToObject(String in) {
                return new Integer(in);
            }
        },
        new BasicTypeMapping(Integer.class, "boxedIntegerValue"),
        new BasicTypeMapping(boolean.class, "booleanValue") {
            protected Object stringToObject(String in) {
                return new Boolean(in);
            }
        },
        new BasicTypeMapping(Boolean.class, "boxedBooleanValue"),

        // Typesafe enumerations
        new BasicTypeMapping(SslAssertion.Option.class, "optionValue") {
            protected String objectToString(Object in) {
                return ((SslAssertion.Option)in).getKeyName();
            }

            protected Object stringToObject(String in) {
                return SslAssertion.Option.forKeyName(in);
            }
        },

        new BasicTypeMapping(WsTrustRequestType.class, "requestType") {
            protected Object stringToObject(String value) {
                return WsTrustRequestType.fromString(value);
            }

            protected String objectToString(Object target) {
                return ((WsTrustRequestType)target).getUri();
            }
        },

        // Container types
        new ArrayTypeMapping(new Object[0], "arrayValue"),
        new ArrayTypeMapping(new String[0], "stringArrayValue"),
        new MapTypeMapping(),

        // Composite assertions
        new CompositeAssertionMapping(new OneOrMoreAssertion(), "OneOrMore"),
        new CompositeAssertionMapping(new AllAssertion(), "All"),
        new CompositeAssertionMapping(new ExactlyOneAssertion(), "ExactlyOne"),

        // Leaf assertions
        new AssertionMapping(new HttpBasic(), "HttpBasic"),
        new AssertionMapping(new HttpDigest(), "HttpDigest"),
        new AssertionMapping(new WssBasic(), "WssBasic"),
        new AssertionMapping(new FalseAssertion(), "FalseAssertion"),
        new AssertionMapping(new SslAssertion(), "SslAssertion"),
        new AssertionMapping(new JmsRoutingAssertion(), "JmsRoutingAssertion"),
        new AssertionMapping(new HttpRoutingAssertion(), "HttpRoutingAssertion"),
        new AssertionMapping(new HttpRoutingAssertion(), "RoutingAssertion"), // backwards compatibility with pre-3.0
        new AssertionMapping(new BridgeRoutingAssertion(), "BridgeRoutingAssertion"),
        new AssertionMapping(new TrueAssertion(), "TrueAssertion"),
        new AssertionMapping(new MemberOfGroup(), "MemberOfGroup"),
        new AssertionMapping(new SpecificUser(), "SpecificUser"),
        new AssertionMapping(new RequestWssIntegrity(), "RequestWssIntegrity"),
        new AssertionMapping(new RequestWssConfidentiality(), "RequestWssConfidentiality"),
        new AssertionMapping(new ResponseWssIntegrity(), "ResponseWssIntegrity"),
        new AssertionMapping(new ResponseWssConfidentiality(), "ResponseWssConfidentiality"),
        new AssertionMapping(new RequestWssX509Cert(), "RequestWssX509Cert"),
        new AssertionMapping(new RequestSwAAssertion(), "RequestSwAAssertion"),
        new AssertionMapping(new SecureConversation(), "SecureConversation"),
        new AssertionMapping(new RequestWssReplayProtection(), "RequestWssReplayProtection"),
        new AssertionMapping(new RequestWssSaml(), "RequestWssSaml"),
        new AssertionMapping(new RequestXpathAssertion(), "RequestXpathAssertion") {
            // Compatibility with old 2.1 instances of this assertion
            protected void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                super.populateObject(object, source, new WspUpgradeUtilFrom21.RequestXpathAssertionPropertyVisitor(visitor));
            }
        },
        new AssertionMapping(new ResponseXpathAssertion(), "ResponseXpathAssertion"),
        new AssertionMapping(new SchemaValidation(), "SchemaValidation"),
        new AssertionMapping(new XslTransformation(), "XslTransformation"),
        new AssertionMapping(new TimeRange(), "TimeRange"),
        new AssertionMapping(new RemoteIpRange(), "RemoteIpAddressRange"),
        new AssertionMapping(new AuditAssertion(), "AuditAssertion"),
        new AssertionMapping(new WsTrustCredentialExchange(), "WsTrustCredentialExchange"),
        new SerializedJavaClassMapping(CustomAssertionHolder.class, "CustomAssertion"),
        new AssertionMapping(new Regex(), "Regex"),
        new AssertionMapping(new SnmpTrapAssertion(), "SnmpTrap"),
        new AssertionMapping(new EmailAlertAssertion(), "EmailAlert"),
        new AssertionMapping(new HttpFormPost(), "HttpFormPost"),

        // Special mapping for UnknownAssertion which attempts to preserve original XML element, if any
        new UnknownAssertionMapping(),

        // Special types
        new BeanTypeMapping(XpathExpression.class, "xpathExpressionValue"),
        new BeanTypeMapping(BindingInfo.class, "wsdlBindingInfo"),
        new BeanTypeMapping(BindingOperationInfo.class, "wsdlBindingOperationInfo"),
        new BeanTypeMapping(MimePartInfo.class, "wsdlMimePartInfo"),
        new BeanTypeMapping(SamlAttributeStatement.class, "samlAttributeInfo"),
        new BeanTypeMapping(SamlAuthenticationStatement.class, "samlAuthenticationInfo"),
        new BeanTypeMapping(SamlAuthorizationStatement.class, "samlAuthorizationInfo"),
        new BeanTypeMapping(SamlAttributeStatement.Attribute.class, "samlAttributeElementInfo"),
        new ArrayTypeMapping(new SamlAttributeStatement.Attribute[0], "samlAttributeElementInfoArray"),
        new BeanTypeMapping(XmlSecurityRecipientContext.class, "xmlSecurityRecipientContext"),
        new BeanTypeMapping(TimeOfDayRange.class, "timeOfDayRange"),
        new BeanTypeMapping(TimeOfDay.class, "timeOfDay"),
        new BeanTypeMapping(HttpFormPost.FieldInfo.class, "fieldInfo"),
        new ArrayTypeMapping(new HttpFormPost.FieldInfo[0], "fieldInfoArray"),

        // Backward compatibility with old policy documents
        WspUpgradeUtilFrom21.xmlRequestSecurityCompatibilityMapping,
        WspUpgradeUtilFrom21.xmlResponseSecurityCompatibilityMapping,
        WspUpgradeUtilFrom30.httpClientCertCompatibilityMapping,
        WspUpgradeUtilFrom30.samlSecurityCompatibilityMapping,
        WspUpgradeUtilFrom30.wssDigestCompatibilityMapping,
    };
}
