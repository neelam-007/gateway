package com.l7tech.external.assertions.policybundleinstaller.server;

import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.composite.ServerCompositeAssertion;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerAbstractServerAssertion;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerServerAssertionException;
import org.apache.http.HttpStatus;
import org.springframework.context.ApplicationContext;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.MessageFormat;

import static com.l7tech.server.policy.bundle.PolicyBundleInstallerAbstractServerAssertion.CONTEXT_VARIABLE_PREFIX;
import static com.l7tech.server.policy.bundle.PolicyBundleInstallerAbstractServerAssertion.REQUEST_HTTP_PARAMETER;

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
        final String installerAssertionName;

        try {
            installerAssertionName = context.getVariable(REQUEST_HTTP_PARAMETER + CONTEXT_VARIABLE_PREFIX + "installer_name").toString();
        } catch (NoSuchVariableException e) {
            throw new PolicyAssertionException(assertion, "Installer name must be specified.", e);
        }

        try {
            Assertion installerAssertion = wspReader.parseStrictly(MessageFormat.format(POLICY_BUNDLE_INSTALLER_POLICY_XML_TEMPLATE, installerAssertionName), WspReader.Visibility.omitDisabled);
            installerServerAssertion = serverPolicyFactory.compilePolicy(installerAssertion, false);
        } catch (IOException | ServerPolicyException | LicenseException e) {
            throw new PolicyAssertionException(assertion, "Unable to create installer: " + installerAssertionName, e);
        }

        if (installerServerAssertion instanceof ServerCompositeAssertion) {
            for (Object serverAssertion : ((ServerCompositeAssertion) installerServerAssertion).getChildren()) {
                if (serverAssertion instanceof PolicyBundleInstallerAbstractServerAssertion) {
                    // must access inputs via request http parameter context variables (e.g. request.http.parameter.pbi.installer_name)
                    ((PolicyBundleInstallerAbstractServerAssertion) serverAssertion).setUsesRequestHttpParams(true);
                }
            }
        }

        try {
            return installerServerAssertion.checkRequest(context);
        } catch (PolicyBundleInstallerServerAssertionException e) {
            switch (e.getHttpStatusCode()) {
                case HttpStatus.SC_BAD_REQUEST:
                    throw new AssertionStatusException(AssertionStatus.BAD_REQUEST, e.getMessage());
                case HttpStatus.SC_NOT_FOUND:
                    throw new AssertionStatusException(AssertionStatus.SERVICE_NOT_FOUND, e.getMessage());
                default:
                    throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage());
            }
        }
    }
}
