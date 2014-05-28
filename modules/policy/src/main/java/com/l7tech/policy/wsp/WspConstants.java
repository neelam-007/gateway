/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.wsp;

import com.l7tech.objectmodel.AttributeHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.*;
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
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.transport.PreemptiveCompression;
import com.l7tech.policy.assertion.transport.RemoteDomainIdentityInjection;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.security.xml.XmlElementEncryptionConfig;
import com.l7tech.util.FullQName;
import com.l7tech.util.NameValuePair;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.TimeUnit;
import com.l7tech.wsdl.BindingInfo;
import com.l7tech.wsdl.BindingOperationInfo;
import com.l7tech.wsdl.MimePartInfo;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.WsTrustRequestType;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathVersion;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains the registry of types we can freeze to a policy.
 */
public class WspConstants {
    public static final String WSP_POLICY_NS = SoapConstants.WSP_NAMESPACE;
    public static final String L7_POLICY_NS = "http://www.layer7tech.com/ws/policy";
    public static final String POLICY_ELNAME = "Policy";
    public static final String WSP_ATTRIBUTE_ENABLED = "L7p:Enabled";

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
            @Override
            protected Object stringToObject(String in) {
                return new Long(in);
            }
        },
        new BasicTypeMapping(Long.class, "boxedLongValue"),

        new BasicTypeMapping(double.class, "doubleValue") {
            @Override
            protected Object stringToObject(String in) {
                return new Double(in);
            }
        },
        new BasicTypeMapping(Double.class, "boxedDoubleValue"),

        new BasicTypeMapping(int.class, "intValue") {
            @Override
            protected Object stringToObject(String in) {
                return new Integer(in);
            }
        },
        new BasicTypeMapping(Integer.class, "boxedIntegerValue"),

        new BasicTypeMapping(boolean.class, "booleanValue") {
            @Override
            protected Object stringToObject(String in) {
                return Boolean.valueOf(in);
            }
        },
        new BasicTypeMapping(Boolean.class, "boxedBooleanValue"),
        new BasicTypeMapping(BigInteger.class, "bigIntegerValue"),

        new BeanTypeMapping(Assertion.Comment.class, "assertionComment"),
        new BasicTypeMapping(Goid.class,"goidValue"),

        // Typesafe enumerations
        new WspEnumTypeMapping(WsTrustRequestType.class, "requestType"),
        new WspEnumTypeMapping(AuthenticationProperties.Method.class, "authenticationMethod"),
        new WspEnumTypeMapping(TimeUnit.class, "abbreviation"),
        new WspEnumTypeMapping(AssertionResourceType.class, "resourceType"),
        new WspEnumTypeMapping(HtmlFormDataType.class, "fieldDataType"),
        new WspEnumTypeMapping(HtmlFormDataLocation.class, "fieldLocation"),
        new WspEnumTypeMapping(CodeInjectionProtectionType.class, "protectionType"),
        new WspEnumTypeMapping(DataType.class, "variableDataType"),
        new WspEnumTypeMapping(LineBreak.class, "lineBreak"),

        // Container types
        new ArrayTypeMapping(new Object[0], "arrayValue"),
        new ArrayTypeMapping(new String[0], "stringArrayValue"),
        new ArrayTypeMapping(new Long[0], "longArrayValue"),
        new ArrayTypeMapping(new Goid[0], "goidArrayValue"),
        new ArrayTypeMapping(new CertificateInfo[0], "certificateInfoArrayValue"),
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
        new SecurityTokenAssertionMapping(new RequireWssX509Cert(), "RequestWssX509Cert",
                                          SecurityTokenType.WSS_X509_BST), // freeze RequestWssX509Cert as SecurityToken or pre32 form; thaw pre32 form
        new SecurityTokenAssertionMapping(new SecureConversation(), "SecureConversation",
                                          SecurityTokenType.WSSC_CONTEXT), // freeze SecureConversation as SecurityToken or pre32 form; thaw pre32 form
        new SecurityTokenAssertionMapping(new RequireWssSaml(), "RequestWssSaml",
                                              SecurityTokenType.SAML_ASSERTION),
        new SecurityTokenAssertionMapping(new RequireWssSaml2(), "RequestWssSaml",
                                          SecurityTokenType.SAML2_ASSERTION),
        new AssertionMapping(new RequestXpathAssertion(), "RequestXpathAssertion") { // thaw pre32 form
            // Compatibility with old 2.1 instances of this assertion
            @Override
            public void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                super.populateObject(object, source, new WspUpgradeUtilFrom21.RequestXpathAssertionPropertyVisitor(visitor));
            }
        },
        new MessagePredicateMapping(new RequestXpathAssertion(), "MessagePredicate"), // freeze RequestXpathAssertion as MessagePredicate or pre32 form; thaw MessagePredicate
        new IntegrityMapping(new RequireWssSignedElement(), "RequireWssSignedElement", false, "5.1.0"),
        new IntegrityMapping(new RequireWssSignedElement(), "Integrity", true, "3.2.0"), // freeze RequestWssIntegrity as wsse:Integrity or pre32 form; thaw wsse:Integrity form
        new IntegrityMapping(new RequireWssSignedElement(), "RequestWssIntegrity", false, null), // thaw pre32 form
        new ConfidentialityMapping(new RequireWssEncryptedElement(), "RequireWssEncryptedElement", "5.1.0"),
        new ConfidentialityMapping(new RequireWssEncryptedElement(), "RequestWssConfidentiality", "3.2.0"), // 3.7+ and pre32 form.
        new ConfidentialityMapping(new RequireWssEncryptedElement(), "Confidentiality"), // thaw wsse:Confidentiality (3.2 - 3.6.5)

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
            @Override
            public void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                super.populateObject(object, source, new WspUpgradeUtilFrom365.HttpRoutingPropertyVisitor(visitor));
            }
        },
        new AssertionMapping(new HttpRoutingAssertion(), "RoutingAssertion"), // backwards compatibility with pre-3.0
        new AssertionMapping(new TrueAssertion(), "TrueAssertion"),
        new AssertionMapping(new MemberOfGroup(), "MemberOfGroup"),
        new AssertionMapping(new SpecificUser(), "SpecificUser"),
        new AssertionMapping(new WssSignElement(), "WssSignElement", "5.1.0"),
        new AssertionMapping(new WssSignElement(), "ResponseWssIntegrity"),
        new AssertionMapping(new WssEncryptElement(), "WssEncryptElement", "5.1.0"),
        new AssertionMapping(new WssEncryptElement(), "ResponseWssConfidentiality"),
        new AssertionMapping(new RequestSwAAssertion(), "RequestSwAAssertion"),
        new AssertionMapping(new WssReplayProtection(), "WssReplayProtection", "5.1.0"),
        new AssertionMapping(new WssReplayProtection(), "RequestWssReplayProtection"),
        new AssertionMapping(new ResponseXpathAssertion(), "ResponseXpathAssertion"),
        new AssertionMapping(new SchemaValidation(), "SchemaValidation") {
            @Override
            public void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                super.populateObject(object, source, new WspUpgradeUtilFrom35.SchemaValidationPropertyVisitor(visitor));
            }
        },
        new AssertionMapping(new XslTransformation(), "XslTransformation") {
            @Override
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
            @Override
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

            @Override
            protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) {
                super.populateElement(wspWriter, element, object);
            }
        },
        new AssertionMapping(new CommentAssertion(), "CommentAssertion"),
        new AssertionMapping(new Operation(), "WSDLOperation"),
        new AssertionMapping(new SqlAttackAssertion(), "SqlAttackProtection"),
        new AssertionMapping(new AddWssTimestamp(), "AddWssTimestamp", "5.1.0"),
        new AssertionMapping(new AddWssTimestamp(), "ResponseWssTimestamp"),
        new AssertionMapping(new RequireWssTimestamp(), "RequireWssTimestamp", "5.1.0"),
        new AssertionMapping(new RequireWssTimestamp(), "RequestWssTimestamp"),
        new AssertionMapping(new AddWssSecurityToken(), "AddWssSecurityToken", "5.1.0"),
        new AssertionMapping(new AddWssSecurityToken(), "ResponseWssSecurityToken"),
        new AssertionMapping(new RequestWssKerberos(), "Kerberos"),
        new AssertionMapping(new WsiBspAssertion(), "WsiBspAssertion"),
        new AssertionMapping(new WsiSamlAssertion(), "WsiSamlAssertion"),
        new AssertionMapping(new WsspAssertion(), "WsspAssertion"),
        new AssertionMapping(new CookieCredentialSourceAssertion(), "CookieCredentialSource"),
        new AssertionMapping(new HtmlFormDataAssertion(), "HtmlFormDataAssertion"),
        new AssertionMapping(new CodeInjectionProtectionAssertion(), "CodeInjectionProtectionAssertion"),
        new AssertionMapping(new PreemptiveCompression(), "PreemptiveCompression"),
        new AssertionMapping(new RemoteDomainIdentityInjection(), "RemoteDomainIdentityInjection"),

        // Special mapping for UnknownAssertion which attempts to preserve original XML element, if any
        new UnknownAssertionMapping(),

        // Special types that are broadly useful, across modules
        new BasicTypeMapping(FullQName.class, "fullQName"),
        new ArrayTypeMapping(new FullQName[0], "fullQNameArray"),
        new BeanTypeMapping(XpathExpression.class, "xpathExpressionValue"),
        new ArrayTypeMapping(new XpathExpression[0], "xpathExpressionArray"),
        new Java5EnumTypeMapping(XpathVersion.class, "xpathVersion"),
        new BeanTypeMapping(BindingInfo.class, "wsdlBindingInfo"),
        new BeanTypeMapping(BindingOperationInfo.class, "wsdlBindingOperationInfo"),
        new BeanTypeMapping(MimePartInfo.class, "wsdlMimePartInfo"),
        new BeanTypeMapping(SamlAttributeStatement.class, "samlAttributeInfo"),
        new BeanTypeMapping(SamlAuthenticationStatement.class, "samlAuthenticationInfo"),
        new BeanTypeMapping(SamlAuthorizationStatement.class, "samlAuthorizationInfo"),
        new BeanTypeMapping(SamlAttributeStatement.Attribute.class, "samlAttributeElementInfo"),
        new ArrayTypeMapping(new SamlAttributeStatement.Attribute[0], "samlAttributeElementInfoArray"),
        new Java5EnumTypeMapping(SamlAttributeStatement.Attribute.AttributeValueAddBehavior.class, "samlAttributeAddBehavior" ),
        new Java5EnumTypeMapping(SamlAttributeStatement.Attribute.AttributeValueComparison.class, "samlAttributeValueComparison" ),
        new Java5EnumTypeMapping(SamlAttributeStatement.Attribute.EmptyBehavior.class, "samlAttributeEmptyBehavior" ),
        new Java5EnumTypeMapping(SamlAttributeStatement.Attribute.VariableNotFoundBehavior.class, "samlAttributeVariableNotFoundBehavior" ),
        new BeanTypeMapping(XmlSecurityRecipientContext.class, "xmlSecurityRecipientContext"),
        new BeanTypeMapping(TimeOfDayRange.class, "timeOfDayRange"),
        new BeanTypeMapping(TimeOfDay.class, "timeOfDay"),
        new BeanTypeMapping(HttpFormPost.FieldInfo.class, "fieldInfo"),
        new ArrayTypeMapping(new HttpFormPost.FieldInfo[0], "fieldInfoArray"),
        new ArrayTypeMapping(new String[0], "fieldNames"),
        new BeanTypeMapping(AuthenticationProperties.class, "authenticationInfo"),
        new BeanTypeMapping(SoapFaultLevel.class, "soapFaultLevel"),
        new BeanTypeMapping(CertificateInfo.class, "certificateInfo"),

        new AbstractClassTypeMapping(AssertionResourceInfo.class, "resourceInfo"),
        new BeanTypeMapping(StaticResourceInfo.class, "staticResourceInfo"),
        new BeanTypeMapping(SingleUrlResourceInfo.class, "singleUrlResourceInfo"),
        new BeanTypeMapping(GlobalResourceInfo.class, "globalResourceInfo"),
        new BeanTypeMapping(MessageUrlResourceInfo.class, "messageUrlResourceInfo"),
        new BeanTypeMapping(HttpPassthroughRule.class, "httpPassthroughRule"),
        new ArrayTypeMapping(new HttpPassthroughRule[0], "httpPassthroughRules"),
        new BeanTypeMapping(HttpPassthroughRuleSet.class, "httpPassthroughRuleSet"),
        new BeanTypeMapping(HtmlFormDataAssertion.FieldSpec.class, "htmlFormFieldSpec"),
        new ArrayTypeMapping(new HtmlFormDataAssertion.FieldSpec[0], "htmlFormFieldSpecArray"),
        new ArrayTypeMapping(new CodeInjectionProtectionType[0], "codeInjectionProtectionTypeArray"),
        new BeanTypeMapping(JmsMessagePropertyRule.class, "jmsMessagePropertyRule"),
        new BeanTypeMapping(JmsDynamicProperties.class, "dynamicJmsRoutingProperties"),
        new ArrayTypeMapping(new JmsMessagePropertyRule[0], "jmsMessagePropertyRuleArray"),
        new BeanTypeMapping(JmsMessagePropertyRuleSet.class, "jmsMessagePropertyRuleSet"),
        new BeanTypeMapping(IdentityTarget.class, "IdentityTarget"),
        new BeanTypeMapping(MessageTargetableSupport.class, "MessageTarget"),
        new BeanTypeMapping(NameValuePair.class, "nameValuePair"),
        new ArrayTypeMapping(new NameValuePair[0], "nameValuePairArray"),

        new Java5EnumTypeMapping(TargetMessageType.class, "target"),
        new Java5EnumTypeMapping(CertificateValidationType.class, "certificateValidation"),
        new Java5EnumTypeMapping(IdentityTarget.TargetIdentityType.class, "identityType"),
        new Java5EnumTypeMapping(SoapVersion.class, "soapVersion"),

        // Make sure we get equal instances of built-in attributes (analogous to readResolve())
        new BeanTypeMapping( AttributeHeader.class, "header") {
            @Override
            public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                TypedReference tr = super.thaw(source, visitor);
                if (tr.target instanceof AttributeHeader) {
                    AttributeHeader ah = (AttributeHeader) tr.target;
                    AttributeHeader canon = AttributeHeader.forName(ah.getVariableName());
                    return canon == null ? tr : new TypedReference(AttributeHeader.class, canon);
                }
                return tr;
            }

            @Override
            public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
                return super.freeze(wspWriter, object, container);
            }
        },
        new BeanTypeMapping(XmlElementEncryptionConfig.class, "xmlElementEncryptionConfig")
    };

    final static TypeMapping[] readOnlyTypeMappings = new TypeMapping[] {

        // Backward compatibility with old policy documents
        WspUpgradeUtilFrom21.xmlResponseSecurityCompatibilityMapping,
        WspUpgradeUtilFrom30.httpClientCertCompatibilityMapping,
        WspUpgradeUtilFrom30.samlSecurityCompatibilityMapping,
        StealthReplacedByFaultLevel.faultDropCompatibilityMapping
    };

}
