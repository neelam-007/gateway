/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Contains the guts of the WspReader and WspWriter, and the registry of types we can serialize.
 */
public class WspConstants {
    public static final String POLICY_NS = "http://www.layer7tech.com/ws/policy";
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

    /**
     * A reference that knows the nominal type of its target, even when it is null.
     */
    static class TypedReference {
        public final Class type;
        public final Object target;
        public final String name;

        TypedReference(Class type, Object target, String name) {
            this.type = type;
            this.target = target;
            this.name = name;
            if (type == null)
                throw new IllegalArgumentException("A non-null concrete type must be provided");
            if (name == null && target == null)
                throw new IllegalArgumentException("Only named references may have a null target");
        }

        TypedReference(Class type, Object target) {
            this(type, target, null);
        }
    }

    interface TypeMapping {
        Class getMappedClass();

        String getExternalName();

        /**
         * Serialize the specified Object as a child element of the specified container element.  The serialized
         * element will be added to container with appendChild(), and will also be returned for reference.
         * <p/>
         * If object has a non-null name field, then "Named" format will be used for the returned Element:
         * the returned element will look like <code>&lt;name typeValueNull="null"/&gt;</code> if null, like
         * <code>&lt;name typeValue="..."/&gt;</code> if a non-null simple type, or like
         * <code>&lt;name typeValue="included"&gt;...&lt;/name&gt;</code> if a non-null complex type.
         * <p/>
         * If object has a null name field, then "Anonymous" format will be used for the returned Element:
         * the object may not be null, and the returned element will look like <code>&lt;Type&gt;...&lt;/Type&gt;</code>
         *
         * @param object    the object to serialize
         * @param container the container to receive it
         * @return the newly created Element, which has also been appended underneath container
         */
        Element freeze(TypedReference object, Element container);

        /**
         * De-serialize the specified XML element into an Object and return a TypedReference the new Object.
         * The returned TypedReference will have a name if one is known.
         *
         * @param source
         * @return
         */
        TypedReference thaw(Element source) throws InvalidPolicyStreamException;
    }

    /**
     * TypeMapping to use for basic concrete types whose values are represented most naturally by simple strings.
     */
    static class BasicTypeMapping implements TypeMapping {
        protected String externalName;
        protected Class clazz;
        protected boolean isNullable;
        protected Constructor stringConstructor;  // constructor-from-string, if this type has one

        BasicTypeMapping(Class clazz, String externalName) {
            this.clazz = clazz;
            this.externalName = externalName;
            this.isNullable = isNullableType(clazz);
            try {
                stringConstructor = clazz.getConstructor(new Class[]{String.class});
            } catch (Exception e) {
                stringConstructor = null;
            }
        }

        public Class getMappedClass() { return clazz; }

        public String getExternalName() { return externalName; }

        public Element freeze(TypedReference object, Element container) {
            if (object == null)
                throw new IllegalArgumentException("a non-null TypedReference must be provided");
            if (container == null)
                throw new IllegalArgumentException("a non-null container must be provided");
            if (object.type != clazz)
                throw new IllegalArgumentException("this TypeMapper is only for " + clazz + "; can't use with " + object.type);
            Element elm = object.name == null ? freezeAnonymous(object, container) : freezeNamed(object, container);
            container.appendChild(elm);
            return elm;
        }

        /**
         * Return the new element, without appending it to the container yet.
         */
        protected Element freezeAnonymous(TypedReference object, Element container) {
            throw new IllegalArgumentException("BasicTypeMapping supports only Named format");
        }

        /**
         * Return the new element, without appending it to the container yet.
         */
        protected Element freezeNamed(TypedReference object, Element container) {
            Element elm = container.getOwnerDocument().createElement(object.name);
            if (object.target == null) {
                if (!isNullable)  // sanity check
                    throw new InvalidPolicyTreeException("Assertion has property \"" + object.name + "\" which mustn't be null yet is");
                elm.setAttribute(externalName + "Null", "null");
            } else {
                String stringValue = objectToString(object.target);
                elm.setAttribute(externalName, stringValue);
                populateElement(elm, object); // hook for more complex types
            }
            return elm;
        }

        /**
         * Do any extra work that might be requried by this element.
         *
         * @param newElement the newly-created element that needs to have properties filled in from object, whose
         *                   target may not be null.
         * @param object
         */
        protected void populateElement(Element newElement, TypedReference object) throws InvalidPolicyTreeException {
            // no action required for simple types
        }

