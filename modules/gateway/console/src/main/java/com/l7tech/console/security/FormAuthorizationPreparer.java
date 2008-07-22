/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.security;

import com.l7tech.gateway.common.Authorizer;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.console.util.FormPreparer;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * The <code>FormAuthorizationPreparer</code> authorizes the the form or given form
 * components for the given user role.
 *
 * @author emil
 * @version Sep 24, 2004
 */
public class FormAuthorizationPreparer extends FormPreparer {
    /**
     * Create the form preparer with the security provider and the edit roles
     *
     * @param provider         the security provider that will perform the authorization
     */
    public FormAuthorizationPreparer( Authorizer provider, AttemptedOperation attemptedOperation) {
        super(new GrantToRolePreparer(attemptedOperation, provider));
    }

    /**
     * <code>GrantToRolePreparer</code> grants component action for a given role
     */
    public static class GrantToRolePreparer implements ComponentPreparer {
        private final AttemptedOperation attemptedOperation;
        private final Authorizer authorizer;

        public GrantToRolePreparer(AttemptedOperation attemptedOperation, Authorizer authorizer) {
            this.attemptedOperation = attemptedOperation;
            this.authorizer = authorizer;
        }

        public void prepare(Component c) {}

        public void prepare(JTextComponent c) {
            c.setEditable(c.isEditable() && hasPermission());
        }

        public void prepare(AbstractButton c) {
            c.setEnabled(c.isEnabled() && hasPermission());
        }

        public void prepare(JComboBox c) {
            c.setEnabled(c.isEnabled() && hasPermission());
        }

        protected boolean hasPermission() {
            return authorizer.hasPermission(attemptedOperation);
        }
    }
}