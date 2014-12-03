package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.event.bundle.InstallPolicyBundleEvent;
import com.l7tech.server.event.bundle.PolicyBundleInstallerEvent;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.test.BugId;
import com.l7tech.util.*;
import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.*;

import static com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAdminImpl.OAUTH_SLASH_CLIENTS;
import static com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAssertion.SECURE_ZONE_STORAGE_COMP_ID;
import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.*;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getEntityName;
import static com.l7tech.util.Functions.toMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;

/**
 * These tests are concerned with customization logic relating to the contents of the OTK bundles
 * (e.g. getPolicyBundleInstallerCallback(installationPrefix)).
 */
public class OAuthInstallerAdminImplCallbackCustomizationTest {

    private final String productionBaseName = "/com/l7tech/external/assertions/oauthinstaller/bundles/";
    private final String productionWsmanPolicyBundleInfoFileName = "OAuthToolkitBundleInfo.xml";
    private final String productionWsmanPolicyBundleInfoNamespace = "http://ns.l7tech.com/2012/11/oauth-toolkit-bundle";

    private final String testRestmanBaseName = "/com/l7tech/external/assertions/oauthinstaller/test_bundles/restman/";

    /**
     * Validates the definition and usage (where possible) of any ${host_ variable.
     *
     * This is important as the OTK installer modifies , when requested, usages of these variable to inject the 'prefix'
     * value to allow for side by side installs of the OTK. If the idiom is not followed correctly in the canned OTK
     * policies, then this mechanism will not work.
     *
     * Rules are:
     * Variables defined with a name of 'host_' must not end with a trailing slash.
     * Usages of host_ variables, whether in another set variable assertion or in a HTTP routing assertion must follow
     * the variable with a slash '/' character. This rule cannot be enforced when the usage is followed by another
     * variable reference.
     *
     */
    @Test
    public void testHostNamesDoNotContainTrailingSlash() throws Exception {
        // check production wsman bundles
        List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), productionBaseName);
        BundleResolver resolver = new BundleResolverImpl(bundleInfos, getClass()) {};
        List<BundleInfo> allBundles = resolver.getResultList();
        for (BundleInfo aBundle : allBundles) {
            final Document policyDocument = resolver.getBundleItem(aBundle.getId(), BundleItem.POLICY, false);
            final Document serviceDocument = resolver.getBundleItem(aBundle.getId(), BundleItem.SERVICE, false);

            List<Element> resourceSetPolicyElements = XpathUtil.findElements(policyDocument.getDocumentElement(), "//l7:Resources/l7:ResourceSet/l7:Resource[@type=\"policy\"]", getNamespaceMap());
            validateEnumerationDocForHostNames(resourceSetPolicyElements);

            resourceSetPolicyElements = XpathUtil.findElements(serviceDocument.getDocumentElement(), "//l7:Resources/l7:ResourceSet/l7:Resource[@type=\"policy\"]", getNamespaceMap());
            validateEnumerationDocForHostNames(resourceSetPolicyElements);
        }

        // check test restman bundles
        bundleInfos = BundleUtils.getBundleInfos(getClass(), testRestmanBaseName);
        resolver = new BundleResolverImpl(bundleInfos, getClass()) {};
        allBundles = resolver.getResultList();
        for (BundleInfo aBundle : allBundles) {
            final BundleItem migrationBundleItem =  BundleItem.MIGRATION_BUNDLE;
            migrationBundleItem.setVersion(aBundle.getVersion());
            final Document migrationDocument = resolver.getBundleItem(aBundle.getId(), migrationBundleItem, false);

            List<Element> resourceSetPolicyElements = new RestmanMessage(migrationDocument).getResourceSetPolicyElements();
            validateEnumerationDocForHostNames(resourceSetPolicyElements);
            validateEnumerationDocForHostNames(resourceSetPolicyElements);
        }
    }

    private void validateEnumerationDocForHostNames(List<Element> resourceSetPolicyElements) throws Exception {
        for (Element resourceSetPolicyElement : resourceSetPolicyElements) {
            final Element layer7Policy = getPolicyDocumentFromResource(resourceSetPolicyElement, "Policy", "Not Needed").getDocumentElement();
            final List<Element> contextVariables = PolicyUtils.findContextVariables(layer7Policy);
            validateSetVariableForHostVarUsage(contextVariables);
            final List<Element> protectedUrls = PolicyUtils.findProtectedUrls(layer7Policy);
            for (Element protectedUrl : protectedUrls) {
                final String urlValue = protectedUrl.getAttribute("stringValue");
                validateHostVariableUsage(urlValue, "ProtectedServiceUrl");
            }
        }
    }

    private void validateSetVariableForHostVarUsage(List<Element> contextVariables) {
        for (Element contextVariable : contextVariables) {
            final Element variableToSet = XmlUtil.findFirstChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "VariableToSet");
            final String varName = variableToSet.getAttribute("stringValue");
            final Element expression = XmlUtil.findFirstChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "Base64Expression");
            final String base64Value = expression.getAttribute("stringValue");
            final byte[] decodedValue = HexUtils.decodeBase64(base64Value);
            final String varValue = new String(decodedValue, Charsets.UTF8);
            if (varName.startsWith("host_")) {
                assertFalse("Host variable '" + varName + "'value should not contain a trailing slash: " + varValue, varValue.endsWith("/"));
            } else if (varValue.contains("${host_")) {
                validateHostVariableUsage(varValue, varName);
            }
        }
    }

    /**
     * Validate the usage of a ${host_ variable in a policy. The same rules apply whether it is referneced from within
     * a context variable or a protected service URL in a routing assertion. The ${host_ variable must be followed by
     * a slash. If it is followed by a context variable then we cannot validate that particular usage.
     *
     * @param usageValue The value of the variable or protected service URL which references the ${host_ variable
     * @param description A description of the usage used when test fails to identify the usage issue.
     */
    private void validateHostVariableUsage(String usageValue, String description) {
        int index = usageValue.indexOf("${host_");
        while (index != -1) {
            int closeIndex = usageValue.indexOf("}", index + 1);
            char nextChar = usageValue.charAt(closeIndex + 1);
            if (nextChar == '$') {
                System.out.println("Cannot verify '" + description + "' with value '" + usageValue + "' as host variable usage as it is followed by a context variable");
            } else {
                assertEquals("Invalid ${host_ var reference for '" + description + "' with value '" + usageValue + "'. " +
                        "Usage must be followed by a trailing slash.", "/", String.valueOf(nextChar));
            }
            index = usageValue.indexOf("${host_", closeIndex + 1);
        }
    }

    @Test
    public void testGetUpdatedHostValue() throws Exception {
        String test = "https://${host_target}${request.url.path}";
        String actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", test);
        System.out.println(actual);
        assertEquals("https://${host_target}/version1${request.url.path}", actual);

        actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", "https://${host_target}/auth/oauth/v1/token");
        System.out.println(actual);
        assertEquals("https://${host_target}/version1/auth/oauth/v1/token", actual);

        actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", "${host_target}");
        assertNull(actual);

        actual = OAuthInstallerAdminImpl.getUpdatedHostValue("version1", "https://${host_target}");
        assertEquals("https://${host_target}/version1", actual);
    }

    /**
     * Test callback customizations in production OAuth 1.0 bundle (wsman, main\resources\com\l7tech\external\assertions\oauthinstaller\bundles\OAuth_1_0\BundleInfo.xml).
     * For the test OAuthInstallerAdminImpl will listen to the install event (InstallPolicyBundleEvent) to setup and trigger a callback (PolicyBundleInstallerCallback).
     * Test will:
     *      - verify ${host_oauth_session_server} ProtectedServiceUrl now includes a version modifier (e.g. ${host_oauth_session_server}/v1)
     *      - verify SetVariable oauth.endpoint.request name now includes a version modifier in the expression (e.g. ${host_oauth_endpoint}/v1)
     */
    @Test
    public void testCallbackForWsmanBundle() throws Exception {
        final String versionModifier = "v1";

        // check production wsman bundle
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(productionBaseName, productionWsmanPolicyBundleInfoFileName, productionWsmanPolicyBundleInfoNamespace, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {

                // test only install event (not dry run event)
                if (applicationEvent instanceof InstallPolicyBundleEvent) {
                    InstallPolicyBundleEvent installEvent = (InstallPolicyBundleEvent) applicationEvent;
                    final PolicyBundleInstallerCallback policyBundleInstallerCallback = installEvent.getPolicyBundleInstallerCallback();
                    if (policyBundleInstallerCallback == null) {
                        fail("Policy call back should be configured.");
                    }
                    final BundleInfo bundleInfo = installEvent.getContext().getBundleInfo();
                    try {
                        // get a handle on each wsman Service document
                        final Document serviceEnum = installEvent.getContext().getBundleResolver().getBundleItem(bundleInfo.getId(), BundleResolver.BundleItem.SERVICE, false);
                        final List<Element> gatewayMgmtPolicyElments = getEntityElements(serviceEnum.getDocumentElement(), "Service");
                        for (Element serviceEnumElm : gatewayMgmtPolicyElments) {
                            final Element policyResourceElmWritable = getPolicyResourceElement(serviceEnumElm, "Service", "not used");
                            if (policyResourceElmWritable == null) {
                                throw new Exception("No policy resource element.  Expect a policy resource element in the service bundle.");
                            }
                            final Document policyDocWriteable = getPolicyDocumentFromResource(policyResourceElmWritable, "Service", "not used");
                            final Element serviceDetailElement = getServiceDetailElement(serviceEnumElm);

                            // make sure portal integration present
                            if (SECURE_ZONE_STORAGE_COMP_ID.equals(bundleInfo.getId())) {
                                final String entityName = getEntityName(serviceDetailElement);
                                if (OAUTH_SLASH_CLIENTS.equals(entityName)) {
                                    verifyApiPortalIntegrationExists(policyDocWriteable.getDocumentElement());
                                }
                            }

                            // trigger callback for testing
                            policyBundleInstallerCallback.prePolicySave(bundleInfo, serviceDetailElement, policyDocWriteable);

                            // verify host modification
                            verifyHostVersionModify(policyDocWriteable.getDocumentElement(), versionModifier);

                            // verify portal integration removed
                            if (SECURE_ZONE_STORAGE_COMP_ID.equals(bundleInfo.getId())) {
                                // check the service being published
                                final String entityName = getEntityName(serviceDetailElement);
                                if (OAUTH_SLASH_CLIENTS.equals(entityName)) {
                                    verifyApiPortalIntegrationRemoved(policyDocWriteable.getDocumentElement());
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("Unexpected exception: " + e.getMessage());
                    }

                    installEvent.setProcessed(true);
                }
            }
        });

        // run test against production OAuth 1.0 component
        final String oauth1_0 = "1c2a2874-df8d-4e1d-b8b0-099b576407e1";
        executeAsInstallJob(admin, oauth1_0, versionModifier);

        //  run test against production Secure Zone Storage component
        executeAsInstallJob(admin, SECURE_ZONE_STORAGE_COMP_ID, versionModifier);
    }

    private void executeAsInstallJob(final OAuthInstallerAdminImpl admin, final String component, final String versionModifier)
            throws InterruptedException, AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException, PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        AsyncAdminMethods.JobId<ArrayList> jobId = admin.install(Arrays.asList(component), new Goid(0, -5002), new HashMap<String, BundleMapping>(), versionModifier, false);
        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }
        AsyncAdminMethods.JobResult jobResult = admin.getJobResult(jobId);
        assertEquals("Job error", null, jobResult.throwableClassname);
    }

    /**
     * Test callback customizations in test OAuth 1.0 bundle (restman, test\resources\com\l7tech\external\assertions\oauthinstaller\test_bundles\restman\OAuth Manager\BundleInfo.xml).
     * OAuthInstallerAdminImpl will listen to both the dry run and install event (DryRunInstallPolicyBundleEvent & InstallPolicyBundleEvent) to setup and trigger a callback (PolicyBundleInstallerCallback).
     * Test will:
     *      - verify ${host_oauth_session_server} ProtectedServiceUrl now includes a version modifier (e.g. ${host_oauth_session_server}/v1)
     *      - verify SetVariable oauth.endpoint.request name now includes a version modifier in the expression (e.g. ${host_oauth_endpoint}/v1)
     */
    @Test
    @BugId("SSG-9593")
    public void testCallbackForRestmanBundle() throws Exception {
        final String versionModifier = "v1";
        final String testRestmanPolicyBundleInfoFileName = "OAuthToolkitPolicyBundleInfo.xml";
        final String testRestmanPolicyBundleInfoNamespace = "http://ns.l7tech.com/2014/11/oauth-toolkit-policy-bundle";
        final String secureZoneStorageComponent = "e66733c866130a55b140ba085a5fe875";

        // check test restman bundle
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(testRestmanBaseName, testRestmanPolicyBundleInfoFileName, testRestmanPolicyBundleInfoNamespace, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {

                // test both dry run & install events
                if (applicationEvent instanceof PolicyBundleInstallerEvent) {
                    PolicyBundleInstallerEvent installerEvent = (PolicyBundleInstallerEvent) applicationEvent;
                    final PolicyBundleInstallerCallback policyBundleInstallerCallback = installerEvent.getPolicyBundleInstallerCallback();
                    if (policyBundleInstallerCallback == null) {
                        fail("Policy call back should be configured.");
                    }

                    final BundleInfo bundleInfo = installerEvent.getContext().getBundleInfo();
                    final BundleItem migrationBundleItem =  BundleItem.MIGRATION_BUNDLE;
                    migrationBundleItem.setVersion(bundleInfo.getVersion());
                    try {
                        // get a handle to the restman migration document
                        final Document migrationDocument = installerEvent.getContext().getBundleResolver().getBundleItem(bundleInfo.getId(), migrationBundleItem, false);
                        final Element migrationDocumentElement = migrationDocument.getDocumentElement();

                        // make sure portal integration exists
                        if (secureZoneStorageComponent.equals(bundleInfo.getId())) {
                            List<Element> oauthClientServiceResources = XpathUtil.findElements(migrationDocumentElement,
                                    "//l7:Item[l7:Type='SERVICE' and l7:Name='" + OAUTH_SLASH_CLIENTS + "']/l7:Resource/l7:Service/l7:Resources/l7:ResourceSet/l7:Resource",
                                    getNamespaceMap());
                            for (Element oauthClientServiceResource : oauthClientServiceResources) {
                                verifyApiPortalIntegrationExists(XmlUtil.parse(DomUtils.getTextValue(oauthClientServiceResource)).getDocumentElement());
                            }
                        }

                        // trigger callback for testing
                        policyBundleInstallerCallback.preMigrationBundleImport(bundleInfo, migrationDocument);

                        // verify host modification
                        for (Element policyElement : XpathUtil.findElements(migrationDocumentElement, "//l7:Resources/l7:ResourceSet/l7:Resource[@type=\"policy\"]", getNamespaceMap())) {
                            try {
                                verifyHostVersionModify(XmlUtil.parse(DomUtils.getTextValue(policyElement)).getDocumentElement(), versionModifier);
                            } catch (TooManyChildElementsException | MissingRequiredElementException e) {
                                throw new PolicyBundleInstallerCallback.CallbackException(e);
                            }
                        }

                        // verify portal integration removed
                        if (secureZoneStorageComponent.equals(bundleInfo.getId())) {
                            List<Element> oauthClientServiceResources = XpathUtil.findElements(migrationDocumentElement,
                                    "//l7:Item[l7:Type='SERVICE' and l7:Name='" + OAUTH_SLASH_CLIENTS + "']/l7:Resource/l7:Service/l7:Resources/l7:ResourceSet/l7:Resource",
                                    getNamespaceMap());
                            for (Element oauthClientServiceResource : oauthClientServiceResources) {
                                verifyApiPortalIntegrationRemoved(XmlUtil.parse(DomUtils.getTextValue(oauthClientServiceResource)).getDocumentElement());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("Unexpected exception: " + e.getMessage());
                    }

                    installerEvent.setProcessed(true);
                }
            }
        }) {
            @NotNull
            @Override
            /**
             * Override OAuthInstallerAdminImpl.getPolicyBundleInstallerCallback(...) to version modifying host for restman message
             */
            protected PolicyBundleInstallerCallback getPolicyBundleInstallerCallback(final String versionModifier) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
                return new PolicyBundleInstallerCallback() {
                    @Override
                    public void preMigrationBundleImport(@NotNull final BundleInfo bundleInfo, @NotNull final Document restmanMessageDocument) throws CallbackException {
                        final Element restmanMessageDocumentElement = restmanMessageDocument.getDocumentElement();

                        for (Element policyElement : XpathUtil.findElements(restmanMessageDocumentElement, "//l7:Resources/l7:ResourceSet/l7:Resource[@type=\"policy\"]", getNamespaceMap())) {
                            try {
                                // get a clone element from parsed xml
                                Element parsedPolicyElement = XmlUtil.parse(DomUtils.getTextValue(policyElement)).getDocumentElement();

                                addVersionCommentAssertionsToPolicy(bundleInfo, parsedPolicyElement);

                                if (versionModifier != null) {
                                    updateProtectedServiceUrlForHost(parsedPolicyElement, versionModifier);
                                    updateBase64ContextVariableExpressionForHost(parsedPolicyElement, versionModifier);
                                }

                                // set the modified xml
                                DomUtils.setTextContent(policyElement, XmlUtil.nodeToString(parsedPolicyElement));
                            } catch (IOException | SAXException e) {
                                throw new CallbackException(e);
                            }
                        }

                        // Is the API portal being integrated? If not then we need to remove assertions from the policy
                        if (secureZoneStorageComponent.equals(bundleInfo.getId()) && !integrateApiPortal) {
                            // find "oauth/clients" service
                            List<Element> oauthClientServiceResources = XpathUtil.findElements(
                                    restmanMessageDocumentElement,
                                    "//l7:Item[l7:Type='SERVICE' and l7:Name='" + OAUTH_SLASH_CLIENTS + "']/l7:Resource/l7:Service/l7:Resources/l7:ResourceSet/l7:Resource",
                                    getNamespaceMap());
                            for (Element oauthClientServiceResource : oauthClientServiceResources) {
                                // we do not need to check for modular assertion dependencies, like any other
                                // if the user wants the API Portal integrated, then we will do that, it can be fixed manually later.
                                try {
                                    // get a clone element from parsed xml
                                    final Element parsedPolicyElement = XmlUtil.parse(DomUtils.getTextValue(oauthClientServiceResource)).getDocumentElement();

                                    removeApiPortalIntegration(parsedPolicyElement);

                                    // set the modified xml
                                    DomUtils.setTextContent(oauthClientServiceResource, XmlUtil.nodeToString(parsedPolicyElement));
                                } catch (SAXException | IOException e) {
                                    throw new CallbackException(e);
                                }
                            }
                        }
                    }
                };
            }
        };

        // run test against test OAuth 1.0 component
        final String oauth10Component = "762e04fe9bda06bcff76b7d8ee99ef62";
        executeAsDryRunJob(admin, oauth10Component, versionModifier);
        executeAsInstallJob(admin, oauth10Component, versionModifier);

        // run test against test Secure Zone Storage component
        executeAsInstallJob(admin, secureZoneStorageComponent, versionModifier);
    }

    private void executeAsDryRunJob(final OAuthInstallerAdminImpl admin, final String component, final String versionModifier)
            throws InterruptedException, AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException, PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        AsyncAdminMethods.JobId<PolicyBundleDryRunResult> jobId = admin.dryRunInstall(Arrays.asList(component), new HashMap<String, BundleMapping>(), versionModifier, false);
        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }
        AsyncAdminMethods.JobResult jobResult = admin.getJobResult(jobId);
        assertEquals("Job error", null, jobResult.throwableClassname);
    }

    private void verifyHostVersionModify(final Element bundleElement, final String versionModifier) throws TooManyChildElementsException, MissingRequiredElementException {

        // verify ${host_oauth_session_server} ProtectedServiceUrl now includes a version modifier (e.g. ${host_oauth_session_server}/v1)
        final String hostOauthSessionServer =  "${host_oauth_session_server}";
        final List<Element> protectedUrls = PolicyUtils.findProtectedUrls(bundleElement);
        for (Element protectedUrl : protectedUrls) {
            final String protectedUrlValue = protectedUrl.getAttribute("stringValue");
            if (protectedUrlValue.contains(hostOauthSessionServer)) {
                assertThat(System.lineSeparator() + Thread.currentThread().getStackTrace()[1].toString(), protectedUrlValue, containsString(hostOauthSessionServer + "/" + versionModifier));
            }
        }

        // verify SetVariable oauth.endpoint.request name now includes a version modifier in the expression (e.g. ${host_oauth_endpoint}/v1)
        final String oauthEndpointRequest = "oauth.endpoint.request";
        final String hostOAuthEndpoint = "${host_oauth_endpoint}";
        final List<Element> contextVariables = PolicyUtils.findContextVariables(bundleElement);
        for (Element contextVariable : contextVariables) {
            final Element variableToSetElm = XmlUtil.findExactlyOneChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "VariableToSet");
            final Element base64ExpressionElm = XmlUtil.findExactlyOneChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "Base64Expression");
            final String variableName = variableToSetElm.getAttribute("stringValue");
            if (variableName.contains(oauthEndpointRequest)) {
                final String base64Value = base64ExpressionElm.getAttribute("stringValue");
                final String decodedValue = new String(HexUtils.decodeBase64(base64Value, true), Charsets.UTF8);
                assertThat(System.lineSeparator() + Thread.currentThread().getStackTrace()[1].toString(), decodedValue, containsString(hostOAuthEndpoint + "/" + versionModifier));
            }
        }
    }


    private void verifyApiPortalIntegrationExists(final Element bundleElement) {
        // verify all stringValue="PORTAL_INTEGRATION" have been removed
        assertThat(System.lineSeparator() + Thread.currentThread().getStackTrace()[1].toString(),
                XpathUtil.findElements(bundleElement, "//L7p:value[@stringValue='PORTAL_INTEGRATION']", getNamespaceMap()).size(), greaterThan(0));
    }

    private void verifyApiPortalIntegrationRemoved(final Element bundleElement) {
        // verify all stringValue="PORTAL_INTEGRATION" have been removed
            assertEquals(System.lineSeparator() + Thread.currentThread().getStackTrace()[1].toString(), 0,
                XpathUtil.findElements(bundleElement, "//L7p:value[@stringValue='PORTAL_INTEGRATION']", getNamespaceMap()).size());
    }

    /**
     * Test version modification to the "l7otk1a" cookie.  The production OAuth 1.0 bundle is used for this test
     * (wsman, main\resources\com\l7tech\external\assertions\oauthinstaller\bundles\OAuth_1_0\BundleInfo.xml).
     *
     * OAuthInstallerAdminImpl will listen to the install event (InstallPolicyBundleEvent) to setup and trigger a callback (PolicyBundleInstallerCallback).
     * Test will verify l7otk1a is version modified correctly for:
     *      - L7p:CookieCredentialSource/L7p:CookieName stringValue=l7otk1a
     *      - L7p:Regex/L7p:Regex stringValue contains l7otk1a
     *      - L7p:SetVariable/L7p:Base64Expression stringValue=${cookie.l7otk1a}
     *      - L7p:AddHeader L7p:HeaderValue stringValue contains l7otk1a
     */
    @Test
    @BugId("SSG-6376, SSG-9593")
    public void testCookieVersionModify() throws Exception {
        final String l7otk1aCookieName = "l7otk1a";
        final String versionModifier = "v1";

        // check production wsman bundle
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(productionBaseName, productionWsmanPolicyBundleInfoFileName, productionWsmanPolicyBundleInfoNamespace, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {

                if (applicationEvent instanceof InstallPolicyBundleEvent) {
                    InstallPolicyBundleEvent installEvent = (InstallPolicyBundleEvent) applicationEvent;
                    final PolicyBundleInstallerCallback policyBundleInstallerCallback = installEvent.getPolicyBundleInstallerCallback();
                    if (policyBundleInstallerCallback == null) {
                        fail("Policy call back should be configured.");
                    }
                    final BundleInfo bundleInfo = installEvent.getContext().getBundleInfo();
                    try {
                        final Document serviceEnum = installEvent.getContext().getBundleResolver().getBundleItem(bundleInfo.getId(), BundleResolver.BundleItem.SERVICE, false);
                        final List<Element> gatewayMgmtPolicyElments = getEntityElements(serviceEnum.getDocumentElement(), "Service");
                        for (Element serviceEnumElm : gatewayMgmtPolicyElments) {
                            final Element policyResourceElmWritable = getPolicyResourceElement(serviceEnumElm, "Service", "not used");
                            if (policyResourceElmWritable == null) {
                                throw new Exception("No policy resource element.  Expect a policy resource element in the service bundle.");
                            }
                            final Document policyDocWriteable = getPolicyDocumentFromResource(policyResourceElmWritable, "Service", "not used");
                            final Element serviceDetailElement = getServiceDetailElement(serviceEnumElm);

                            // count instances of ${cookie.l7otk1a} to compare later
                            final Element writeablePolicyElement = policyDocWriteable.getDocumentElement();
                            String encodedCookieL7otk1a = HexUtils.encodeBase64(HexUtils.encodeUtf8(Syntax.getVariableExpression("cookie." + l7otk1aCookieName)), true);
                            List<Element> cookieElements = XpathUtil.findElements(writeablePolicyElement, "//L7p:SetVariable/L7p:Base64Expression[@stringValue=\"" + encodedCookieL7otk1a + "\"]", getNamespaceMap());
                            int numCookieL7otk1a = cookieElements.size();

                            // trigger callback for testing
                            policyBundleInstallerCallback.prePolicySave(bundleInfo, serviceDetailElement, policyDocWriteable);

                            // code below verifies l7otk1a CookieName now includes a version modifier (e.g. l7otk1av1)

                            cookieElements = XpathUtil.findElements(writeablePolicyElement, "//L7p:CookieCredentialSource/L7p:CookieName[@stringValue=\"" +  l7otk1aCookieName + "\"]", getNamespaceMap());
                            for (Element cookieElement : cookieElements) {
                                assertEquals(l7otk1aCookieName + versionModifier, cookieElement.getAttribute("stringValue"));
                            }

                            cookieElements = XpathUtil.findElements(writeablePolicyElement, "//L7p:Regex/L7p:Regex[contains(@stringValue,'" +  l7otk1aCookieName + "')]", getNamespaceMap());
                            for (Element cookieElement : cookieElements) {
                                assertThat(cookieElement.getAttribute("stringValue"), containsString(l7otk1aCookieName + versionModifier));
                            }

                            encodedCookieL7otk1a = HexUtils.encodeBase64(HexUtils.encodeUtf8(Syntax.getVariableExpression("cookie." + l7otk1aCookieName + versionModifier)), true);
                            cookieElements = XpathUtil.findElements(writeablePolicyElement, "//L7p:SetVariable/L7p:Base64Expression[@stringValue=\"" + encodedCookieL7otk1a  + "\"]", getNamespaceMap());
                            assertEquals(numCookieL7otk1a, cookieElements.size());

                            cookieElements = XpathUtil.findElements(writeablePolicyElement, "//L7p:AddHeader/L7p:HeaderValue[contains(@stringValue,'" +  l7otk1aCookieName + "')]", getNamespaceMap());
                            for (Element cookieElement : cookieElements) {
                                assertThat(cookieElement.getAttribute("stringValue"), containsString(l7otk1aCookieName + versionModifier));
                            }

                            cookieElements = XpathUtil.findElements(writeablePolicyElement, "//L7p:CookieCredentialSource/L7p:CookieName[@stringValue=\"" +  l7otk1aCookieName + "\"]", getNamespaceMap());
                            for (Element cookieElement : cookieElements) {
                                assertEquals(l7otk1aCookieName + versionModifier, cookieElement.getAttribute("stringValue"));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("Unexpected exception: " + e.getMessage());
                    }

                    installEvent.setProcessed(true);
                }
            }
        }) {
            @NotNull
            @Override
            /**
             * Override OAuthInstallerAdminImpl.getPolicyBundleInstallerCallback(...) to version modifying l7otk1a cookie
             */
            protected PolicyBundleInstallerCallback getPolicyBundleInstallerCallback(final String versionModifier) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
                return new PolicyBundleInstallerCallback() {
                        @Override
                        public void prePolicySave(@NotNull BundleInfo bundleInfo, @NotNull Element entityDetailElmReadOnly, @NotNull Document writeablePolicyDoc) throws CallbackException {

                            // version modify l7otk1a cookie usage
                            if (versionModifier != null) {
                                final Element writeablePolicyElement = writeablePolicyDoc.getDocumentElement();

                                // modify L7p:CookieCredentialSource/L7p:CookieName stringValue=l7otk1a
                                versionModifyStringValueAttribute(XpathUtil.findElements(writeablePolicyElement,
                                        "//L7p:CookieCredentialSource/L7p:CookieName[@stringValue=\"" +  l7otk1aCookieName + "\"]", getNamespaceMap()));

                                // modify L7p:Regex/L7p:Regex stringValue contains l7otk1a
                                versionModifyStringValueAttribute(XpathUtil.findElements(writeablePolicyElement,
                                        "//L7p:Regex/L7p:Regex[contains(@stringValue,'" +  l7otk1aCookieName + "')]", getNamespaceMap()));

                                // modify L7p:SetVariable/L7p:Base64Expression stringValue=${cookie.l7otk1a}
                                String encodedCookieL7otk1a = HexUtils.encodeBase64(HexUtils.encodeUtf8(Syntax.getVariableExpression("cookie." + l7otk1aCookieName)), true);
                                List<Element> cookieElements = XpathUtil.findElements(writeablePolicyElement, "//L7p:SetVariable/L7p:Base64Expression[@stringValue=\"" + encodedCookieL7otk1a + "\"]", getNamespaceMap());
                                for (Element cookieElement : cookieElements) {
                                    final String cookieName = cookieElement.getAttribute("stringValue");
                                    final String decodedCookieName = new String(HexUtils.decodeBase64(cookieName, true), Charsets.UTF8);
                                    final String updatedCookieName = decodedCookieName.replaceAll(l7otk1aCookieName, l7otk1aCookieName + versionModifier);
                                    cookieElement.setAttribute("stringValue", HexUtils.encodeBase64(HexUtils.encodeUtf8(updatedCookieName), true));
                                }

                                // modify L7p:AddHeader L7p:HeaderValue stringValue contains l7otk1a
                                versionModifyStringValueAttribute(XpathUtil.findElements(writeablePolicyElement,
                                        "//L7p:AddHeader/L7p:HeaderValue[contains(@stringValue,'" +  l7otk1aCookieName + "')]", getNamespaceMap()));
                            }
                        }

                        private void versionModifyStringValueAttribute(final List<Element> cookieElements) {
                            for (Element cookieElement : cookieElements) {
                                final String cookieName = cookieElement.getAttribute("stringValue");
                                final String updatedCookieName = cookieName.replaceAll(l7otk1aCookieName, l7otk1aCookieName + versionModifier);
                                cookieElement.setAttribute("stringValue", updatedCookieName);
                            }
                        }
                };
            }
        };

        final String oauth1_0 = "1c2a2874-df8d-4e1d-b8b0-099b576407e1";
        final AsyncAdminMethods.JobId<ArrayList> jobId = admin.install(Arrays.asList(oauth1_0), new Goid(0, -5002), new HashMap<String, BundleMapping>(), versionModifier, false);

        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }
    }

    /**
     * API Portal integration requires storing the policy with the API Portal integrated. At install time this option
     * is off by default and if not chosen the sections of policy with deal with API Portal integration must be removed.
     *
     * This test is hardcoded with a list of folder assertions and individual comments with the left comment
     * 'PORTAL_INTEGRATION'.
     *
     * If this test fails then update only when it's confirmed that the Secure Zone Storage clientstore service's policy
     * was updated for API Portal integration.
     *
     * A future version of the API Portal may require a more complicated policy. The current version can support both
     * versions 2.1 and 2.2 but future versions may not be backwards compatible, in which case the logic to remove
     * support for the API Portal may be more complicated.
     *
     */
    @Test
    public void testVerifyExpectedPortalIntegrationCommentsExist() throws Exception {
        // Set up

        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), productionBaseName);
        final BundleResolver resolver = new BundleResolverImpl(bundleInfos, getClass()) {};

        final Map<String, BundleInfo> bundleMap = toMap(resolver.getResultList(), new Functions.Unary<Pair<String, BundleInfo>, BundleInfo>() {
            @Override
            public Pair<String, BundleInfo> call(BundleInfo bundleInfo) {
                return new Pair<>(bundleInfo.getId(), bundleInfo);
            }
        });

        final BundleInfo secureZoneBundle = bundleMap.get(OAuthInstallerAssertion.SECURE_ZONE_STORAGE_COMP_ID);
        assertNotNull(secureZoneBundle);

        // Find the clientstore service's policy
        final Document serviceDocMgmtEnum = resolver.getBundleItem(secureZoneBundle.getId(), BundleResolver.BundleItem.SERVICE, false);
        final List<Element> serviceElms = GatewayManagementDocumentUtilities.getEntityElements(serviceDocMgmtEnum.getDocumentElement(), "Service");
        final Map<String, Document> serviceNameToPolicyMap = getServicesAndPolicyDocuments(serviceElms);
        final Document clientStorePolicyDoc = serviceNameToPolicyMap.get("oauth/clients");
        assertNotNull(clientStorePolicyDoc);

        // Set up finished
        // validate contents of policy
        final Element policyElm = clientStorePolicyDoc.getDocumentElement();
        // System.out.println(XmlUtil.nodeToFormattedString(policyElm));
        validatePortalIntegrationComments(policyElm, 7, 1, 3, 3);
    }

    /**
     * If the API Portal integration is not required, then the SecureZone clientstore policy needs to be updated
     * to remove all assertions added for API Portal integration.
     *
     */
    @Test
    public void testApiPortalIntegrationNotRequested() throws Exception {
        final boolean[] testPass = new boolean[1];
        final OAuthInstallerAdminImpl admin = new OAuthInstallerAdminImpl(productionBaseName, productionWsmanPolicyBundleInfoFileName, productionWsmanPolicyBundleInfoNamespace, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {

                if (applicationEvent instanceof InstallPolicyBundleEvent) {
                    InstallPolicyBundleEvent installEvent = (InstallPolicyBundleEvent) applicationEvent;
                    final PolicyBundleInstallerCallback policyBundleInstallerCallback = installEvent.getPolicyBundleInstallerCallback();
                    if (policyBundleInstallerCallback == null) {
                        fail("Policy call back should be configured.");
                    }
                    final BundleInfo bundleInfo = installEvent.getContext().getBundleInfo();
                    try {

                        final Document serviceEnum = installEvent.getContext().getBundleResolver().getBundleItem(bundleInfo.getId(), BundleResolver.BundleItem.SERVICE, false);
                        final List<Element> gatewayMgmtPolicyElments = getEntityElements(serviceEnum.getDocumentElement(), "Service");
                        for (Element serviceEnumElm : gatewayMgmtPolicyElments) {
                            final Element policyResourceElmWritable = getPolicyResourceElement(serviceEnumElm, "Service", "not used");
                            assert policyResourceElmWritable != null;
                            final Document policyDocWriteable = getPolicyDocumentFromResource(policyResourceElmWritable, "Service", "not used");
                            final Element serviceDetailElement = getServiceDetailElement(serviceEnumElm);
                            policyBundleInstallerCallback.prePolicySave(bundleInfo, serviceDetailElement, policyDocWriteable);
                            // Verify elements removed
                            final String entityName = getEntityName(serviceDetailElement);
                            if ("oauth/clients".equals(entityName)) {
                                validatePortalIntegrationComments(policyDocWriteable.getDocumentElement(), 0, 0, 0, 0);
                                testPass[0] = true;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("Unexpected exception: " + e.getMessage());
                    }

                    installEvent.setProcessed(true);
                }
            }
        });

        // Secure Zone Storage
        // false to not integrate the API Portal
        final AsyncAdminMethods.JobId<ArrayList> jobId =
                admin.install(Arrays.asList("b082274b-f00e-4fbf-bbb7-395a95ca2a35"), new Goid(0, -5002), new HashMap<String, BundleMapping>(), null, false);

        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }

        assertTrue(testPass[0]);
    }

    /**
     * The 'Manage Clients' button is not available when the API Portal is integrated with the OTK.
     *
     * This test is a heuristic, it looks for the presence of the variables and template responses needed and if it finds
     * them it assumes that the policies contain the correct logic. It is possible that the policy logic is broken and
     * this test will pass. The intent of this test is to avoid someone changing the error message or removing the
     * required values entirely.
     *
     * This test validates the connection between the manager and clientstore endpoints.
     */
    @BugId("SSG-6456")
    @Test
    public void testManageClientsNotAvailableWhenPortalIntegrated() throws Exception {

        final List<Pair<BundleInfo, String>> bundleInfos = BundleUtils.getBundleInfos(getClass(), productionBaseName);
        final BundleResolver resolver = new BundleResolverImpl(bundleInfos, getClass()) {};

        final Map<String, BundleInfo> bundleMap = Functions.toMap(resolver.getResultList(), new Functions.Unary<Pair<String, BundleInfo>, BundleInfo>() {
            @Override
            public Pair<String, BundleInfo> call(BundleInfo bundleInfo) {
                return new Pair<>(bundleInfo.getId(), bundleInfo);
            }
        });

        final String expectedValue = "API Portal Integration is configured";
        {
            final BundleInfo clientStoreBundle = bundleMap.get("b082274b-f00e-4fbf-bbb7-395a95ca2a35");
            assertNotNull(clientStoreBundle);

            final Document serviceDocument = resolver.getBundleItem(clientStoreBundle.getId(), BundleResolver.BundleItem.SERVICE, false);
            final List<Element> enumServiceElms = getEntityElements(serviceDocument.getDocumentElement(), "Service");
            // find the client store policy

            boolean foundVariable = false;
            boolean foundTemplateResponse = false;
            for (Element serviceElm : enumServiceElms) {
                final Element serviceDetailElement = getServiceDetailElement(serviceElm);
                final String entityName = getEntityName(serviceDetailElement);
                if (!"oauth/clients".equals(entityName)) {
                    continue;
                }

                final Element policyResourceElement = getPolicyResourceElement(serviceElm, "Service", "Not used");
                assert policyResourceElement != null;
                final Document layer7Policy = getPolicyDocumentFromResource(policyResourceElement, "Service", "Not used");
                final List<Element> contextVariables = PolicyUtils.findContextVariables(layer7Policy.getDocumentElement());
                // find customError
                for (Element contextVariable : contextVariables) {
                    final Element variableToSet = DomUtils.findExactlyOneChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "VariableToSet");
                    final String varName = variableToSet.getAttribute("stringValue");
                    if ("customError".equals(varName)) {
                        final Element base64Expression = DomUtils.findExactlyOneChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "Base64Expression");
                        final byte[] value = HexUtils.decodeBase64(base64Expression.getAttribute("stringValue"));
                        final String varValue = new String(value, Charsets.UTF8);

                        if (varValue.contains(expectedValue)) {
                            foundVariable = true;
                        }
                    }
                }

                final List<Element> templateResponses = PolicyUtils.findTemplateResponses(layer7Policy.getDocumentElement());
                for (Element templateResponse : templateResponses) {
                    final Element base64ResponseBody = DomUtils.findExactlyOneChildElementByName(templateResponse, "http://www.layer7tech.com/ws/policy", "Base64ResponseBody");
                    final byte[] value = HexUtils.decodeBase64(base64ResponseBody.getAttribute("stringValue"));
                    final String varValue = new String(value, Charsets.UTF8);
                    if (varValue.contains("${customError}")) {
                        foundTemplateResponse = true;
                    }
                }
            }

            assertTrue("Did not find customError variable with the correct value", foundVariable);
            assertTrue("Did not find a template response which references ${customError}", foundTemplateResponse);
        }

        // client store has passed, now test the manager policy

        {
            final BundleInfo managerBundle = bundleMap.get("a07924c0-0265-42ea-90f1-2428e31ae5ae");
            assertNotNull(managerBundle);

            final Document serviceDocument = resolver.getBundleItem(managerBundle.getId(), BundleResolver.BundleItem.SERVICE, false);
            final List<Element> enumServiceElms = getEntityElements(serviceDocument.getDocumentElement(), "Service");

            boolean foundCorrectComparison = false;
            for (Element serviceElm : enumServiceElms) {
                final Element serviceDetailElement = getServiceDetailElement(serviceElm);
                final String entityName = getEntityName(serviceDetailElement);
                if (!"oauth/manager".equals(entityName)) {
                    continue;
                }

                final Element policyResourceElement = getPolicyResourceElement(serviceElm, "Service", "Not used");
                assert policyResourceElement != null;
                final Document layer7Policy = getPolicyDocumentFromResource(policyResourceElement, "Service", "Not used");
                final List<Element> comparisonAssertions = PolicyUtils.findComparisonAssertions(layer7Policy.getDocumentElement());
                for (Element comparisonAssertion : comparisonAssertions) {
                    final Element expression2 = DomUtils.findExactlyOneChildElementByName(comparisonAssertion, "http://www.layer7tech.com/ws/policy", "Expression2");
                    final String expression2Value = expression2.getAttribute("stringValue");

                    final Element rightValueElm = DomUtils.findFirstDescendantElement(comparisonAssertion, "http://www.layer7tech.com/ws/policy", "RightValue");
                    assert rightValueElm != null;
                    final String rightValue = rightValueElm.getAttribute("stringValue");

                    if (expectedValue.equals(expression2Value) && expectedValue.equals(rightValue)) {
                        foundCorrectComparison = true;
                    }
                }
            }

            assertTrue("Did not find check for correct error message", foundCorrectComparison);
        }

    }



    /**
     * Comments are required on all portal integration assertions. The presence of the specific comment
     * implies the assertion, all all it's childern if it's an All, are for portal integration only.
     */
    private void validatePortalIntegrationComments(Element policyElm,
                                                   final int totalCommentsExpected,
                                                   final int setVariableFoundExpected,
                                                   final int comparisonFoundExpected,
                                                   final int allFoundExpected) {
        final List<Element> foundComments = XpathUtil.findElements(policyElm, ".//L7p:value[@stringValue='PORTAL_INTEGRATION']", getNamespaceMap());
        assertEquals("Wrong number of PORTAL_INTEGRATION comments found", totalCommentsExpected, foundComments.size());

        // verify they are all left comments
        int setVariableFound = 0;
        int comparionsFound = 0;
        int allFound = 0;
        for (Element foundComment : foundComments) {
            final Element parentNode = (Element) foundComment.getParentNode();
            final List<Element> elements = XpathUtil.findElements(parentNode, ".//L7p:key[@stringValue='LEFT.COMMENT']", getNamespaceMap());
            assertNotNull(elements);
            assertEquals(1, elements.size());
            final Node assertionNode = parentNode.getParentNode().getParentNode().getParentNode();
            final String assertionName = assertionNode.getLocalName();
            System.out.println(assertionName);
            switch (assertionName) {
                case "SetVariable":
                    setVariableFound++;
                    break;
                case "ComparisonAssertion":
                    comparionsFound++;
                    break;
                case "All":
                    allFound++;
                    break;
            }
        }

        // 3 assertions and 2 all folders
        assertEquals(setVariableFoundExpected, setVariableFound);
        assertEquals(comparisonFoundExpected, comparionsFound);
        assertEquals(allFoundExpected, allFound);
    }

    private Map<String, Document> getServicesAndPolicyDocuments(List<Element> serviceMgmtElements) {
        return toMap(serviceMgmtElements, new Functions.Unary<Pair<String, Document>, Element>() {
            @Override
            public Pair<String, Document> call(Element serviceElement) {
                try {
                    final Element serviceDetailElement = getServiceDetailElement(serviceElement);
                    final String serviceName = getEntityName(serviceDetailElement);
                    final Element resourceElement = getPolicyResourceElement(serviceElement, "Service", "Not Used");
                    assertNotNull(resourceElement);
                    final Document layer7Policy = getPolicyDocumentFromResource(resourceElement, "Policy", "Not Used");
                    return new Pair<>(serviceName, layer7Policy);
                } catch (BundleResolver.InvalidBundleException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
