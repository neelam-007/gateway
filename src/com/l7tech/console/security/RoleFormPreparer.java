/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.security;

import com.l7tech.console.util.FormPreparer;

import javax.security.auth.Subject;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>RoleFormPreparer</code>
 *
 * @author emil
 * @version Sep 24, 2004
 */
public class RoleFormPreparer extends FormPreparer {

    public RoleFormPreparer(SecurityProvider provider, String[] editGrantedRoles) {
        super(new GrantRolePreparer(editGrantedRoles, provider));
    }

    public RoleFormPreparer(SecurityProvider provider, String[] editGrantedRoles, ComponentPreparer[] preparers) {
        super(combinePreparers(new GrantRolePreparer(editGrantedRoles, provider), preparers));
    }


    /**
     * <code>RoleBasedPreparer</code> holder for one or more preparers
     */
    public static class GrantRolePreparer implements ComponentPreparer {
        private final String[] editGrantedRoles;
        private final SecurityProvider provider;

        public GrantRolePreparer(String[] editGrantedRole, SecurityProvider provider) {
            this.editGrantedRoles = editGrantedRole;
            this.provider = provider;
        }

        public void prepare(Component c) {}

        public void prepare(JTextComponent c) {
            c.setEditable(isInRole());
        }

        protected boolean isInRole() {
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (subject == null) return false;
            return provider.isSubjectInRole(subject, editGrantedRoles);
        }
    }


    private static CompositePreparer combinePreparers(GrantRolePreparer grantRolePreparer, ComponentPreparer[] preparers) {
        List allPreparers = new ArrayList();
        for (int i = 0; preparers != null && i < preparers.length; i++) {
            ComponentPreparer preparer = preparers[i];
            allPreparers.add(preparer);
        }
        allPreparers.add(grantRolePreparer);
        return new CompositePreparer((ComponentPreparer[])allPreparers.toArray(new ComponentPreparer[]{}));
    }

}