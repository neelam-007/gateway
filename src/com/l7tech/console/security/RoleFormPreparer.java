/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.security;

import com.l7tech.console.util.FormPreparer;

import javax.security.auth.Subject;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.security.AccessController;

/**
 * The <code>RoleFormPreparer</code>
 *
 * @author emil
 * @version Sep 24, 2004
 */
public class RoleFormPreparer extends FormPreparer {

    public RoleFormPreparer(SecurityProvider provider, String[] editGrantedRoles) {
        super(new GrantToRolePreparer(editGrantedRoles, provider));
    }

    public RoleFormPreparer(SecurityProvider provider, String[] editGrantedRoles, ComponentPreparer[] preparers) {
        super(combinePreparers(new GrantToRolePreparer(editGrantedRoles, provider), preparers));
    }


    /**
     * <code>GrantToRolePreparer</code> grants component action for a given role
     */
    public static class GrantToRolePreparer implements ComponentPreparer {
        private final String[] editGrantedRoles;
        private final SecurityProvider provider;

        public GrantToRolePreparer(String[] editGrantedRole, SecurityProvider provider) {
            this.editGrantedRoles = editGrantedRole;
            this.provider = provider;
        }

        public void prepare(Component c) {}

        public void prepare(JTextComponent c) {
            c.setEditable(isInRole());
        }

        public void prepare(JButton c) {
            c.setEnabled(isInRole());
        }

        protected boolean isInRole() {
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (subject == null) return false;
            return provider.isSubjectInRole(subject, editGrantedRoles);
        }
    }
}