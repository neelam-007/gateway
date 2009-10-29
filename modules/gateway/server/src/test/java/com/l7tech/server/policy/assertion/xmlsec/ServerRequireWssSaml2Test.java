package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml2;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.test.BugNumber;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ServerRequireWssSaml2Test {

    Message request;

    @Before
    public void initRequest() throws Exception {
        request = SamlTestUtil.makeSamlRequest(true);
    }

    @Test
    @BugNumber(5141)
    public void testContextVariableAttr() throws Exception {
        RequireWssSaml2 ass = SamlTestUtil.configureToAllowAttributeBearer(new RequireWssSaml2());
        ServerRequireWssSaml2 sass = new ServerRequireWssSaml2(ass, SamlTestUtil.beanFactory, null);

        PolicyEnforcementContext context = SamlTestUtil.createWssProcessedContext(request);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        SamlTestUtil.checkContextVariableResults(context);
    }

}