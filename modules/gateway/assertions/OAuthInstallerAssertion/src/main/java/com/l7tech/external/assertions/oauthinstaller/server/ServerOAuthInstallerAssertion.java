package com.l7tech.external.assertions.oauthinstaller.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAdmin;
import com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAssertion;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.policy.bundle.BundleUtils;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerAbstractServerAssertion;
import com.l7tech.util.DomUtils;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Server side implementation of the OAuthInstallerAssertion to support custom no GUI (headless) installer logic.
 * Customizations include:
 *      Custom action to get the OAuth database schema
 *              Input HTTP parameter: set oauth_installer.action to get_db_schema
 *              Output: OAuth database schema
 *      Add custom wsman dry run logic to integrate API Portal
 *              Input: optionally set HTTP parameter oauth_installer.integrate_api_portal to true
 *              Output: same as parent class (i.e. BundleInstallerAbstractServerAssertion)
 *      Add custom wsman install logic to integrate API Portal
 *              Input: optionally set HTTP parameter oauth_installer.integrate_api_portal to true
 *              Output: same as parent class (i.e. BundleInstallerAbstractServerAssertion)
 */
public class ServerOAuthInstallerAssertion extends PolicyBundleInstallerAbstractServerAssertion<OAuthInstallerAssertion> {
    final OAuthInstallerAdmin oAuthInstallerAdmin;

    public ServerOAuthInstallerAssertion( final OAuthInstallerAssertion assertion, final ApplicationContext applicationContext ) throws PolicyAssertionException {
        super(assertion, applicationContext);
        oAuthInstallerAdmin = (OAuthInstallerAdmin) policyBundleInstallerAdmin;
    }

    @Override
    protected void customActionCallback() throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        try {
            final String action = context.getVariable(CONTEXT_VARIABLE_PREFIX + "oauth_installer.action").toString();
            if ("get_db_schema".equals(action)) {
                final Document document = XmlUtil.createEmptyDocument("OAuthDbSchema", L7, BundleUtils.NS_BUNDLE);
                DomUtils.setTextContent(document.getDocumentElement(), oAuthInstallerAdmin.getOAuthDatabaseSchema());
                writeResponse(document);
            }
        } catch (NoSuchVariableException e) {
            throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException(e);
        }
    }

    @Override
    protected AsyncAdminMethods.JobId<PolicyBundleDryRunResult> callAdminDryRun(final List<String> componentIds,
                                                                                final HashMap<String, BundleMapping> mappings,
                                                                                final String versionModifier) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        boolean integrateApiPortal = false;
        try {
            integrateApiPortal = Boolean.parseBoolean(context.getVariable(CONTEXT_VARIABLE_PREFIX + "oauth_installer.integrate_api_portal").toString());
        } catch (NoSuchVariableException e) {
            // leave integrateApiPortal false
        }
        return oAuthInstallerAdmin.dryRunInstall(componentIds, mappings, versionModifier, integrateApiPortal);
    }

    @Override
    protected AsyncAdminMethods.JobId<ArrayList> callAdminInstall(final List<String> componentIds,
                                                                  final Goid folder,
                                                                  final HashMap<String, BundleMapping> mappings,
                                                                  final String versionModifier) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        boolean integrateApiPortal = false;
        try {
            integrateApiPortal = Boolean.parseBoolean(context.getVariable(CONTEXT_VARIABLE_PREFIX + "oauth_installer.integrate_api_portal").toString());
        } catch (NoSuchVariableException e) {
            // leave integrateApiPortal false
        }
        return oAuthInstallerAdmin.install(componentIds, folder, mappings, versionModifier, integrateApiPortal);
    }
}
