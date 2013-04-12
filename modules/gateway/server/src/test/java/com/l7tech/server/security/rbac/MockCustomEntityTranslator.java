package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is used by the SecuredMethodInterceptorTest.
 */
public class MockCustomEntityTranslator implements CustomEntityTranslator {

    public static class WrappedEntity {
        final Entity wrapped;

        public WrappedEntity(Entity wrapped) {
            this.wrapped = wrapped;
        }
    }

    @Override
    public Entity locateEntity(@Nullable Object element, @NotNull EntityFinder entityFinder) throws FindException {
        return ((WrappedEntity) element).wrapped;
    }
}
