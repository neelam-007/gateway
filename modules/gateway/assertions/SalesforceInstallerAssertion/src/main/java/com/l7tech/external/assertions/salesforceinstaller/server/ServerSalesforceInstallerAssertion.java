package com.l7tech.external.assertions.salesforceinstaller.server;

import com.l7tech.external.assertions.salesforceinstaller.SalesforceInstallerAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.AllModulesClassLoader;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Server side implementation of the SalesforceInstallerAssertion.
 * This assertion is used to load a Task Action to install the required services to connect to Salesforce.
 * @see com.l7tech.external.assertions.salesforceinstaller.SalesforceInstallerAssertion
 */
public class ServerSalesforceInstallerAssertion extends AbstractServerAssertion<SalesforceInstallerAssertion> {
    private final String localHostName;
    private final AllModulesClassLoader allModulesClassLoader;

    public ServerSalesforceInstallerAssertion( final SalesforceInstallerAssertion assertion, ApplicationContext context ) throws PolicyAssertionException {
        super(assertion);
        allModulesClassLoader = context.getBean("allModulesClassLoader", AllModulesClassLoader.class);
        try {
            localHostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            throw new PolicyAssertionException(assertion, e);
        }
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        return AssertionStatus.FAILED;
    }
}
