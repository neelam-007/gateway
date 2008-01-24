/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.util.Functions;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.security.rbac.FindEntityDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;

import javax.swing.*;

/**
 * @author alex
 */
public class AuthenticationAssertionPolicyNode extends IdentityAssertionTreeNode<AuthenticationAssertion> {
    private final AbstractAction propertiesAction = new SecureAction(null) {
        @Override
        protected void performAction() {
            FindEntityDialog.find(EntityType.ID_PROVIDER_CONFIG, new Functions.UnaryVoid<EntityHeader>() {
                public void call(EntityHeader entityHeader) {
                    assertion.setIdentityProviderOid(entityHeader.getOid());
                    provName = null; // Reset cached name
                    PolicyTree tree = (PolicyTree) TopComponents.getInstance().getComponent(PolicyTree.NAME);
                    PolicyTreeModel dtm = (PolicyTreeModel) tree.getModel();
                    dtm.assertionTreeNodeChanged(AuthenticationAssertionPolicyNode.this);
                }
            });
        }

        @Override
        protected String iconResource() {
            return "com/l7tech/console/resources/Properties16.gif";
        }

        @Override
        public String getName() {
            return "Authentication Assertion Properties";
        }
    };

    public AuthenticationAssertionPolicyNode(AuthenticationAssertion idass) {
        super(idass);
    }

    @Override
    public String getName() {
        return "Authenticate against " + idProviderName();
    }

    @Override
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/user16.png";
    }

    @Override
    public Action getPreferredAction() {
        if ( !canEdit() )
            return null;
        else
            return propertiesAction;
    }

}
