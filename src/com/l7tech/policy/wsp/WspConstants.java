/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 *
 * User: mike
 * Date: Sep 8, 2003
 * Time: 1:33:00 PM
 */
class WspConstants {
    static final String POLICY_NS = "http://www.layer7tech.com/ws/policy";

    static class AssertionMapping {
        Assertion source;
        String tag;
        AssertionMapping(Assertion a, String t) { this.source = a; this.tag = t; }
    }

    static String[] ignoreProperties = {
        "Parent",    // Parent links will be reconstructed when tree is deserialized
        "Copy",      // getCopy() is a utility method of Assertion; not a property
        "Class",     // getClass() is a method of Object; not a property
        "Instance",  // getInstance() is used by singleton assertions; not a property
        "Path",      // getPath() is a utility method of Assertion; not a property
    };

    static boolean isIgnorableProperty(String parm) {
        for (int i = 0; i < ignoreProperties.length; i++) {
            String ignoreProperty = ignoreProperties[i];
            if (ignoreProperty.equals(parm))
                return true;
        }
        return false;
    }

    static AssertionMapping[] supportedCompositeAssertions = {
        new AssertionMapping(new OneOrMoreAssertion(),  "OneOrMore"),
        new AssertionMapping(new AllAssertion(),        "All"),
        new AssertionMapping(new ExactlyOneAssertion(), "ExactlyOne")
    };

    static AssertionMapping[] supportedLeafAssertions = {
        new AssertionMapping(new HttpBasic(),             "HttpBasic"),
        new AssertionMapping(new HttpClientCert(),        "HttpClientCert"),
        new AssertionMapping(new HttpDigest(),            "HttpDigest"),
        new AssertionMapping(new WssBasic(),              "WssBasic"),
        new AssertionMapping(new WssDigest(),             "WssDigest"),
        new AssertionMapping(new FalseAssertion(),        "FalseAssertion"),
        new AssertionMapping(new SslAssertion(),          "SslAssertion"),
        new AssertionMapping(new RoutingAssertion(),      "RoutingAssertion"),
        new AssertionMapping(new TrueAssertion(),         "TrueAssertion"),
        new AssertionMapping(new MemberOfGroup(),         "MemberOfGroup"),
        new AssertionMapping(new SpecificUser(),          "SpecificUser"),
        new AssertionMapping(new XmlResponseSecurity(),   "XmlResponseSecurity"),
        new AssertionMapping(new XmlRequestSecurity(),    "XmlRequestSecurity"),
        new AssertionMapping(new SamlSecurity(),          "SamlSecurity"),
        new AssertionMapping(new RequestXpathAssertion(), "RequestXpathAssertion"),
    };

    static AssertionMapping findAssertionMappingByAssertion(AssertionMapping[] map, Assertion a) {
        for (int i = 0; i < map.length; i++) {
            AssertionMapping assertionMapping = map[i];
            if (assertionMapping.source.getClass().equals(a.getClass()))
                return assertionMapping;
        }
        return null;
    }

    static AssertionMapping findAssertionMappingByTagName(AssertionMapping[] map, String tag) {
        for (int i = 0; i < map.length; i++) {
            AssertionMapping assertionMapping = map[i];
            if (assertionMapping.tag.equals(tag))
                return assertionMapping;
        }
        return null;
    }

    public static AssertionMapping findAssertionMappingByTagName(String name) {
        AssertionMapping m = findAssertionMappingByTagName(supportedCompositeAssertions, name);
        if (m != null)
            return m;
        return findAssertionMappingByTagName(supportedLeafAssertions, name);
    }

    static class TypedReference {
        public Class type;
        public Object target;
        TypedReference(Class type, Object target) { this.type = type; this.target = target; }
    }

    static class TypeMapping {
        Class type;
        String typeName;
        final boolean isNullable;

        private TypeMapping(Class type, String typeName) {
            this.type = type;
            this.typeName = typeName;
            this.isNullable = isNullableType(type);
        }

        /**
         * Convert a simple object to a String.
         * @param in
         * @return
         */
        protected String freeze(Object in) {
            return in == null ? null : in.toString();
        }

        /**
         * Convert an object into an element in the specified Document with the specified name.
         *
         * @param document The document in which to create the new element
         * @param name The name to use for the new element
         * @param source The object to be frozen.
         * @return an Element ready to be inserted into the document in the appropriate place.
         */
        Element freeze(Document document, String name, Object source) {
            Element parmElement = document.createElement(name);
            String svalue = freeze(source);
            if (svalue == null) {
                if (!isNullable)
                    throw new InvalidPolicyTreeException("Assertion has property \"" + name + "\" which mustn't be null yet is");
                parmElement.setAttribute(typeName + "Null", "null");
            } else {
                parmElement.setAttribute(typeName, svalue);
            }
            populateElement(parmElement, document, name, source);
            return parmElement;
        }

