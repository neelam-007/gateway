/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import org.w3c.dom.Element;

import java.util.Arrays;

/**
 * Superclass for TypeMappings that know how to serialize policy assertions into a policy XML document.
 */
public class AssertionMapping extends BeanTypeMapping {
    private final Assertion prototype;

    public AssertionMapping(Assertion a, String externalName) {
        this( a, externalName, null );
    }

    public AssertionMapping(Assertion a, String externalName, String version) {
        super(a.getClass(), externalName, version);
        prototype = newPrototype(a.getClass());
    }

    public AssertionMapping(Class<? extends Assertion> c, String externalName) {
        super(c, externalName);
        prototype = newPrototype(c);
    }


    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        TypeMappingFinder subtypeFinder = null;
        if (object.target instanceof Assertion) {
            Assertion assertion = (Assertion)object.target;
            subtypeFinder = (TypeMappingFinder)assertion.meta().get(AssertionMetadata.WSP_SUBTYPE_FINDER);
        }

        if (subtypeFinder != null) wspWriter.addTypeMappingFinder(subtypeFinder);
        try {
            return super.freeze(wspWriter, object, container);
        } finally {
            if (subtypeFinder != null) wspWriter.removeTypeMappingFinder(subtypeFinder);
        }
    }


    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        WspVisitor subVisitor = visitor;

        TypeMappingFinder subTmf = (TypeMappingFinder)prototype.meta().get(AssertionMetadata.WSP_SUBTYPE_FINDER);
        if (subTmf != null) {
            final TypeMappingFinderWrapper wrappedTmf =
                    new TypeMappingFinderWrapper(Arrays.asList(visitor.getTypeMappingFinder(), subTmf));            
            subVisitor = new WspVisitorWrapper(visitor) {
                public TypeMappingFinder getTypeMappingFinder() {
                    return wrappedTmf;
                }
            };
        }

        return super.thaw(source, subVisitor);
    }

    private Assertion newPrototype(Class<? extends Assertion> assclass) {
        try {
            return assclass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e); // broken mapping
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e); // broken mapping
        }
    }

    public TypeMappingFinder getSubtypeFinder() {
        AssertionMetadata meta = prototype.meta();
        TypeMappingFinder tmf = (TypeMappingFinder)meta.get(AssertionMetadata.WSP_SUBTYPE_FINDER);
        return tmf != null ? tmf : super.getSubtypeFinder();
    }
}
