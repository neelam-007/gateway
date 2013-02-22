package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface implemented by custom entity return value translators.  This is required for admin methods that need to return
 * collections of something other than Entity or EntityHeader but still need the returned collection to be filtered
 * by the RBAC interceptor.
 */
public interface CustomEntityTranslator {
    /**
     * Translate the specified element object into the corresponding entity.
     * <p/>
     * Implementations may extract the entity from the element object, or look it up from an extracted ID/GUID/Header/etc
     * using the specified EntityFinder.
     * <p/>
     * The returned entity will be used for a permission check.  If the current admin user has permission to see the
     * entity, the corresponding element will be left in the returned collection.  Otherwise, it will be filtered out.
     * <p/>
     * The CustomEntityTranslator can return null from this method to cause the corresponding element to be filtered out of the returned collection (unless the corresponding element
     * was null).
     *
     * @param element  element found within a collection returned by the annotated method.  May be null, but only if a null value was present in the returned collection.
     * @param entityFinder an entity finder to use to look up entities from the persistent store if necessary.  Never null.
     * @return a fully-populated instance of the returned Entity, suitable for permission checks.  All attributes must be populated since they may be subject of permission attribute predicates;
     *         if attributes are missing, the returned entity may be filtered out when it should be permitted to be seen.
     *         <p/>
     *         OR, null to cause the corresponding element to be omitted from the filtered collection (provided the element was not null).
     * @throws com.l7tech.objectmodel.FindException if no corresponding entity could be located.  Throwing this exception will cause the entire admin method invocation to fail.
     */
    @Nullable
    Entity locateEntity(@Nullable Object element, @NotNull EntityFinder entityFinder) throws FindException;
}
