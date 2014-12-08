package com.l7tech.external.assertions.policybundleinstaller.server;

import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.text.MessageFormat;

import static com.l7tech.server.policy.bundle.PolicyBundleInstallerAbstractServerAssertion.REQUEST_HTTP_PARAMETER_PBI;

/**
 * Server side implementation of the PolicyBundleInstallerAssertion.
 *
 * @see com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerAssertion
 */
public class ServerPolicyBundleInstallerAssertion extends AbstractServerAssertion<PolicyBundleInstallerAssertion> {
    private static final String POLICY_BUNDLE_INSTALLER_POLICY_XML_TEMPLATE = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:{0}/>\n" + // assertion name input
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";

    private final WspReader wspReader;
    private final ServerPolicyFactory serverPolicyFactory;

    public ServerPolicyBundleInstallerAssertion( final PolicyBundleInstallerAssertion assertion, final ApplicationContext applicationContext ) throws PolicyAssertionException {
        super(assertion);
        wspReader = applicationContext.getBean("wspReader", WspReader.class);
        serverPolicyFactory = applicationContext.getBean("policyFactory", ServerPolicyFactory.class);
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        ServerAssertion installerServerAssertion;
        try {
            final String installerAssertionName = context.getVariable(REQUEST_HTTP_PARAMETER_PBI + "installer_name").toString();
            Assertion installerAssertion = wspReader.parseStrictly(MessageFormat.format(POLICY_BUNDLE_INSTALLER_POLICY_XML_TEMPLATE, installerAssertionName), WspReader.Visibility.omitDisabled);

            installerServerAssertion = serverPolicyFactory.compilePolicy(installerAssertion, false);
        } catch (LicenseException | NoSuchVariableException e) {
            throw new PolicyAssertionException(assertion, e);
        }

        return installerServerAssertion.checkRequest(context);
    }

}
