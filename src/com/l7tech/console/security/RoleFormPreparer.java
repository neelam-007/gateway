/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.security;

import com.l7tech.common.Authorizer;
import com.l7tech.console.util.FormPreparer;

import javax.security.auth.Subject;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.security.AccessController;

/**
 * The <code>RoleFormPreparer</code> authorizes the the form or given form components
 * for the given user role.
 *
 * @author emil
 * @version Sep 24, 2004
 */
public class RoleFormPreparer extends FormPreparer {
    /**
     * Create the form preparer with the security provider and the edit roles
     *
     * @param provider         the security provider that will perform the authorization
     * @param editGrantedRoles the roles that allow edit
     */
    public RoleFormPreparer(Authorizer provider, String[] editGrantedRoles) {
        super(new GrantToRolePreparer(editGrantedRoles, provider));
    }

    /**
     * <code>GrantToRolePreparer</code> grants component action for a given role
     */
    public static class GrantToRolePreparer implements ComponentPreparer {
        private final String[] editGrantedRoles;
        private final Authorizer authorizer;

        public GrantToRolePreparer(String[] editGrantedRole, Authorizer authorizer) {
            this.editGrantedRoles = editGrantedRole;
            this.authorizer = authorizer;
        }

        public void prepare(Component c) {}

        public void prepare(JTextComponent c) {
            c.setEditable(c.isEditable() && isInRole());
        }

        public void prepare(AbstractButton c) {
            c.setEnabled(c.isEnabled() && isInRole());
        }

        public void prepare(JComboBox c) {
            c.setEnabled(c.isEnabled() && isInRole());
        }

        protected boolean isInRole() {
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (subject == null) return false;
            return authorizer.isSubjectInRole(subject, editGrantedRoles);
        }
    }
}