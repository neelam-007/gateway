/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * TypeMapping that knows how to map any Object to a policy XML.  When serializing any object that is some conrete
 * type other than Object, will try to locate
 * an exact-match TypeMapping for the given class and will fail if one is not found.  Similarly, will attempt
 * to deserialize any XML element for which a valid TypeMapping can be found.
 */
class ObjectTypeMapping extends BasicTypeMapping {
    ObjectTypeMapping(Class clazz, String externalName) {
        super(clazz, externalName);
    }

    public Element freeze(TypedReference object, Element container) {
        // Before delegating to generic Bean serialize, check if there's a serializer
        // specific to this concrete type.
        if (object.target != null) {
            Class c = object.target.getClass();
            if (c != Object.class) {
                TypeMapping tm = TypeMappingUtils.findTypeMappingByClass(c);
                if (tm != null)
                    return tm.freeze(new TypedReference(c, object.target, object.name), container);

                throw new InvalidPolicyTreeException("Don't know how to safely serialize instance of class " + c);
            }
        }

        // The target is either null or a concrete instance of Object (and not some subclass), so this is safe
        return super.freeze(object, container);
    }

    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        try {
            return doThaw(source, visitor, false);
        } catch (InvalidPolicyStreamException e) {
            return doThaw(visitor.invalidElement(source, e), visitor, true);
        }
    }

    private TypedReference doThaw(Element source, WspVisitor visitor, boolean recursing) throws InvalidPolicyStreamException {
        if (!WspConstants.L7_POLICY_NS.equals(source.getNamespaceURI()))
            throw new InvalidPolicyStreamException("Policy contains node \"" + source.getNodeName() +
              "\" with unrecognized namespace URI \"" + source.getNamespaceURI() + "\"");

        NamedNodeMap attrs = source.getAttributes();
        if (attrs.getLength() == 0) {
            // Appears to be an anonymous element  <Typename>..</Typename>
            TypeMapping tm = TypeMappingUtils.findTypeMappingByExternalName(source.getNodeName());
            if (tm == null) {
                final InvalidPolicyStreamException e = new InvalidPolicyStreamException("Unrecognized anonymous element " + source.getNodeName());
                if (recursing) throw e;
                final Element newSource = visitor.invalidElement(source, e);

                return doThaw(newSource, visitor, true);
            }
            return tm.thaw(source, visitor);
        }

        // Nope, must be a named element   <Refname typenameValue="..."/>
        if (attrs.getLength() != 1) {
            final InvalidPolicyStreamException e = new BadAttributeCountException("Policy contains a " + source.getNodeName() +
                              " element that doesn't have exactly one attribute");
            if (recursing) throw e;
            return doThaw(visitor.invalidElement(source, e), visitor, true);
        }
        Node attr = attrs.item(0);
        String typeName = attr.getLocalName();
        if (typeName == null)
            typeName = attr.getNodeName();
        if (typeName == null)
            throw new RuntimeException("Policy contains an attribute with a null LocalName");
        boolean isNull = false;
        if (typeName.endsWith("Null") && typeName.length() > 4) {
            typeName = typeName.substring(0, typeName.length() - 4);
            isNull = true;
        }

        if (externalName.equals(typeName)) {
            // This is describing an actual Object, and not some subclass
            return new TypedReference(clazz, isNull ? null : new Object(), source.getNodeName());
        }

        TypeMapping tm = TypeMappingUtils.findTypeMappingByExternalName(typeName);
        if (tm == null)
            throw new InvalidPolicyStreamException("Policy contains unrecognized type name \"" + source.getNodeName() + "\"");

        return tm.thaw(source, visitor);
    }
}
