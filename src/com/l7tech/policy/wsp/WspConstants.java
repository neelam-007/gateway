/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.TimeUnit;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.xml.SoapFaultLevel;
import com.l7tech.common.xml.WsTrustRequestType;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.identity.MappingAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains the registry of types we can freeze to a policy.
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
    public static final List POLICY_NAMESPACE_LIST = Arrays.asList(POLICY_NAMESPACES);

    public static final String[] L7_POLICY_NAMESPACES = {
        L7_POLICY_NS,
    };
    public static final List L7_POLICY_NAMESPACE_LIST = Arrays.asList(L7_POLICY_NAMESPACES);

    static boolean isRecognizedPolicyNsUri(String nsUri) {
        for (String policyNamespace : POLICY_NAMESPACES) {
            if (policyNamespace.equals(nsUri))
                return true;
        }
        return false;
    }

    /** TypeMappingFinder to use when parsing policies using a default WspVisitor (permissive or strict) */
    private static final AtomicReference<TypeMappingFinder> typeMappingFinder = new AtomicReference<TypeMappingFinder>();

    /**
     * Check which TypeMappingFinder will be used when parsing policies using one of the default WspVisitors.
     *
     * @return a TypeMappingFinder instance, or null if none is currently set.
     */
    public static TypeMappingFinder getTypeMappingFinder() {
        return typeMappingFinder.get();
    }

    /**
     * Set a TypeMappingFinder to use when parsing policies using one of the default WspVisitors.
     *
     * @param tmf a TypeMappingFinder instance to set, or null to turn this off.
     */
    public static void setTypeMappingFinder(TypeMappingFinder tmf) {
        typeMappingFinder.set(tmf);
    }

    static String[] ignoreAssertionProperties = {
        "Parent", // Parent links will be reconstructed when tree is thawed
        "Copy", // getCopy() is a utility method of Assertion; not a property
        "Class", // getClass() is a method of Object; not a property
        "Instance", // getInstance() is used by singleton assertions; not a property
        "Path", // getPath() is a utility method of Assertion; not a property
        "AssertionWithOrdinal", // getAssertionWithOrdinal() is a utility lookup method of Assertion; not a property
        "Ordinal", // ordinal is transient and is recomputed when policy is thawed
        "CredentialSource", // computed by an assertion to signal that is a credential source
        "CredentialModifier", // computed by an assertion to signal that is a credential modifier
        //"RequireProofOfPosession", // computed by an saml assertion to indicate that the proof of posesions has been required
        "VariablesUsed",
        "VariablesSet",
        "FeatureSetName",
    };

    static final TypeMapping typeMappingObject = new ObjectTypeMapping(Object.class, "objectValue");
    static final TypeMapping typeMappingString = new BasicTypeMapping(String.class, "stringValue");

    /**
     * This is our master list of supported type mappings.
     */
    final static TypeMapping[] typeMappings = new TypeMapping[]{
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

        new BasicTypeMapping(double.class, "doubleValue") {
            protected Object stringToObject(String in) {
                return new Double(in);
            }
        },
        new BasicTypeMapping(Double.class, "boxedDoubleValue"),

        new BasicTypeMapping(int.class, "intValue") {
            protected Object stringToObject(String in) {
                return new Integer(in);
            }
        },
        new BasicTypeMapping(Integer.class, "boxedIntegerValue"),

        new BasicTypeMapping(boolean.class, "booleanValue") {
            protected Object stringToObject(String in) {
                return Boolean.valueOf(in);
            }
        },
        new BasicTypeMapping(Boolean.class, "boxedBooleanValue"),

        // Typesafe enumerations
        new WspEnumTypeMapping(WsTrustRequestType.class, "requestType"),
        new WspEnumTypeMapping(AuthenticationProperties.Method.class, "authenticationMethod"),
        new WspEnumTypeMapping(TimeUnit.class, "abbreviation"),
        new WspEnumTypeMapping(AssertionResourceType.class, "resourceType"),
        new WspEnumTypeMapping(HtmlFormDataType.class, "fieldDataType"),
        new WspEnumTypeMapping(HtmlFormDataLocation.class, "fieldLocation"),
        new WspEnumTypeMapping(CodeInjectionProtectionType.class, "protectionType"),

        // Container types
        new ArrayTypeMapping(new Object[0], "arrayValue"),
        new ArrayTypeMapping(new String[0], "stringArrayValue"),
        new MapTypeMapping(),
        new CollectionTypeMapping(Set.class, String.class, HashSet.class, "stringSetValue"),
        new CollectionTypeMapping(List.class, String.class, ArrayList.class, "stringListValue"),

        // Composite assertions
        new CompositeAssertionMapping(new OneOrMoreAssertion(), "OneOrMore"),
        new CompositeAssertionMapping(new AllAssertion(), "All"),
        new CompositeAssertionMapping(new ExactlyOneAssertion(), "ExactlyOne"),

        // Standard WS-Policy vocabulary
        new SecurityTokenTypeMapping(), // freeze SecurityTokenType object to wsse:TokenType element; thaw wsse:TokenType element
        new SecurityTokenAssertionMapping(), // freeze nothing; thaw all wssu:SecurityToken elements

        // Leaf assertions expressible using standard WS-Policy vocabulary
        new SecurityTokenAssertionMapping(new WssBasic(), "WssBasic",
                                          SecurityTokenType.WSS_USERNAME), // freeze WssBasic as SecurityToken or pre32 form; thaw pre32 form
        new SecurityTokenAssertionMapping(new RequestWssX509Cert(), "RequestWssX509Cert",
                                          SecurityTokenType.WSS_X509_BST), // freeze RequestWssX509Cert as SecurityToken or pre32 form; thaw pre32 form
        new SecurityTokenAssertionMapping(new SecureConversation(), "SecureConversation",
                                          SecurityTokenType.WSSC_CONTEXT), // freeze SecureConversation as SecurityToken or pre32 form; thaw pre32 form
        new SecurityTokenAssertionMapping(new RequestWssSaml(), "RequestWssSaml",
                                          SecurityTokenType.SAML_ASSERTION),
        new SecurityTokenAssertionMapping(new RequestWssSaml2(), "RequestWssSaml",
                                          SecurityTokenType.SAML2_ASSERTION),
        new MessagePredicateMapping(new RequestXpathAssertion(), "MessagePredicate", "RequestXpathAssertion"), // freeze RequestXpathAssertion as MessagePredicate or pre32 form; thaw MessagePredicate
        new AssertionMapping(new RequestXpathAssertion(), "RequestXpathAssertion") { // thaw pre32 form
            // Compatibility with old 2.1 instances of this assertion
            public void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                super.populateObject(object, source, new WspUpgradeUtilFrom21.RequestXpathAssertionPropertyVisitor(visitor));
            }
        },
        new IntegrityMapping(new RequestWssIntegrity(), "Integrity"), // freeze RequestWssIntegrity as wsse:Integrity or pre32 form; thaw wsse:Integrity form
        new IntegrityMapping(new RequestWssIntegrity(), "RequestWssIntegrity"), // thaw pre32 form
        new ConfidentialityMapping(new RequestWssConfidentiality(), "RequestWssConfidentiality"), // 3.7+ and pre32 form.
        new ConfidentialityMapping(new RequestWssConfidentiality(), "Confidentiality"), // thaw wsse:Confidentiality (3.2 - 3.6.5)

        // Encrypted username token will use our proprietary vocabulary since it has special semantics
        new AssertionMapping(new EncryptedUsernameTokenAssertion(), "EncryptedUsernameToken"),

        // Leaf assertions
        new AssertionMapping(new HttpBasic(), "HttpBasic"),
        new AssertionMapping(new HttpDigest(), "HttpDigest"),
        new AssertionMapping(new HttpNegotiate(), "HttpNegotiate"),
        new AssertionMapping(new FalseAssertion(), "FalseAssertion"),
        new AssertionMapping(new SslAssertion(), "SslAssertion"),
        new AssertionMapping(new JmsRoutingAssertion(), "JmsRoutingAssertion"),
        new AssertionMapping(new HttpRoutingAssertion(), "HttpRoutingAssertion") {
            public void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                super.populateObject(object, source, new WspUpgradeUtilFrom365.HttpRoutingPropertyVisitor(visitor));
            }
        },
        new AssertionMapping(new HttpRoutingAssertion(), "RoutingAssertion"), // backwards compatibility with pre-3.0
        new AssertionMapping(new BridgeRoutingAssertion(), "BridgeRoutingAssertion"),
        new AssertionMapping(new TrueAssertion(), "TrueAssertion"),
        new AssertionMapping(new MemberOfGroup(), "MemberOfGroup"),
        new AssertionMapping(new SpecificUser(), "SpecificUser"),
        new AssertionMapping(new ResponseWssIntegrity(), "ResponseWssIntegrity"),
        new AssertionMapping(new ResponseWssConfidentiality(), "ResponseWssConfidentiality"),
        new AssertionMapping(new RequestSwAAssertion(), "RequestSwAAssertion"),
        new AssertionMapping(new RequestWssReplayProtection(), "RequestWssReplayProtection"),
        new AssertionMapping(new ResponseXpathAssertion(), "ResponseXpathAssertion"),
        new AssertionMapping(new SchemaValidation(), "SchemaValidation") {
            public void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                super.populateObject(object, source, new WspUpgradeUtilFrom35.SchemaValidationPropertyVisitor(visitor));
            }
        },
        new AssertionMapping(new XslTransformation(), "XslTransformation") {
            public void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                super.populateObject(object, source, new WspUpgradeUtilFrom35.XslTransformationPropertyVisitor(visitor));
            }
        },
        new AssertionMapping(new RemoteIpRange(), "RemoteIpAddressRange"),
        new AssertionMapping(new AuditAssertion(), "AuditAssertion"),
        new AssertionMapping(new AuditDetailAssertion(), "AuditDetailAssertion"),
        new AssertionMapping(new WsTrustCredentialExchange(), "WsTrustCredentialExchange"),
        new SerializedJavaClassMapping(CustomAssertionHolder.class, "CustomAssertion"),
        new AssertionMapping(new Regex(), "Regex"),
        new AssertionMapping(new EmailAlertAssertion(), "EmailAlert") {
            public void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                if (source == null) { throw new IllegalArgumentException("Source cannot be null");}
                NodeList messageNodes = source.getElementsByTagName("L7p:Message");
                if (messageNodes.getLength() == 0) {
                    super.populateObject(object, source, visitor);
                } else {
                    //this is an old style EmailAssertion
                    Element messageNode = (Element)messageNodes.item(0);
                    Node messageAttr = messageNode.getAttributeNode("stringValue");
                    String message = messageAttr.getNodeValue();

                    EmailAlertAssertion ema = (EmailAlertAssertion) object.target;
                    ema.messageString(message);
                    super.populateObject(object, source, new PermissiveWspVisitor(visitor.getTypeMappingFinder()));
                }
            }

            protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) {
                super.populateElement(wspWriter, element, object);
            }
        },
        new AssertionMapping(new CommentAssertion(), "CommentAssertion"),
        new AssertionMapping(new Operation(), "WSDLOperation"),
        new AssertionMapping(new SqlAttackAssertion(), "SqlAttackProtection"),
        new AssertionMapping(new HardcodedResponseAssertion(), "HardcodedResponse") {
            protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) {
                super.populateElement(wspWriter, element, object);
            }

            public void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                if (source == null) { throw new IllegalArgumentException("Source cannot be null");}
                NodeList responseBodyNodes = source.getElementsByTagName("L7p:ResponseBody");
                if (responseBodyNodes.getLength() == 0) {
                    super.populateObject(object, source, visitor);
                } else {
                    //this is an old style HardCodedResponse Assertion
                    Element bodyNode = (Element)responseBodyNodes.item(0);
                    Node bodyAttr = bodyNode.getAttributeNode("stringValue");
                    String responseBody = bodyAttr.getNodeValue();

                    HardcodedResponseAssertion hra = (HardcodedResponseAssertion) object.target;
                    hra.responseBodyString(responseBody);
                    super.populateObject(object, source, new PermissiveWspVisitor(visitor.getTypeMappingFinder()));
                }
            }
        },
        new AssertionMapping(new ResponseWssTimestamp(), "ResponseWssTimestamp"),
        new AssertionMapping(new RequestWssTimestamp(), "RequestWssTimestamp"),
        new AssertionMapping(new ResponseWssSecurityToken(), "ResponseWssSecurityToken"),
        new AssertionMapping(new RequestWssKerberos(), "Kerberos"),
        new AssertionMapping(new MappingAssertion(), "IdentityMapping"),
        new AssertionMapping(new WsiBspAssertion(), "WsiBspAssertion"),
        new AssertionMapping(new WsiSamlAssertion(), "WsiSamlAssertion"),
        new AssertionMapping(new WsspAssertion(), "WsspAssertion"),
        new AssertionMapping(new CookieCredentialSourceAssertion(), "CookieCredentialSource"),
        new AssertionMapping(new HtmlFormDataAssertion(), "HtmlFormDataAssertion"),
        new AssertionMapping(new CodeInjectionProtectionAssertion(), "CodeInjectionProtectionAssertion"),

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
        new ArrayTypeMapping(new String[0], "fieldNames"),
        new BeanTypeMapping(AuthenticationProperties.class, "authenticationInfo"),
        new BeanTypeMapping(SoapFaultLevel.class, "soapFaultLevel"),

        new AbstractClassTypeMapping(AssertionResourceInfo.class, "resourceInfo"),
        new BeanTypeMapping(StaticResourceInfo.class, "staticResourceInfo"),
        new BeanTypeMapping(SingleUrlResourceInfo.class, "singleUrlResourceInfo"),
        new BeanTypeMapping(MessageUrlResourceInfo.class, "messageUrlResourceInfo"),
        new BeanTypeMapping(HttpPassthroughRule.class, "httpPassthroughRule"),
        new ArrayTypeMapping(new HttpPassthroughRule[0], "httpPassthroughRules"),
        new BeanTypeMapping(HttpPassthroughRuleSet.class, "httpPassthroughRuleSet"),
        new BeanTypeMapping(HtmlFormDataAssertion.FieldSpec.class, "htmlFormFieldSpec"),
        new ArrayTypeMapping(new HtmlFormDataAssertion.FieldSpec[0], "htmlFormFieldSpecArray"),

    };

    final static TypeMapping[] readOnlyTypeMappings = new TypeMapping[] {

        // Backward compatibility with old policy documents
        WspUpgradeUtilFrom21.xmlResponseSecurityCompatibilityMapping,
        WspUpgradeUtilFrom30.httpClientCertCompatibilityMapping,
        WspUpgradeUtilFrom30.samlSecurityCompatibilityMapping,
        WspUpgradeUtilFrom30.wssDigestCompatibilityMapping,

        // TODO figure out where to put this
        // EqualityRenamedToComparison.equalityCompatibilityMapping,
        StealthReplacedByFaultLevel.faultDropCompatibilityMapping
    };

}