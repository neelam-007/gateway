/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy.advice;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import com.l7tech.console.security.rbac.FindEntityDialog;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;

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
        FindEntityDialog.find(EntityType.ID_PROVIDER_CONFIG, new Functions.UnaryVoid<EntityHeader>() {
            public void call(EntityHeader entityHeader) {
                ass.setIdentityProviderOid(entityHeader.getGoid());
                pc.proceed();
            }
        }, ass);
    }
}
