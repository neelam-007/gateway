/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import org.w3c.dom.Element;

/**
 * Pre32TypeMapping that knows how to map any Object to a policy XML.  When serializing any object that is some conrete
 * type other than Object, will try to locate
 * an exact-match Pre32TypeMapping for the given class and will fail if one is not found.  Similarly, will attempt
 * to deserialize any XML element for which a valid Pre32TypeMapping can be found.
 */
class Pre32ObjectTypeMapping extends Pre32BasicTypeMapping {
    Pre32ObjectTypeMapping(Class clazz, String externalName) {
        super(clazz, externalName);
    }

    public Element freeze(Pre32TypedReference object, Element container) {
        // Before delegating to generic Bean serialize, check if there's a serializer
        // specific to this concrete type.
        if (object.target != null) {
            Class c = object.target.getClass();
            if (c != Object.class) {
                Pre32TypeMapping tm = Pre32TypeMappingUtils.findTypeMappingByClass(c);
                if (tm != null)
                    return tm.freeze(new Pre32TypedReference(c, object.target, object.name), container);

                throw new Pre32InvalidPolicyTreeException("Don't know how to safely serialize instance of class " + c);
            }
        }

        // The target is either null or a concrete instance of Object (and not some subclass), so this is safe
        return super.freeze(object, container);
    }

    public Pre32TypedReference thaw(Element source, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        try {
            return doThaw(source, visitor, false);
        } catch (Pre32InvalidPolicyStreamException e) {
            return doThaw(visitor.invalidElement(source, e), visitor, true);
        }
    }

    private Pre32TypedReference doThaw(Element source, Pre32WspVisitor visitor, boolean recursing) throws Pre32InvalidPolicyStreamException {
        if (!getNsUri().equals(getNsUri()))
            throw new Pre32InvalidPolicyStreamException("Policy contains node \"" + source.getLocalName() +
              "\" with unrecognized namespace URI \"" + source.getNamespaceURI() + "\"");


        // Check for a named element   <Refname typenameValue="..."/>
        String typeName = Pre32TypeMappingUtils.findTypeName(source);

        if (typeName == null) {
            // Appears to be an anonymous element  <Typename>..</Typename>
            Pre32TypeMapping tm = Pre32TypeMappingUtils.findTypeMappingByExternalName(source.getLocalName());
            if (tm == null) {
                final Pre32InvalidPolicyStreamException e = new Pre32InvalidPolicyStreamException(source.getLocalName());
                if (recursing) throw e;
                final Element newSource = visitor.invalidElement(source, e);

                return doThaw(newSource, visitor, true);
            }
            return tm.thaw(source, visitor);
        }

        boolean isNull = false;
        if (typeName.endsWith("Null") && typeName.length() > 4) {
            typeName = typeName.substring(0, typeName.length() - 4);
            isNull = true;
        }

        if (externalName.equals(typeName)) {
            // This is describing an actual Object, and not some subclass
            return new Pre32TypedReference(clazz, isNull ? null : new Object(), source.getLocalName());
        }

        Pre32TypeMapping tm = Pre32TypeMappingUtils.findTypeMappingByExternalName(typeName);
        if (tm == null)
            throw new Pre32InvalidPolicyStreamException("Policy contains unrecognized type name \"" + source.getLocalName() + "\"");

        return tm.thaw(source, visitor);
    }

}
