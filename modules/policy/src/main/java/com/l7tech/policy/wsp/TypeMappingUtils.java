/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
     * @param type The class to look up
     * @param writer  WspWriter context we are operating in, for locating context-specific type mappings,
     *                or null to find only global or Assertion-specific type mappings.
     * @return The TypeMapping for this class, or null if not found.
     */
    public static TypeMapping findTypeMappingByClass(Type type, WspWriter writer) {
        final TypeMapping[] tms = WspConstants.typeMappings;
        final String targetVersion = writer!=null ? writer.getTargetVersion() : null;
        TypeMapping tm = findTypeMapping(type, Arrays.asList(tms), targetVersion);
        if (tm != null) return tm;

        // Check for assertion
        if (type instanceof Class && Assertion.class.isAssignableFrom((Class)type)) {
            Class clazz = (Class)type;
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
            if (tmf != null){
                return tmf.getTypeMapping(type, writer.getTargetVersion());
            }
        }

        return null;
    }

    /**
     * Find the Class for the supplied Type
     * @param type the Type we want Class information for. Type's implementation must be Class or an implementation of
     * ParameterizedType
     * @return The Class which represents this type, never null
     */
    public static Class getClassForType(Type type){
        if(type == null) throw new NullPointerException("type cannot be null");
        //Not assuming below that if the type is not a Class that it must be a ParameterizedType
        if(!((type instanceof Class) || (type instanceof ParameterizedType)))
            throw new IllegalArgumentException("Type Implementation must either be a Class " +
                    "or an implementation of ParameterizedType");

        return (type instanceof Class) ? (Class) type : (Class) ((ParameterizedType) type).getRawType();
    }

    /**
     * Find the TypeMapping for a type in the supplied Collection
     * @param type the Type we want to find a TypeMapping for. Cannot be null
     * @param typeMappings the Collection of TypeMappings to search. Cannot be null
     * @param version the target software version (may be null)
     * @return TypeMapping which can map the param type, or null if no matching TypeMapping is found
     */
    static TypeMapping findTypeMapping(Type type, Collection<TypeMapping> typeMappings, String version){
        if(type == null) throw new NullPointerException("type cannot be null");
        if(typeMappings == null) throw new NullPointerException("typeMappings cannot be null");

        for (TypeMapping typeMapping : typeMappings) {
            if(type instanceof ParameterizedType && typeMapping instanceof ParameterizedMapping){
                ParameterizedMapping parameterizedMapping = (ParameterizedMapping)typeMapping;
                ParameterizedType parameterizedType = (ParameterizedType)type;

                if(!typeMapping.getMappedClass().equals(parameterizedType.getRawType())) continue;

                if (!versionMatch(version, typeMapping.getSinceVersion())) continue;

                Type [] paramTypeTypes = parameterizedType.getActualTypeArguments();
                Type [] paramMapTypes = parameterizedMapping.getMappedObjectsParameterizedClasses();

                if(paramTypeTypes.length != paramMapTypes.length) continue;

                //both arrays are the same length. Can interate over both with the index from one
                boolean paramsTypesDontMatch = false;
                for(int i = 0; i < paramTypeTypes.length; i++){
                    if(!paramTypeTypes[i].equals(paramMapTypes[i])) paramsTypesDontMatch = true;
                }
                if(paramsTypesDontMatch) continue;

                return typeMapping;

            }else{
                Class clazz = getClassForType(type);
                if (typeMapping.getMappedClass().equals(clazz) && versionMatch(version, typeMapping.getSinceVersion())) {
                    return typeMapping;
                }
            }
        }
        return null;
    }

    /**
     * Check if the given target version is greater than or equal to the since version.
     *
     * Version numbers are expected to be in the form "4.3" or "5.1.0", any extra version
     * digits are ignored, so "1.2.3.4" is the same as "1.2.3" as far as this test is concerned.
     */
    static boolean versionMatch( final String targetVersion, final String sinceVersion ) {
        boolean match = false;

        if ( targetVersion == null || sinceVersion == null ) {
            match = true;
        } else if ( targetVersion.equals(sinceVersion) ) {
            match = true;
        } else {
            int[] targetVersionValues = parseVersion( targetVersion );
            int[] sinceVersionValues = parseVersion( sinceVersion );

            if ( (targetVersionValues[0] > sinceVersionValues[0]) ||
                 (targetVersionValues[0] == sinceVersionValues[0] && targetVersionValues[1] > sinceVersionValues[1]) ||
                 (targetVersionValues[0] == sinceVersionValues[0] && targetVersionValues[1] == sinceVersionValues[1] && targetVersionValues[2] >= sinceVersionValues[2]) ) {
                match = true;
            }
        }

        return match;
    }

    /**
     * Parse a version number in to a int array of length 3 ("5.1.0").
     */
    private static int[] parseVersion( final String versionString ) {
        int[] version = new int[3];

        String[] versionStrings = versionString.split("\\.");
        for ( int i=0; i<versionStrings.length && i<version.length; i++ ) {
            try {
                version[i] = Integer.parseInt( versionStrings[i] );
            } catch ( NumberFormatException nfe ) {
                // 0
            }
        }

        return version;
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
        final boolean doWorkaround = SyspropUtil.getBoolean("com.l7tech.compat.enableInvokeMethodWorkaround", false);
        if (!doWorkaround) {
            return method.invoke(targetObject, args);
        }

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
