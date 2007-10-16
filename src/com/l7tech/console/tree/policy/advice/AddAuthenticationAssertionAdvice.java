/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.security.rbac.FindEntityDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityHeader;

/**
 * @author alex
 */
public class AddAuthenticationAssertionAdvice implements Advice {
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof AuthenticationAssertion)) {
            throw new IllegalArgumentException();
        }

        final AuthenticationAssertion ass = (AuthenticationAssertion) assertions[0];
        final FindEntityDialog fed = new FindEntityDialog(TopComponents.getInstance().getTopParent(), EntityType.ID_PROVIDER_CONFIG);
        fed.pack();
        DialogDisplayer.display(fed, new Runnable() {
            public void run() {
                EntityHeader eh = fed.getSelectedEntityHeader();
                if (eh == null) return;
                ass.setIdentityProviderOid(eh.getOid());
                pc.proceed();
            }
        });
    }
}
