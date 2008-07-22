/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility code used by some TypeMappings.
 */
public class TypeMappingUtils {
    protected static final Logger logger = Logger.getLogger(TypeMappingUtils.class.getName());

    static boolean isNullableType(Class type) {
        return !(int.class.equals(type) ||
          long.class.equals(type) ||
          boolean.class.equals(type) ||
          float.class.equals(type) ||
          double.class.equals(type) ||
          byte.class.equals(type) ||
          char.class.equals(type));
    }

    public static boolean isIgnorableProperty(String parm) {
        for (String ignoreProperty : WspConstants.ignoreAssertionProperties) {
            if (ignoreProperty.equals(parm))
                return true;
        }
        return false;
    }

    /**
     * Find a TypeMapping capable of serializing the specified class.
     *
     * @param clazz The class to look up
     * @param writer  WspWriter context we are operating in, for locating context-specific type mappings,
     *                or null to find only global or Assertion-specific type mappings.
     * @return The TypeMapping for this class, or null if not found.
     */
    public static TypeMapping findTypeMappingByClass(Class clazz, WspWriter writer) {
        final TypeMapping[] tms = WspConstants.typeMappings;
        for (TypeMapping typeMapping : tms) {
            if (typeMapping.getMappedClass().equals(clazz))
                return typeMapping;
        }

        // Check for assertion
        if (Assertion.class.isAssignableFrom(clazz)) {
            try {
                Assertion prototype = (Assertion)clazz.newInstance();
                return (TypeMapping) prototype.meta().get(AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE);
            } catch (InstantiationException e) {
                logger.log(Level.WARNING, "Unable to create instance of assertion: " + clazz.getName() + ": " + ExceptionUtils.getMessage(e), e);
            } catch (IllegalAccessException e) {
                logger.log(Level.WARNING, "Unable to create instance of assertion: " + clazz.getName() + ": " + ExceptionUtils.getMessage(e), e);
            }
        }

        // Check for globals
        if (writer != null) {
            TypeMappingFinder tmf = writer.getTypeMappingFinder();
            if (tmf != null)
                return tmf.getTypeMapping(clazz);
        }

        return null;
    }

    /**
     * Find a TypeMapping corresponding to the specified external name (ie, "OneOrMore" or "mapValue").
     *
     * @param typeName The external name to look up
     * @param fallback  TypeMappingFinder to look in if the specified typeName is not recognized as a builtin TypeMapping,
     *                  or null to perform no fallback.
     * @return The TypeMapping for this external name, or null if not found.
     */
    static TypeMapping findTypeMappingByExternalName(String typeName, TypeMappingFinder fallback) {
        for (TypeMapping typeMapping : WspConstants.typeMappings) {
            if (typeMapping.getExternalName().equals(typeName))
                return typeMapping;
        }
        for (TypeMapping typeMapping : WspConstants.readOnlyTypeMappings) {
            if (typeMapping.getExternalName().equals(typeName))
                return typeMapping;
        }
        if (fallback != null)
            return fallback.getTypeMapping(typeName);
        return null;
    }

    /**
     * Get a list of all immediate child nodes of the given node that are of type ELEMENT.
     *
     * @param node the node to check
     * @return a List of Element objects
     */
    static List<Element> getChildElements(Node node) {
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
    static List<Element> getChildElements(Node node, String name) {
        NodeList kidNodes = node.getChildNodes();
        List<Element> kidElements = new ArrayList<Element>();
        for (int i = 0; i < kidNodes.getLength(); ++i) {
            Node n = kidNodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && (name == null || name.equals(n.getLocalName())))
                kidElements.add((Element)n);
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
     * @throws IllegalAccessException see contract in {@link java.lang.reflect.Method#invoke(Object, Object[])}
     * @throws java.lang.reflect.InvocationTargetException see contract in {@link java.lang.reflect.Method#invoke(Object, Object[])}
     */
    static Object invokeMethod(Method method, Object targetObject, final Object[] args)
      throws IllegalAccessException, InvocationTargetException {
        boolean accessible = method.isAccessible();
        boolean accessibilityChanged = false;
        try {
            if (!accessible) {
                try {
                    method.setAccessible(true);
                    accessibilityChanged = true;
                } catch (SecurityException e) {
                    // Unable to open up access.  Try the invocation anyway.
                }
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
     * @param visitor  WspVisitor for handling unrecognized properties or subelements
     * @return A TypedReference with information about the object.  The name and/or target might be null.
     * @throws InvalidPolicyStreamException if we were unable to recover an object from this Element
     */
    static TypedReference thawElement(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        return WspConstants.typeMappingObject.thaw(source, visitor);
    }

    /**
     * Find the typeValue or typeValueNull attribute, if this element has one.
     *
     * @param source the element to examine
     * @return a String such as "typeValue" or "typeValueNull" if this attribute is a named reference, or null
     *         if no type name was found and hence the attribute should be assumed to be an anonymous reference.
     * @throws InvalidPolicyStreamException if policy parsing can't continue
     */
    static String findTypeName(Element source) throws InvalidPolicyStreamException {
        // Only L7 elements can be named references
        if (!WspConstants.L7_POLICY_NAMESPACE_LIST.contains(source.getNamespaceURI()))
            return source.getLocalName();

        NamedNodeMap attrs = source.getAttributes();
        int numAttr = attrs.getLength();
        String foundTypeName = null;
        for (int i = 0; i < numAttr; ++i) {
            Node attr = attrs.item(i);
            if ("xmlns".equals(attr.getPrefix())) continue; // Ignore namespace decls
            if (WspConstants.POLICY_NAMESPACE_LIST.contains(attr.getNamespaceURI())) continue; // Ignore WSP attributes
            String typeName = attr.getLocalName();
            if (typeName == null || typeName.length() < 1) typeName = attr.getNodeName();
            if (typeName == null || typeName.length() < 1) throw new RuntimeException("Policy contains an attribute with no LocalName or NodeName"); // can't happen
            if ("xmlns".equals(typeName)) continue; // Ignore namespace decls
            if (foundTypeName != null) throw new InvalidPolicyStreamException("Policy contains an element '" + source.getNodeName() + "' with more than one non-xmlns attribute");
            foundTypeName = typeName;
        }
        return foundTypeName;
    }
}
