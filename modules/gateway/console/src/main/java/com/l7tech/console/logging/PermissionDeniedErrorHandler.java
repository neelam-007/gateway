/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.logging;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles {@link PermissionDeniedException}s thrown from the SSG
 */
public class PermissionDeniedErrorHandler implements ErrorHandler {
    @Override
    public void handle(ErrorEvent e) {
        final Throwable t = e.getThrowable();
        // Check for a PermissionDeniedException wrapped inside a RuntimeException which seems like a common anti-pattern
        if (t instanceof PermissionDeniedException ||
            (t != null && RuntimeException.class.equals(t.getClass()) && t.getCause() instanceof PermissionDeniedException)) {
            final PermissionDeniedException pde = ExceptionUtils.getCauseIfCausedBy( t, PermissionDeniedException.class );
            showMessageDialog(pde, e.getLogger());
        } else {
            e.handle();
        }
    }

    public static void showMessageDialog(@NotNull final PermissionDeniedException pde, @NotNull final Logger logger) {
        final OperationType op = pde.getOperation();
        final String opname = op == null ? null : (op == OperationType.OTHER) ? pde.getOtherOperationName() : op.getName();
        final EntityType type = pde.getType();
        final Entity entity = pde.getEntity();
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
            message = "You do not have sufficient privileges to perform the requested operation";
        }

        logger.log(Level.INFO, message, ExceptionUtils.getDebugException(pde));
        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Permission Denied", message, null);
    }
}
