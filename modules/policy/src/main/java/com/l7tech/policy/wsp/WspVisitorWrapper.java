package com.l7tech.policy.wsp;

import org.w3c.dom.Element;

/**
 *
 */
class WspVisitorWrapper implements WspVisitor {
    private final WspVisitor delegate;

    public WspVisitorWrapper(WspVisitor delegate) {
        this.delegate = delegate;
    }

    public void unknownProperty(Element originalObject,
                                Element problematicParameter,
                                Object deserializedObject,
                                String parameterName,
                                TypedReference parameterValue,
                                Throwable problemEncountered)
            throws InvalidPolicyStreamException
    {
        delegate.unknownProperty(originalObject,
                                 problematicParameter,
                                 deserializedObject,
                                 parameterName,
                                 parameterValue,
                                 problemEncountered);
    }

    public Element invalidElement(Element problematicElement,
                                  Exception problemEncountered)
            throws InvalidPolicyStreamException
    {
        return delegate.invalidElement(problematicElement, problemEncountered);
    }

    public TypeMappingFinder getTypeMappingFinder() {
        return delegate.getTypeMappingFinder();
    }
}
