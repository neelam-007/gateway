package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.ResolvedEntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResolvedEntityHeaderTranslator implements CustomEntityTranslator {

    public Entity locateEntity(@Nullable Object element, @NotNull EntityFinder entityFinder) throws FindException {
        if(element instanceof ResolvedEntityHeader)
        {
            EntityHeader header = ((ResolvedEntityHeader) element).getEntityHeader();
            return entityFinder.find(header);
        }
        return null;
    }
}
