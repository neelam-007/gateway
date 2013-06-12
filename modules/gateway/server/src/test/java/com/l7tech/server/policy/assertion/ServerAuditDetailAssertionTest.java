package com.l7tech.server.policy.assertion;

import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 *  Unit tests for ServerAuditDetailAssertion
 */
public class ServerAuditDetailAssertionTest {

    private ApplicationContext appContext;
    private PolicyEnforcementContext pec;

    @Before
    public void setUp() {
        appContext = ApplicationContexts.getTestApplicationContext();
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Test
    public void testCustomLoggerNameUsingContextVariable() throws IOException, PolicyAssertionException {
        pec.setVariable("var", "value");

        final AuditDetailAssertion detailAssertion = new AuditDetailAssertion("Testing Custom Logger Name using Context Variable");
        detailAssertion.setLoggingOnly(true);
        detailAssertion.setCustomLoggerSuffix("${var}");

        final ServerAuditDetailAssertion serverAssertion = new ServerAuditDetailAssertion(detailAssertion, appContext);
        serverAssertion.checkRequest(pec);

        assertEquals("com.l7tech.log.custom.value", serverAssertion.getLogger().getName());
    }

    @Test
    public void testCustomLoggerNameFallbackDueToNonExistingContextVariable() throws IOException, PolicyAssertionException {
        final AuditDetailAssertion detailAssertion = new AuditDetailAssertion("Testing Custom Logger Name Fall back due to non-existing Context Variable");
        detailAssertion.setLoggingOnly(true);
        detailAssertion.setCustomLoggerSuffix("${var_non_existing}");

        final ServerAuditDetailAssertion serverAssertion = new ServerAuditDetailAssertion(detailAssertion, appContext);
        serverAssertion.checkRequest(pec);

        assertEquals("com.l7tech.server.policy.assertion.ServerAuditDetailAssertion", serverAssertion.getLogger().getName());
    }

    @Test
    public void testCustomLoggerNameFallbackDueToContextVariableWithInvalidPackageName() throws IOException, PolicyAssertionException {
        pec.setVariable("var", "val$ue");

        final AuditDetailAssertion detailAssertion = new AuditDetailAssertion("Testing Custom Logger Name Fall back due to Context Variable with invalid package name");
        detailAssertion.setLoggingOnly(true);
        detailAssertion.setCustomLoggerSuffix("${var}");

        final ServerAuditDetailAssertion serverAssertion = new ServerAuditDetailAssertion(detailAssertion, appContext);
        serverAssertion.checkRequest(pec);

        assertEquals("com.l7tech.server.policy.assertion.ServerAuditDetailAssertion", serverAssertion.getLogger().getName());
    }
}