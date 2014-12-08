package com.l7tech.external.assertions.jwt.server;


import com.l7tech.external.assertions.jwt.CreateJsonWebKeyAssertion;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;


public class ServerCreateJsonWebKeyAssertion extends AbstractServerAssertion<CreateJsonWebKeyAssertion> {

    public ServerCreateJsonWebKeyAssertion(@NotNull CreateJsonWebKeyAssertion assertion, @Nullable AuditFactory auditFactory) {
        super(assertion, auditFactory);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return null;
    }
}