        protected void populateElement(Element parmElement, Document document, String name, Object source) {
            // Do nothing; simple types require no additional child elements.
        }

        void thaw(Object target, Method setter, String in)
                throws InvocationTargetException, IllegalAccessException, InvalidPolicyStreamException
        {
            setter.invoke(target, new Object[] { thawValue(in) });
        }

        static TypedReference thawElement(Element source) throws InvalidPolicyStreamException {
            NamedNodeMap attrs = source.getAttributes();
            if (attrs.getLength() != 1)
                throw new InvalidPolicyStreamException("Policy contains a " + source.getNodeName() +
                                                       " element that doesn't have exactly one attribute");
            Node attr = attrs.item(0);
            String typeName = attr.getLocalName();
            String value = attr.getNodeValue();

            // Check for Nulls
            if (typeName.endsWith("Null") && value.equals("null") && typeName.length() > 4) {
                typeName = typeName.substring(0, typeName.length() - 4);
                value = null;
            }

            TypeMapping tm = WspConstants.findTypeMappingByTypeName(typeName);
            if (tm == null)
                throw new InvalidPolicyStreamException("Policy contains unrecognized type name \"" + source.getNodeName() + "\"");

            return new TypedReference(tm.type, tm.thawElementValue(source, value));
        }

        protected Object thawElementValue(Element source, String value) throws InvalidPolicyStreamException {
            // This is the default thawer, usable for all types which can be represented as a simple string.
            return thawValue(value);
        }

        protected Object thawValue(String in) throws InvalidPolicyStreamException {
            return in;
        }
    }

    static TypeMapping[] typeMappings = new TypeMapping[] {
        new TypeMapping(String.class, "stringValue"),
        new TypeMapping(long.class, "longValue") {
            protected Object thawValue(String in) {
                return new Long(in);
            }
        },
        new TypeMapping(Long.class, "boxedLongValue") {
            protected Object thawValue(String in) {
                return new Long(in);
            }
        },
        new TypeMapping(int.class, "intValue") {
            protected Object thawValue(String in) {
                return new Integer(in);
            }
        },
        new TypeMapping(Integer.class, "boxedIntegerValue") {
            protected Object thawValue(String in) {
                return new Integer(in);
            }
        },
        new TypeMapping(boolean.class, "booleanValue") {
            protected Object thawValue(String in) {
                return new Boolean(in);
            }
        },
        new TypeMapping(Boolean.class, "boxedBooleanValue") {
            protected Object thawValue(String in) {
                return new Boolean(in);
            }
        },
        new TypeMapping(SslAssertion.Option.class, "optionValue") {
            protected String freeze(Object in) {
                return in == null ? null : ((SslAssertion.Option)in).getKeyName();
            }

            protected Object thawValue(String in) {
                return SslAssertion.Option.forKeyName(in);
            }
        },

        //TODO: we can't leave serialized java objects in the policy
        new TypeMapping(Map.class, "base64SerializedMapValue") {
            Pattern nocr = Pattern.compile("\\s+", Pattern.MULTILINE);

            protected String freeze(Object in) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(in);
                } catch (IOException e) {
                    throw new RuntimeException(e); // can't happen
                }
                byte[] bay = baos.toByteArray();
                BASE64Encoder enc = new BASE64Encoder();

                String encoded = enc.encode(bay);
                return nocr.matcher(encoded).replaceAll(" ");
            }

            Pattern yescr = Pattern.compile("\\s+", Pattern.MULTILINE);

            protected Object thawValue(String in) throws InvalidPolicyStreamException {
                BASE64Decoder dec = new BASE64Decoder();
                try {
                    in = yescr.matcher(in).replaceAll("\n");
                    byte[] bay = dec.decodeBuffer(in);
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bay));
                    return ois.readObject();
                } catch (IOException e) {
                    throw new InvalidPolicyStreamException("invalid base64 encoded attribute", e);
                } catch (ClassNotFoundException e) {
                    throw new InvalidPolicyStreamException("egg error - unsupported serialized Java class", e);
                }
            }
        },
    };

    static TypeMapping findTypeMappingByClass(Class clazz) {
        for (int i = 0; i < typeMappings.length; i++) {
            TypeMapping typeMapping = typeMappings[i];
            if (typeMapping.type.equals(clazz))
                return typeMapping;
        }
        return null;
    }

    static TypeMapping findTypeMappingByTypeName(String typeName) {
        for (int i = 0; i < typeMappings.length; i++) {
            TypeMapping typeMapping = typeMappings[i];
            if (typeMapping.typeName.equals(typeName))
                return typeMapping;
        }
        return null;
    }

    public static boolean isNullableType(Class type) {
        return !(int.class.equals(type) ||
                 long.class.equals(type) ||
                 boolean.class.equals(type) ||
                 float.class.equals(type) ||
                 double.class.equals(type) ||
                 byte.class.equals(type) ||
                 char.class.equals(type));
    }
}