        /**
         * Convert object into a string that can be saved as an attribute value.
         *
         * @param target the object to examine. must not be null
         * @return the object in string form, or null if the object was null
         */
        protected String objectToString(Object target) {
            return target.toString();
        }

        public TypedReference thaw(Element source) throws InvalidPolicyStreamException {
            NamedNodeMap attrs = source.getAttributes();
            switch (attrs.getLength()) {
                case 0:
                    // Anonymous element
                    return thawAnonymous(source);

                case 1:
                    // Named element
                    return thawNamed(source);

                default:
                    throw new InvalidPolicyStreamException("Policy contains a " + source.getNodeName() +
                      " element with more than one attribute");
            }
        }

        protected TypedReference thawNamed(Element source) throws InvalidPolicyStreamException {
            NamedNodeMap attrs = source.getAttributes();
            if (attrs.getLength() != 1)
                throw new InvalidPolicyStreamException("Policy contains a " + source.getNodeName() +
                  " element that doesn't have exactly one attribute");
            Node attr = attrs.item(0);
            String typeName = attr.getLocalName();
            String value = attr.getNodeValue();

            if (typeName.endsWith("Null") && typeName.length() > 4) {
                typeName = typeName.substring(0, typeName.length() - 4);
                value = null;
                if (!isNullable)
                    throw new InvalidPolicyStreamException("Policy contains a null " + externalName);
            }

            if (!externalName.equals(typeName))
                throw new InvalidPolicyStreamException("TypeMapping for " + clazz + ": unrecognized attr " + typeName);

            if (value == null)
                return new TypedReference(clazz, null, source.getNodeName());

            return createObject(source, value);
        }

        /**
         * Inspect the DOM element and construct the actual object, which at this point is known to be non-null.
         * The default implementation calls stringToObject(value) to create the object, and populateObject() to fill
         * out its fields.
         *
         * @param element The element being deserialized
         * @param value   The simple string value represented by element, if meaningful for this TypeMapping; otherwise "included"
         * @return A TypedReference to the newly deserialized object
         * @throws InvalidPolicyStreamException if the element cannot be deserialized
         */
        protected TypedReference createObject(Element element, String value) throws InvalidPolicyStreamException {
            if (value == null)
                throw new InvalidPolicyStreamException("Null values not supported");
            TypedReference tr = new TypedReference(clazz, stringToObject(value), element.getNodeName());
            if (tr.target != null)
                populateObject(tr, element);
            return tr;
        }

        /**
         * Do any extra work that might be requried by this new object deserialized from this element.
         *
         * @param object the newly-created object that needs to have properties filled in from source. target may not be null
         * @param source the element from which object is being created
         */
        protected void populateObject(TypedReference object, Element source) throws InvalidPolicyStreamException {
            // no action required for simple types
        }

        protected TypedReference thawAnonymous(Element source) throws InvalidPolicyStreamException {
            throw new IllegalArgumentException("BasicTypeMapping supports only Named format");
        }

        /**
         * This method is responsible for constructing the newly deserialized object, but doesn't populate its fields.
         * For simple types, perform the reverse of objectToString.
         */
        protected Object stringToObject(String value) throws InvalidPolicyStreamException {
            if (stringConstructor == null)
                throw new InvalidPolicyStreamException("No stringToObject defined for TypeMapping for class " + clazz);
            try {
                return stringConstructor.newInstance(new Object[]{value});
            } catch (Exception e) {
                throw new InvalidPolicyStreamException("Unable to convert string into " + clazz, e);
            }
        };
    }

    static class ComplexTypeMapping extends BasicTypeMapping {
        protected Constructor constructor; // default, no-arguments constructor for this type

        ComplexTypeMapping(Class clazz, String externalName) {
            super(clazz, externalName);
            try {
                // Try to find the default constructor
                constructor = clazz.getConstructor(new Class[0]);
            } catch (Exception e) {
                constructor = null;
            }
        }

        ComplexTypeMapping(Class clazz, String externalName, Constructor constructor) {
            super(clazz, externalName);
            this.constructor = constructor;
        }

        protected Element freezeAnonymous(TypedReference object, Element container) {
            Element elm = container.getOwnerDocument().createElement(externalName);
            if (object.target == null)
                throw new InvalidPolicyTreeException("Null objects may not be serialized in Anonymous format");
            populateElement(elm, object);
            return elm;
        }

