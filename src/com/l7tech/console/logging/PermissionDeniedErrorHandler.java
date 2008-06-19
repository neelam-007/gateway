/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.logging;

import com.l7tech.common.security.rbac.PermissionDeniedException;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.WsdlUtils;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.NamedEntity;

import java.util.logging.Level;
import java.text.MessageFormat;

/**
 * Handles {@link PermissionDeniedException}s thrown from the SSG
 */
public class PermissionDeniedErrorHandler implements ErrorHandler {
    public void handle(ErrorEvent e) {
        final Throwable t = e.getThrowable();
        if (t instanceof PermissionDeniedException) {
            PermissionDeniedException pde = (PermissionDeniedException) t;
            OperationType op = pde.getOperation();
            String opname = op == null ? null : (op == OperationType.OTHER) ? pde.getOtherOperationName() : op.getName();
            EntityType type = pde.getType();
            Entity entity = pde.getEntity();
            String message;
            if (opname != null && (type != null || entity instanceof NamedEntity)) {
                String ename;
                if (entity instanceof NamedEntity) {
                    ename = ((NamedEntity)entity).getName();
                } else {
                    ename = "the " + type.getName();
                }
                message = MessageFormat.format("You do not have sufficient privileges to {0} {1}", opname, ename);
            } else {
                message = "You do not have sufficient privileges to invoke the requested operation";
            }

            e.getLogger().log(Level.INFO, message, t);
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Permission Denied", message, null);
        } else if ( ExceptionUtils.causedBy(t, WsdlUtils.WSDLFactoryNotTrustedException.class) ) {
            TopComponents.getInstance().showNoPrivilegesErrorMessage();
        } else {
            e.handle();
        }
    }
}
