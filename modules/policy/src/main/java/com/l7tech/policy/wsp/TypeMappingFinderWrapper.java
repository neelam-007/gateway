package com.l7tech.policy.wsp;

import java.util.Collection;
import java.util.Collections;
import java.lang.reflect.Type;

/**
 * A TypeMappingFinder that delegates searches to one or more other TypeMappingFinder instances.
 *
 */
public class TypeMappingFinderWrapper implements TypeMappingFinder {
    private final TypeMappingFinder delegate;
    private final Collection<TypeMappingFinder> delegates;

    /**
     * Create a wrapper that delegates to the specified collection of delegate finders.
     *
     * @param delegates the collection of delegates to iterate for each query.
     *                  This collection may be modified after the fact, but caller
     *                  is responsible for any needed synchronization of the Collection,
     *                  and for ensuring that no thread is currently in the process of iterating it
     *                  via this wrapper instance when the modification is done.
     *                  <p/>
     *                  This list may contain null entries, which will be ignored.
     */
    public TypeMappingFinderWrapper(Collection<TypeMappingFinder> delegates) {
        this.delegate = null;
        this.delegates = delegates;
    }

    /**
     * Create a wrapper that delegates to the specific single delegate.
     *
     * @param delegate  the delegate.  Required.
     */
    public TypeMappingFinderWrapper(TypeMappingFinder delegate) {
        this.delegate = delegate;
        this.delegates = Collections.emptyList();
    }

    @Override
    public TypeMapping getTypeMapping(String externalName) {
        if (delegate != null)
            return delegate.getTypeMapping(externalName);

        for (TypeMappingFinder tmf : delegates) {
            if (tmf == null) continue;
            TypeMapping typeMapping = tmf.getTypeMapping(externalName);
            if (typeMapping != null)
                return typeMapping;
        }

        return null;
    }

    public TypeMapping getTypeMapping(Type unrecognizedType, String version) {
        if (delegate != null)
            return delegate.getTypeMapping(unrecognizedType, version);

        for (TypeMappingFinder tmf : delegates) {
            if (tmf == null) continue;
            TypeMapping typeMapping = tmf.getTypeMapping(unrecognizedType, version);
            if (typeMapping != null)
                return typeMapping;
        }

        return null;
    }
}
