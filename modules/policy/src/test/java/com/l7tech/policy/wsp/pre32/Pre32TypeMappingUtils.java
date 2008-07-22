/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility code used by some TypeMappings.
 */
class Pre32TypeMappingUtils {
    static boolean isNullableType(Class type) {
        return !(int.class.equals(type) ||
          long.class.equals(type) ||
          boolean.class.equals(type) ||
          float.class.equals(type) ||
          double.class.equals(type) ||
          byte.class.equals(type) ||
          char.class.equals(type));
    }

    static boolean isIgnorableProperty(String parm) {
        for (int i = 0; i < Pre32WspConstants.ignoreAssertionProperties.length; i++) {
            String ignoreProperty = Pre32WspConstants.ignoreAssertionProperties[i];
            if (ignoreProperty.equals(parm))
                return true;
        }
        return false;
    }

    /**
     * Find a Pre32TypeMapping capable of serializing the specified class.
     *
     * @param clazz The class to look up
     * @return The Pre32TypeMapping for this class, or null if not found.
     */
    static Pre32TypeMapping findTypeMappingByClass(Class clazz) {
        for (int i = 0; i < Pre32WspConstants.typeMappings.length; i++) {
            Pre32TypeMapping typeMapping = Pre32WspConstants.typeMappings[i];
            if (typeMapping.getMappedClass().equals(clazz))
                return typeMapping;
        }
        return null;
    }

    /**
     * Find a Pre32TypeMapping corresponding to the specified external name (ie, "OneOrMore" or "mapValue").
     *
     * @param typeName The external name to look up
     * @return The Pre32TypeMapping for this external name, or null if not found.
     */
    static Pre32TypeMapping findTypeMappingByExternalName(String typeName) {
        for (int i = 0; i < Pre32WspConstants.typeMappings.length; i++) {
            Pre32TypeMapping typeMapping = Pre32WspConstants.typeMappings[i];
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
     * @throws IllegalAccessException see contract in {@link java.lang.reflect.Method#invoke(Object, Object[])}
     * @throws java.lang.reflect.InvocationTargetException see contract in {@link java.lang.reflect.Method#invoke(Object, Object[])}
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
     * @return A Pre32TypedReference with information about the object.  The name and/or target might be null.
     * @throws Pre32InvalidPolicyStreamException if we were unable to recover an object from this Element
     */
    static Pre32TypedReference thawElement(Element source, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        return Pre32WspConstants.typeMappingObject.thaw(source, visitor);
    }

    /**
     * Find the typeValue or typeValueNull attribute, if this element has one.
     *
     * @param source the element to examine
     * @return a String such as "typeValue" or "typeValueNull" if this attribute is a named reference, or null
     *         if no type name was found and hence the attribute should be assumed to be an anonymous reference.
     */
    static String findTypeName(Element source) throws Pre32InvalidPolicyStreamException {
        NamedNodeMap attrs = source.getAttributes();
        int numAttr = attrs.getLength();
        String foundTypeName = null;
        for (int i = 0; i < numAttr; ++i) {
            Node attr = attrs.item(i);
            if ("xmlns".equals(attr.getPrefix())) continue; // Ignore namespace decls
            String typeName = attr.getLocalName();
            if (typeName == null || typeName.length() < 1) typeName = attr.getNodeName();
            if (typeName == null || typeName.length() < 1) throw new RuntimeException("Policy contains an attribute with no LocalName or NodeName"); // can't happen
            if ("xmlns".equals(typeName)) continue; // Ignore namespace decls
            if (foundTypeName != null) throw new Pre32InvalidPolicyStreamException("Policy contains an element '" + source.getNodeName() + "' with more than one non-xmlns attribute");
            foundTypeName = typeName;
        }
        return foundTypeName;
    }
}
