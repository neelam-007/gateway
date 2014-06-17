package com.l7tech.gateway.common.custom;

import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;

/**
 * Interface to provide access to external entity serializers registry.<br/>
 * Extract {@code CustomEntitySerializer} given entity serializer class.
 */
public interface ClassNameToEntitySerializer {
    /**
     * Return the entity {@code CustomEntitySerializer serializer} registered with the specified {@code className}
     *
     * @param className    the external entity serializer class name
     * @return entity {@code CustomEntitySerializer serializer} registered with the specified {@code className}
     * or {@code null} if the class name is not registered.
     */
    CustomEntitySerializer getSerializer(final String className);
}
