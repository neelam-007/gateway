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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            if (source != null)
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
            //System.out.println("Thawing element " + source.getLocalName() + " " + typeName + "=" + value);

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

    static abstract class ComplexTypeMapping extends TypeMapping {
        ComplexTypeMapping(Class type, String typeName) {
            super(type, typeName);
        }

        protected String freeze(Object in) {
            return in == null ? null : "included";
        }

        protected Object thawElementValue(Element source, String value) throws InvalidPolicyStreamException {
            if (value == null)
                return value;
            if (!"included".equals(value))
                throw new InvalidPolicyStreamException("Complex elements must have the value \"included\"");
            return thawComplexElement(source);
        }

        protected abstract Object thawComplexElement(Element source) throws InvalidPolicyStreamException;
    }

    static final TypeMapping typeMappingString = new TypeMapping(String.class, "stringValue");
    static final TypeMapping typeMappingMap = new ComplexTypeMapping(Map.class, "mapValue") {
        protected void populateElement(Element parmElement, Document document, String name, Object source) {
            Map map = (Map) source;
            Set entries = map.entrySet();
            for (Iterator i = entries.iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                Object key = entry.getKey();
                if (key == null)
                    throw new InvalidPolicyTreeException("Maps with null keys are not currently permitted within a policy");
                if (!(key instanceof String))
                    throw new InvalidPolicyTreeException("Maps with non-string keys are not currently permitted within a policy");
                Object value = entry.getValue();
                if (value != null && !(value instanceof String))
                    throw new InvalidPolicyTreeException("Maps with non-string values are not currently permitted within a policy");
                Element entryElement = document.createElement("entry");
                entryElement.appendChild(typeMappingString.freeze(document, "key", key));
                entryElement.appendChild(typeMappingString.freeze(document, "value", value));
                parmElement.appendChild(entryElement);
            }
        }

        protected Object thawComplexElement(Element source) throws InvalidPolicyStreamException {
            Map map = new HashMap();
            List entryElements = getChildElements(source, "entry");
            for (Iterator i = entryElements.iterator(); i.hasNext();) {
                Element element = (Element) i.next();
                List keyValueElements = getChildElements(element);
                if (keyValueElements.size() != 2)
                    throw new InvalidPolicyStreamException("Map entry does not have exactly two child elements (key and value)");
                Element keyElement = (Element) keyValueElements.get(0);
                if (keyElement == null || keyElement.getNodeType() != Node.ELEMENT_NODE || !"key".equals(keyElement.getLocalName()))
                    throw new InvalidPolicyStreamException("Map entry first child element is not a key element");
                Element valueElement = (Element) keyValueElements.get(1);
                if (valueElement == null || valueElement.getNodeType() != Node.ELEMENT_NODE || !"value".equals(valueElement.getLocalName()))
                    throw new InvalidPolicyStreamException("Map entry last child element is not a value element");

                TypedReference ktr = TypeMapping.thawElement(keyElement);
                if (!String.class.equals(ktr.type))
                    throw new InvalidPolicyStreamException("Maps with non-string keys are not currently permitted within a policy");
                if (ktr.target == null)
                    throw new InvalidPolicyStreamException("Maps with null keys are not currently permitted within a policy");
                String key = (String) ktr.target;

                TypedReference vtr = TypeMapping.thawElement(valueElement);
                if (!String.class.equals(vtr.type))
                    throw new InvalidPolicyStreamException("Maps with non-string values are not currently permitted within a policy");
                String value = (String) vtr.target;
                map.put(key, value);
            }
            return map;
        }
    };

    static TypeMapping[] typeMappings = new TypeMapping[] {
        typeMappingString,
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

        typeMappingMap,
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

    /**
     * Get a list of all immediate child nodes of the given node that are of type ELEMENT.
     * @param node the node to check
     * @return a List of Element objects
     */
    static List getChildElements(Node node) {
        return getChildElements(node, null);
    }

    /**
     * Get a list of all immediate child nodes of the given node that are of type ELEMENT and
     * have the specified name.
     *
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
     * @param node  the node to check
     * @param name  the required name
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
}
