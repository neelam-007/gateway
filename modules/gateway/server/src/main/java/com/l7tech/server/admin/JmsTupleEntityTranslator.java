package com.l7tech.server.admin;

import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.security.rbac.CustomEntityTranslator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entity translator to use for admin methods of {@link JmsAdmin} that check access against JMS_ENDPOINT but which
 * return collections of JMS_TUPLE.
 * <p/>
 * This translator will throw ClassCastException if the method in question includes in its returned collection
 * any element that is not assignable to JmsTuple.
 */
@SuppressWarnings("UnusedDeclaration") // Used via class name reference as @Secured customEntityTranslator
public class JmsTupleEntityTranslator implements CustomEntityTranslator {
    @Override
    public Entity locateEntity(@Nullable Object element, @NotNull EntityFinder entityFinder) throws FindException {
        return ((JmsAdmin.JmsTuple)element).getEndpoint();
    }
}