        protected Object stringToObject(String value) throws InvalidPolicyStreamException {
            if (!"included".equals(value))
                throw new InvalidPolicyStreamException("Complex type's value must be \"included\" if it is non-null");
            if (constructor == null)
                throw new InvalidPolicyStreamException("No default constructor known for class " + clazz);
            try {
                return constructor.newInstance(new Object[0]);
            } catch (Exception e) {
                throw new InvalidPolicyStreamException("Unable to construct class " + clazz, e);
            }
        }

        protected TypedReference thawAnonymous(Element source) throws InvalidPolicyStreamException {
            return createObject(source, "included");
        }

        protected String objectToString(Object value) throws InvalidPolicyTreeException {
            return value == null ? "null" : "included";
        }
    }

    static class BeanTypeMapping extends ComplexTypeMapping {
        BeanTypeMapping(Class clazz, String externalName) {
            super(clazz, externalName);
        }

        protected void populateElement(Element element, TypedReference object) {
            try {
                emitBeanProperties(object.target, element);
            } catch (InvocationTargetException e) {
                throw new InvalidPolicyTreeException(e);
            } catch (IllegalAccessException e) {
                throw new InvalidPolicyTreeException(e);
            }
        }

        protected void populateObject(TypedReference object, Element source) throws InvalidPolicyStreamException {
            Object target = object.target;

            // gather properties
            List properties = WspConstants.getChildElements(source);
            for (Iterator i = properties.iterator(); i.hasNext();) {
                Element kid = (Element)i.next();
                String parm = kid.getLocalName();

                WspConstants.TypedReference thawedReference = typeMappingObject.thaw(kid);
                Object thawed = thawedReference.target;
                Class thawedType = thawedReference.type;

                //System.out.println("Thawing: " + name + ".set" + parm + "((" + thawedType.getName() + ") " + thawed + ")");
                callSetMethod(target, parm, thawed, thawedType);
            }
        }

        private void callSetMethod(Object target, String parm, Object parameter, Class tryType) throws InvalidPolicyStreamException {
            String methodName = "set" + parm;
            try {
                Method setter = null;
                do {
                    try {
                        setter = target.getClass().getMethod(methodName, new Class[]{tryType});
                    } catch (NoSuchMethodException e) {
                        tryType = tryType.getSuperclass();
                        if (tryType == null) // out of superclasses; buck stops here
                            throw new InvalidPolicyStreamException("Policy contains reference to unsupported object property " + parm, e);
                    }
                } while (setter == null);

                setter.invoke(target, new Object[]{parameter});
            } catch (SecurityException e) {
                throw new InvalidPolicyStreamException("Policy contains reference to unsupported object property " + parm, e);
            } catch (IllegalAccessException e) {
                throw new InvalidPolicyStreamException("Policy contains reference to unsupported object property " + parm, e);
            } catch (InvocationTargetException e) {
                throw new InvalidPolicyStreamException("Policy contains invalid object property " + parm, e);
            }
        }
    }

    static class ObjectTypeMapping extends BasicTypeMapping {
        ObjectTypeMapping(Class clazz, String externalName) {
            super(clazz, externalName);
        }

        public Element freeze(TypedReference object, Element container) {
            // Before delegating to generic Bean serialize, check if there's a serializer
            // specific to this concrete type.
            if (object.target != null) {
                Class c = object.target.getClass();
                if (c != Object.class) {
                    TypeMapping tm = findTypeMappingByClass(c);
                    if (tm != null)
                        return tm.freeze(new TypedReference(c, object.target, object.name), container);

                    throw new InvalidPolicyTreeException("Don't know how to safely serialize instance of class " + c);
                }
            }

            // The target is either null or a concrete instance of Object (and not some subclass), so this is safe
            return super.freeze(object, container);
        }

