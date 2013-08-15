/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.security.rbac.FindEntityDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_ACTION_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_ACTION_ICON;

import javax.swing.*;

/**
 * @author alex
 */
public class AuthenticationAssertionPolicyNode extends IdentityAssertionTreeNode<AuthenticationAssertion> {
    private final AbstractAction propertiesAction = new SecureAction(null) {
        @Override
        protected void performAction() {
            FindEntityDialog.find(EntityType.ID_PROVIDER_CONFIG, new Functions.UnaryVoid<EntityHeader>() {
                @Override
                public void call(EntityHeader entityHeader) {
                    assertion.setIdentityProviderOid(entityHeader.getGoid());
                    provName = null; // Reset cached name
                    PolicyTree tree = (PolicyTree) TopComponents.getInstance().getComponent(PolicyTree.NAME);
                    PolicyTreeModel dtm = (PolicyTreeModel) tree.getModel();
                    dtm.assertionTreeNodeChanged(AuthenticationAssertionPolicyNode.this);
                }
            }, assertion);
        }

        @Override
        protected String iconResource() {
            return assertion.meta().get(PROPERTIES_ACTION_ICON);
        }

        @Override
        public String getName() {
            return assertion.meta().get(PROPERTIES_ACTION_NAME);
        }
    };

    public AuthenticationAssertionPolicyNode(AuthenticationAssertion idass) {
        super(idass);
    }

    public String getName(final boolean decorate) {
        final String name = "Authenticate against " + idProviderName();

        return (decorate) ? DefaultAssertionPolicyNode.addCommentToDisplayText(assertion, decorateName(name)) : name;
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
