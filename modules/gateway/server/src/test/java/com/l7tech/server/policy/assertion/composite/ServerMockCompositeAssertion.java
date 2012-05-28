package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.TimeSource;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;

public class ServerMockCompositeAssertion extends ServerCompositeAssertion {

    public ServerMockCompositeAssertion(CompositeAssertion composite, BeanFactory beanFactory) throws PolicyAssertionException, LicenseException {
        super(composite, beanFactory);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return iterateChildren(context, null);
    }

    @Override
    protected TimeSource getTimeSource() {
        return ((MockCompositeAssertion)assertion).getTimeSource();
    }
}