        public TypedReference thaw(Element source) throws InvalidPolicyStreamException {
            if (!POLICY_NS.equals(source.getNamespaceURI()))
                throw new InvalidPolicyStreamException("Policy contains node \"" + source.getNodeName() +
                  "\" with unrecognized namespace URI \"" + source.getNamespaceURI() + "\"");

            NamedNodeMap attrs = source.getAttributes();
            if (attrs.getLength() == 0) {
                // Appears to be an anonymous element  <Typename>..</Typename>
                TypeMapping tm = WspConstants.findTypeMappingByExternalName(source.getNodeName());
                if (tm == null)
                    throw new InvalidPolicyStreamException("Unrecognized anonymous element " + source.getNodeName());
                return tm.thaw(source);
            }

            // Nope, must be a named element   <Refname typenameValue="..."/>
            if (attrs.getLength() != 1)
                throw new InvalidPolicyStreamException("Policy contains a " + source.getNodeName() +
                  " element that doesn't have exactly one attribute");
            Node attr = attrs.item(0);
            String typeName = attr.getLocalName();
            boolean isNull = false;
            if (typeName.endsWith("Null") && typeName.length() > 4) {
                typeName = typeName.substring(0, typeName.length() - 4);
                isNull = true;
            }

            if (externalName.equals(typeName)) {
                // This is describing an actual Object, and not some subclass
                return new TypedReference(clazz, isNull ? null : new Object(), source.getNodeName());
            }

            TypeMapping tm = findTypeMappingByExternalName(typeName);
            if (tm == null)
                throw new InvalidPolicyStreamException("Policy contains unrecognized type name \"" + source.getNodeName() + "\"");

            return tm.thaw(source);
        }
    }

    private static class ArrayTypeMapping extends ComplexTypeMapping {
        private final Object[] prototype;

        public ArrayTypeMapping(Object[] prototype, String externalName) {
            super(prototype.getClass(), externalName);
            this.prototype = prototype;
        }

        protected void populateElement(Element newElement, TypedReference object) throws InvalidPolicyTreeException {
            Object[] array = (Object[])object.target;
            for (int i = 0; i < array.length; i++) {
                Object o = array[i];
                typeMappingObject.freeze(new TypedReference(Object.class, o, "item"), newElement);
            }
        }

        protected TypedReference createObject(Element element, String value) throws InvalidPolicyStreamException {
            List objects = new ArrayList();
            List arrayElements = getChildElements(element, "item");
            for (Iterator i = arrayElements.iterator(); i.hasNext();) {
                Element kidElement = (Element)i.next();
                TypedReference ktr = typeMappingObject.thaw(kidElement);
                objects.add(ktr.target);
            }
            try {
                return new TypedReference(clazz, objects.toArray(prototype), element.getNodeName());
            } catch (ArrayStoreException e) {
                throw new InvalidPolicyStreamException("Array item with incompatible type", e);
            }
        }
    }

    static class AssertionMapping extends BeanTypeMapping {
        Assertion source;

        AssertionMapping(Assertion a, String externalName) {
            super(a.getClass(), externalName);
            this.source = a;
        }
    }

    static class CompositeAssertionMapping extends AssertionMapping {
        CompositeAssertion source;

        CompositeAssertionMapping(CompositeAssertion a, String externalName) {
            super(a, externalName);
        }

        protected void populateElement(Element element, TypedReference object) throws InvalidPolicyTreeException {
            // Do not serialize any properties of the CompositeAssertion itself: shouldn't be any, and it'll include kid list
            // NO super.populateElement(element, object);
            CompositeAssertion cass = (CompositeAssertion)object.target;

            List kids = cass.getChildren();
            for (Iterator i = kids.iterator(); i.hasNext();) {
                Assertion kid = (Assertion)i.next();
                if (kid == null)
                    throw new InvalidPolicyTreeException("Unable to serialize a null assertion");
                Class kidClass = kid.getClass();
                TypeMapping tm = findTypeMappingByClass(kidClass);
                if (tm == null)
                    throw new InvalidPolicyTreeException("No TypeMapping found for class " + kidClass);
                tm.freeze(new TypedReference(kidClass, kid), element);
            }
        }

        protected void populateObject(TypedReference object, Element source) throws InvalidPolicyStreamException {
            // Do not deserialize any properties of the CompositeAssertion itself: shouldn't be any, and it'll include kid list
            // NO super.populateObject(object, source);
            CompositeAssertion cass = (CompositeAssertion)object.target;

            // gather children
            List convertedKids = new LinkedList();
            List kids = WspConstants.getChildElements(source);
            for (Iterator i = kids.iterator(); i.hasNext();) {
                Element kidNode = (Element)i.next();
                TypedReference tr = typeMappingObject.thaw(kidNode);
                if (tr.target == null)
                    throw new InvalidPolicyStreamException("CompositeAssertion " + cass + " has null child");
                convertedKids.add(tr.target);
            }
            cass.setChildren(convertedKids);
        }
    }

