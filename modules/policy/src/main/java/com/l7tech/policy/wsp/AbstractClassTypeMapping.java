/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.wsp;

import org.w3c.dom.Element;

/**
 * Handles assertion properties that are typed as an abstract class, by chaining freeze/thaw
 * functionality to TypeMappings that are also registered for the concrete classes.
 */
public class AbstractClassTypeMapping extends BasicTypeMapping {
    public AbstractClassTypeMapping(Class abstractClass, String name) {
        super(abstractClass, name);
    }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        Object target = object.target;
        if (target == null) return super.freeze(wspWriter, object, container);
        Class concreteClass = target.getClass();
        TypeMapping concreteMapping = TypeMappingUtils.findTypeMappingByClass(concreteClass, wspWriter);
        if (concreteMapping == null) throw new IllegalArgumentException("No TypeMapping found for " + concreteClass.getName());
        return concreteMapping.freeze(wspWriter, new TypedReference(concreteClass, object.target, object.name), container);
    }

    protected TypedReference thawAnonymous(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        throw new InvalidPolicyStreamException("Anonymous unsupported for this type");
    }

    protected TypedReference doThawNamedNotNull(Element source, WspVisitor visitor, boolean recursing, String typeName, String value) throws InvalidPolicyStreamException {
        TypeMapping subclassMapping = TypeMappingUtils.findTypeMappingByExternalName(typeName, visitor.getTypeMappingFinder());
        if (subclassMapping == null) throw new InvalidPolicyStreamException("No TypeMapping found for " + typeName);
        if (subclassMapping instanceof AbstractClassTypeMapping) throw new InvalidPolicyStreamException("TypeMapping for concrete class is an AbstractClassTypeMapping");
        return subclassMapping.thaw(source, visitor);
    }
}
