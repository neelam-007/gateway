/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains the guts of the WspReader and WspWriter, and the registry of types we can serialize.
 */
public class WspConstants {
    public static final String WSP_POLICY_NS = SoapUtil.WSP_NAMESPACE;
    public static final String L7_POLICY_NS = "http://www.layer7tech.com/ws/policy";
    public static final String POLICY_ELNAME = "Policy";

    static boolean isNullableType(Class type) {
        return !(int.class.equals(type) ||
          long.class.equals(type) ||
          boolean.class.equals(type) ||
          float.class.equals(type) ||
          double.class.equals(type) ||
          byte.class.equals(type) ||
          char.class.equals(type));
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

    static boolean isIgnorableProperty(String parm) {
        for (int i = 0; i < ignoreAssertionProperties.length; i++) {
            String ignoreProperty = ignoreAssertionProperties[i];
            if (ignoreProperty.equals(parm))
                return true;
        }
        return false;
    }

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

        new BasicTypeMapping(WsTrustCredentialExchange.TokenServiceRequestType.class, "requestType") {
            protected Object stringToObject(String value) {
                return WsTrustCredentialExchange.TokenServiceRequestType.fromString(value);
            }

            protected String objectToString(Object target) {
                return ((WsTrustCredentialExchange.TokenServiceRequestType)target).getUri();
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
        new AssertionMapping(new WssDigest(), "WssDigest"),
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
        new AssertionMapping(new RequestXpathAssertion(), "RequestXpathAssertion"),
        new AssertionMapping(new ResponseXpathAssertion(), "ResponseXpathAssertion"),
        new AssertionMapping(new SchemaValidation(), "SchemaValidation"),
        new AssertionMapping(new XslTransformation(), "XslTransformation"),
        new AssertionMapping(new TimeRange(), "TimeRange"),
        new AssertionMapping(new RemoteIpRange(), "RemoteIpAddressRange"),
        new AssertionMapping(new AuditAssertion(), "AuditAssertion"),
        new AssertionMapping(new WsTrustCredentialExchange(), "WsTrustCredentialExchange"),
        new SerializedJavaClassMapping(CustomAssertionHolder.class, "CustomAssertion"),
        new AssertionMapping(new UnknownAssertion(), "UnknownAssertion"),

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

    };

    /**
     * Find a TypeMapping capable of serializing the specified class.
     *
     * @param clazz The class to look up
     * @return The TypeMapping for this class, or null if not found.
     */
    static TypeMapping findTypeMappingByClass(Class clazz) {
        for (int i = 0; i < typeMappings.length; i++) {
            TypeMapping typeMapping = typeMappings[i];
            if (typeMapping.getMappedClass().equals(clazz))
                return typeMapping;
        }
        return null;
    }

    /**
     * Find a TypeMapping corresponding to the specified external name (ie, "OneOrMore" or "mapValue").
     *
     * @param typeName The external name to look up
     * @return The TypeMapping for this external name, or null if not found.
     */
    static TypeMapping findTypeMappingByExternalName(String typeName) {
        for (int i = 0; i < typeMappings.length; i++) {
            TypeMapping typeMapping = typeMappings[i];
            if (typeMapping.getExternalName().equals(typeName))
                return typeMapping;
        }
        return null;
    }

    /**
     * Get a list of all immediate child nodes of the given node that are of type ELEMENT.
     *
     * @param node the node to check
     * @return a List of Element objects
     */
    static List getChildElements(Node node) {
        return getChildElements(node, null);
    }

    /**
     * Get a list of all immediate child nodes of the given node that are of type ELEMENT and
     * have the specified name.
     * <p/>
     * <p>For example, if called on the following Foo node, would return
     * the two Bar subnodes but not the Baz subnode or the Bloof grandchild node:
     * <pre>
     *    [Foo]
     *      [Bar/]
     *      [Baz/]
     *      [Bar][Bloof/][/Bar]
     *    [/Foo]
     * </pre>
     *
     * @param node the node to check
     * @param name the required name
     * @return a List of Element objects
     */
    static List getChildElements(Node node, String name) {
        NodeList kidNodes = node.getChildNodes();
        LinkedList kidElements = new LinkedList();
        for (int i = 0; i < kidNodes.getLength(); ++i) {
            Node n = kidNodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && (name == null || name.equals(n.getLocalName())))
                kidElements.add(n);
        }
        return kidElements;
    }

    /**
     * Invoke the public method attempting to avoid the jvm bug
     * see bug parade 4071957, 4852768. The bug is apparently fixed in Tiger - 1.5
     * @param method the method to invoke
     * @param targetObject the target object
     * @param args the method arguments
     * @return the method invocatin return value
     * @throws IllegalAccessException see contract in {@link Method#invoke(Object, Object[])}
     * @throws InvocationTargetException see contract in {@link Method#invoke(Object, Object[])}
     */
    static Object invokeMethod(Method method, Object targetObject, final Object[] args)
      throws IllegalAccessException, InvocationTargetException {
        boolean accessible = method.isAccessible();
        boolean accessibilityChanged = false;
        try {
            if (!accessible) {
                method.setAccessible(true);
                accessibilityChanged = true;
            }
            return method.invoke(targetObject, args);
        } finally {
            if (accessibilityChanged) {
                method.setAccessible(accessible);
            }
        }
    }

    /**
     * Examine the specified DOM Element and deserialize it into a Java object, if possible.  This method will
     * attempt to find a TypeMapper that recognizes the specified Element.
     *
     * @param source The DOM element to examine
     * @return A TypedReference with information about the object.  The name and/or target might be null.
     * @throws InvalidPolicyStreamException if we were unable to recover an object from this Element
     */
    static TypedReference thawElement(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        return typeMappingObject.thaw(source, visitor);
    }

}