    static String[] ignoreAssertionProperties = {
        "Parent", // Parent links will be reconstructed when tree is deserialized
        "Copy", // getCopy() is a utility method of Assertion; not a property
        "Class", // getClass() is a method of Object; not a property
        "Instance", // getInstance() is used by singleton assertions; not a property
        "Path", // getPath() is a utility method of Assertion; not a property
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
    static final TypeMapping typeMappingArray = new ArrayTypeMapping(new Object[0], "arrayValue");

    // This is utterly grotesque, but it's all Java's fault.  Please close eyes here
    static final Constructor hashMapConstructor;

    static {
        try {
            hashMapConstructor = HashMap.class.getConstructor(new Class[0]);
        } catch (Exception e) {
            throw new LinkageError("Couldn't find HashMap's default constructor");
        }
    }
    // You may now open your eyes

    static final TypeMapping typeMappingMap = new ComplexTypeMapping(Map.class, "mapValue", hashMapConstructor) {
        protected void populateElement(Element newElement, TypedReference object) {
            Map map = (Map)object.target;
            Set entries = map.entrySet();
            for (Iterator i = entries.iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                Object key = entry.getKey();
                if (key == null)
                    throw new InvalidPolicyTreeException("Maps with null keys are not currently permitted within a policy");
                if (!(key instanceof String))
                    throw new InvalidPolicyTreeException("Maps with non-string keys are not currently permitted within a policy");
                Object value = entry.getValue();
                if (value != null && !(value instanceof String))
                    throw new InvalidPolicyTreeException("Maps with non-string values are not currently permitted within a policy");
                Element entryElement = newElement.getOwnerDocument().createElement("entry");
                newElement.appendChild(entryElement);
                typeMappingString.freeze(new TypedReference(String.class, key, "key"), entryElement);
                typeMappingString.freeze(new TypedReference(String.class, value, "value"), entryElement);
            }
        }

        protected void populateObject(TypedReference object, Element source) throws InvalidPolicyStreamException {
            Map map = (Map)object.target;
            List entryElements = getChildElements(source, "entry");
            for (Iterator i = entryElements.iterator(); i.hasNext();) {
                Element element = (Element)i.next();
                List keyValueElements = getChildElements(element);
                if (keyValueElements.size() != 2)
                    throw new InvalidPolicyStreamException("Map entry does not have exactly two child elements (key and value)");
                Element keyElement = (Element)keyValueElements.get(0);
                if (keyElement == null || keyElement.getNodeType() != Node.ELEMENT_NODE || !"key".equals(keyElement.getLocalName()))
                    throw new InvalidPolicyStreamException("Map entry first child element is not a key element");
                Element valueElement = (Element)keyValueElements.get(1);
                if (valueElement == null || valueElement.getNodeType() != Node.ELEMENT_NODE || !"value".equals(valueElement.getLocalName()))
                    throw new InvalidPolicyStreamException("Map entry last child element is not a value element");

                TypedReference ktr = typeMappingObject.thaw(keyElement);
                if (!String.class.equals(ktr.type))
                    throw new InvalidPolicyStreamException("Maps with non-string keys are not currently permitted within a policy");
                if (ktr.target == null)
                    throw new InvalidPolicyStreamException("Maps with null keys are not currently permitted within a policy");
                String key = (String)ktr.target;

                TypedReference vtr = typeMappingObject.thaw(valueElement);
                if (!String.class.equals(vtr.type))
                    throw new InvalidPolicyStreamException("Maps with non-string values are not currently permitted within a policy");
                String value = (String)vtr.target;
                map.put(key, value);
            }
        }
    };

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

        new BeanTypeMapping(TimeOfDayRange.class, "timeOfDayRange"),
        new BeanTypeMapping(TimeOfDay.class, "timeOfDay"),

        // Typesafe enumerations
        new BasicTypeMapping(SslAssertion.Option.class, "optionValue") {
            protected String objectToString(Object in) {
                return ((SslAssertion.Option)in).getKeyName();
            }

            protected Object stringToObject(String in) {
                return SslAssertion.Option.forKeyName(in);
            }
        },

        // Container types
        typeMappingArray,
        typeMappingMap,

        // Composite assertions
        new CompositeAssertionMapping(new OneOrMoreAssertion(), "OneOrMore"),
        new CompositeAssertionMapping(new AllAssertion(), "All"),
        new CompositeAssertionMapping(new ExactlyOneAssertion(), "ExactlyOne"),

        // Leaf assertions
        new AssertionMapping(new HttpBasic(), "HttpBasic"),
        new AssertionMapping(new HttpClientCert(), "HttpClientCert"),
        new AssertionMapping(new HttpDigest(), "HttpDigest"),
        new AssertionMapping(new WssBasic(), "WssBasic"),
        new AssertionMapping(new WssDigest(), "WssDigest"),
        new AssertionMapping(new FalseAssertion(), "FalseAssertion"),
        new AssertionMapping(new SslAssertion(), "SslAssertion"),
        new AssertionMapping(new JmsRoutingAssertion(), "JmsRoutingAssertion"),
        new AssertionMapping(new HttpRoutingAssertion(), "HttpRoutingAssertion"),
        new AssertionMapping(new HttpRoutingAssertion(), "RoutingAssertion"), // backwards compatibility
        new AssertionMapping(new TrueAssertion(), "TrueAssertion"),
        new AssertionMapping(new MemberOfGroup(), "MemberOfGroup"),
        new AssertionMapping(new SpecificUser(), "SpecificUser"),
        new AssertionMapping(new RequestWssIntegrity(), "RequestWssIntegrity"),
        new AssertionMapping(new RequestWssConfidentiality(), "RequestWssConfidentiality"),
        new AssertionMapping(new ResponseWssIntegrity(), "ResponseWssIntegrity"),
        new AssertionMapping(new ResponseWssConfidentiality(), "ResponseWssConfidentiality"),
        new AssertionMapping(new RequestWssX509Cert(), "RequestWssX509Cert"),
        new AssertionMapping(new RequestWssReplayProtection(), "RequestWssReplayProtection"),
        new AssertionMapping(new SamlSecurity(), "SamlSecurity"),
        new AssertionMapping(new RequestXpathAssertion(), "RequestXpathAssertion"),
        new AssertionMapping(new ResponseXpathAssertion(), "ResponseXpathAssertion"),
        new AssertionMapping(new SchemaValidation(), "SchemaValidation"),
        new AssertionMapping(new XslTransformation(), "XslTransformation"),
        new AssertionMapping(new TimeRange(), "TimeRange"),
        new AssertionMapping(new RemoteIpRange(), "RemoteIpAddressRange"),
        new SerializedJavaClassMapping(CustomAssertionHolder.class, "CustomAssertion"),
        new AssertionMapping(new UnknownAssertion(), "UnknownAssertion"),

        // Special types
        new BeanTypeMapping(XpathExpression.class, "xpathExpressionValue"),

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
     * Add the properties of a Bean style object to its already-created node in a document.
     *
     * @param bean    The bean to serialize
     * @param element The assertion's already-created node, to which we will appendChild() each property we find.
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    static void emitBeanProperties(Object bean, Element element)
      throws InvocationTargetException, IllegalAccessException {
        Class ac = bean.getClass();
        Map setters = new HashMap();
        Map getters = new HashMap();
        Method[] methods = ac.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String name = method.getName();
            if (name.startsWith("is") && name.length() > 2 && method.getReturnType().equals(boolean.class))
                getters.put(name.substring(2), method);
            else if (name.startsWith("get") && name.length() > 3)
                getters.put(name.substring(3), method);
            else if (name.startsWith("set") && name.length() > 3)
                setters.put(name.substring(3) + ":" + method.getParameterTypes()[0], method);
        }
        for (Iterator i = getters.keySet().iterator(); i.hasNext();) {
            String parm = (String)i.next();
            if (isIgnorableProperty(parm))
                continue;
            Method getter = (Method)getters.get(parm);
            if (getter == null)
                throw new InvalidPolicyTreeException("Internal error"); // can't happen

            if (Modifier.isStatic(getter.getModifiers())) { // ignore statics
                continue;
            }

            Method setter = (Method)setters.get(parm + ":" + getter.getReturnType());
            if (setter == null)
                throw new InvalidPolicyTreeException("WspWriter: Warning: class " + bean.getClass() + ": no setter found for parameter " + parm);
            Class returnType = getter.getReturnType();
            if (!setter.getParameterTypes()[0].equals(returnType))
                throw new InvalidPolicyTreeException("class has getter and setter for " + parm + " which disagree about its type");
            TypeMapping tm = findTypeMappingByClass(returnType);
            if (tm == null)
                throw new InvalidPolicyTreeException("class " + bean.getClass() + " has property \"" + parm + "\" with unsupported type " + returnType);
            Object value = getter.invoke(bean, new Object[0]);
            TypedReference tr = new TypedReference(returnType, value, parm);
            tm.freeze(tr, element);
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
    static TypedReference thawElement(Element source) throws InvalidPolicyStreamException {
        return typeMappingObject.thaw(source);
    }
}
