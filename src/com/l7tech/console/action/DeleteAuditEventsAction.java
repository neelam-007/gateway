/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.action;

import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.Permission;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.DeleteException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.util.EnumSet;

/**
 * Action that deletes the audit events older than 48 hours, after getting confirmation.
 */
public class DeleteAuditEventsAction extends SecureAction {
    private Action chainAction = null;
    private static final EnumSet<EntityType> AUDIT_SUBTYPES = EnumSet.of(EntityType.AUDIT_MESSAGE, EntityType.AUDIT_ADMIN, EntityType.AUDIT_SYSTEM);

    public DeleteAuditEventsAction() {
        super(null);
    }

    public String getName() {
        return "Delete Old Audit Events";
    }

    public String getDescription() {
        return null;
    }

    protected String iconResource() {
        return null;
    }

    /** Set an action to chain to after this action is performed. */
    public void setChainAction(Action chainAction) {
        this.chainAction = chainAction;
    }

    @Override
    public synchronized boolean isAuthorized() {
        // The set of EntityTypes on which the current user has blanket DELETE permissions
        EnumSet<EntityType> allTypes = EnumSet.noneOf(EntityType.class);

        for (Permission perm : getSecurityProvider().getUserPermissions()) {
            if (perm.getOperation() == OperationType.DELETE && perm.getScope().isEmpty()) {
                EntityType etype = perm.getEntityType();
                if (etype == EntityType.ANY || etype == EntityType.AUDIT_RECORD) return true;
                if (AUDIT_SUBTYPES.contains(etype)) allTypes.add(etype);
            }
        }
        return allTypes.equals(AUDIT_SUBTYPES);
    }

    protected void performAction() {
        final AuditAdmin auditAdmin = Registry.getDefault().getAuditAdmin();

        // Delete the audit events
        try {
            Object[] options  = new Object[] {"Delete Events", "Cancel"};
            String title  ="Delete Audit Events";

            final int age = auditAdmin.serverMinimumPurgeAge();
            String message = "You are about to cause this Gateway to delete\n" +
                    "all non-SEVERE audit events more than " + age + " hours\n" +
                    "(approx. " + (double)(age/24) + " days) old.\n\n" +
                    "This action will result in a permanent audit entry,\n" +
                    "and cannot be undone.\n\nDo you wish to proceed?\n";
            int res2 = JOptionPane.showOptionDialog(null, message, title,
                                                    0, JOptionPane.WARNING_MESSAGE,
                                                    null, options, options[1]);
            if (res2 != 0)
                return;

            auditAdmin.deleteOldAuditRecords();
        } catch (RemoteException e) {
            throw new RuntimeException("Unable to delete old audit events.", e);
        } catch (DeleteException e) {
            throw new RuntimeException("Unable to delete old audit events.", e);
        }

        if (chainAction != null)
            chainAction.actionPerformed(new ActionEvent(this, 0, "chainAction"));
    }
}
