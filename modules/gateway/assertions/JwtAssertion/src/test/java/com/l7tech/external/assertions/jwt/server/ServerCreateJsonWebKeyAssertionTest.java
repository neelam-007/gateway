package com.l7tech.external.assertions.jwt.server;


import com.google.common.collect.Lists;
import com.l7tech.external.assertions.jwt.CreateJsonWebKeyAssertion;
import com.l7tech.external.assertions.jwt.JwkKeyInfo;
import com.l7tech.message.Message;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import junit.framework.Assert;
import org.junit.Test;

public class ServerCreateJsonWebKeyAssertionTest {

    private PolicyEnforcementContext getContext() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Test
    public void test_noKeys() throws Exception {
        PolicyEnforcementContext context = getContext();

        CreateJsonWebKeyAssertion ass = new CreateJsonWebKeyAssertion();
        ass.setTargetVariable("result");
        ass.setKeys(Lists.<JwkKeyInfo>newArrayList());

        ServerCreateJsonWebKeyAssertion sass = new ServerCreateJsonWebKeyAssertion(ass);
        sass.checkRequest(context);
        final String jwks = (String) context.getVariable(ass.getTargetVariable());
        Assert.assertEquals("{\"keys\":[]}", jwks);
    }

}
