package com.l7tech.server.policy.bundle;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.message.AbstractHttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Functions;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static com.l7tech.server.policy.bundle.PolicyBundleInstallerAbstractServerAssertion.Action.*;
import static com.l7tech.server.policy.bundle.PolicyBundleInstallerAbstractServerAssertion.COMPONENT_ID_SEPARATOR;
import static com.l7tech.server.policy.bundle.PolicyBundleInstallerAbstractServerAssertion.COMPONENT_ID_SEPARATOR_CHAR;
import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PolicyBundleInstallerAbstractServerAssertionTest {
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private Assertion assertion;
    @Mock
    private AssertionMetadata meta;
    @Mock
    private Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext> factory;
    @Mock
    private Collection<ExtensionInterfaceBinding> bindings;
    @Mock
    private Iterator<ExtensionInterfaceBinding> bindingsIterator;
    @Mock
    private ExtensionInterfaceBinding<?> binding;
    @Mock
    private PolicyBundleInstallerAdmin policyBundleInstallerAdmin;
    @Mock
    private PolicyEnforcementContext policyEnforcementContext;
    @Spy
    StashManagerFactory stashManagerFactory = new StashManagerFactory() {
        @Override
        public StashManager createStashManager() {
            return new ByteArrayStashManager();
        }
    };

    private final String restmanBundleId = "33b16742-d62d-4095-8f8d-4db707e9ad52";
    @Mock
    private BundleInfo restmanBundleInfo;
    private final String wsmanBundleId = "33b16742-d62d-4095-8f8d-4db707e9ad50";
    @Mock
    private BundleInfo wsmanBundleInfo;
    final HashMap<String, BundleInfo> availableComponents = new LinkedHashMap<>(2);

    @Before
    public void setup() {
        // constructor setup for PolicyBundleInstallerAbstractServerAssertion
        when(assertion.meta()).thenReturn(meta);
        when(meta.get(AssertionMetadata.EXTENSION_INTERFACES_FACTORY)).thenReturn(factory);
        when(factory.call(applicationContext)).thenReturn(bindings);
        when(bindings.size()).thenReturn(1);
        when(bindings.iterator()).thenReturn(bindingsIterator);
        when(bindingsIterator.next()).thenReturn(binding);
        when(binding.getImplementationObject()).thenReturn(policyBundleInstallerAdmin);

        // setup bundles
        when(restmanBundleInfo.getId()).thenReturn(restmanBundleId);
        when(restmanBundleInfo.hasActiveVersionMigrationBundleFile()).thenReturn(true);
        when(wsmanBundleInfo.getId()).thenReturn(wsmanBundleId);
        when(wsmanBundleInfo.hasWsmanFile()).thenReturn(true);
        availableComponents.put(restmanBundleId, restmanBundleInfo);
        availableComponents.put(wsmanBundleId, wsmanBundleInfo);
    }

    @Test
    public void getComponentIds() throws Exception {
        // test HTTP 400 Bad Request
        PolicyBundleInstallerAbstractServerAssertion testInstaller = new PolicyBundleInstallerAbstractServerAssertion<Assertion>(assertion, applicationContext) {
            @Override
            protected String getContextVariable(@NotNull String name) throws NoSuchVariableException {
                return restmanBundleId + COMPONENT_ID_SEPARATOR +  wsmanBundleId;
            }

            @Override
            protected Map<String, BundleInfo> getAvailableComponents() {
                return availableComponents;
            }
        };
        try {
            testInstaller.getComponentIds(restman_get);
            fail("Expected HTTP 400 Bad Request: component not restman");
        } catch (PolicyBundleInstallerServerAssertionException e) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, e.getHttpStatusCode());
        }
        try {
            testInstaller.getComponentIds(wsman_dry_run);
            fail("Expected HTTP 400 Bad Request: component not wsman");
        } catch (PolicyBundleInstallerServerAssertionException e) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, e.getHttpStatusCode());
        }
        try {
            testInstaller.getComponentIds(wsman_install);
            fail("Expected HTTP 400 Bad Request: component not wsman");
        } catch (PolicyBundleInstallerServerAssertionException e) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, e.getHttpStatusCode());
        }

        // test HTTP 404 Not Found
        testInstaller = new PolicyBundleInstallerAbstractServerAssertion<Assertion>(assertion, applicationContext) {
            @Override
            protected String getContextVariable(@NotNull String name) throws NoSuchVariableException {
                return "some_non_existent_component_id";
            }

            @Override
            protected Map<String, BundleInfo> getAvailableComponents() {
                return availableComponents;
            }
        };
        try {
            testInstaller.getComponentIds(wsman_install);
            fail("Expected HTTP 404 Not Found: component ID not found");
        } catch (PolicyBundleInstallerServerAssertionException e) {
            assertEquals(HttpStatus.SC_NOT_FOUND, e.getHttpStatusCode());
        }
    }

    @Test
    public void list() throws Exception {
        // setup response for action to populate
        final Message response = new Message();
        response.attachHttpResponseKnob(new AbstractHttpResponseKnob() {});
        when(policyEnforcementContext.getResponse()).thenReturn(response);

        // test list action output equals installer component IDs
        final String installerComponentIds = restmanBundleId + COMPONENT_ID_SEPARATOR +  wsmanBundleId + COMPONENT_ID_SEPARATOR;
        PolicyBundleInstallerAbstractServerAssertion testInstaller = new PolicyBundleInstallerAbstractServerAssertion<Assertion>(assertion, applicationContext) {
            @Override
            protected String getContextVariable(@NotNull String name) throws NoSuchVariableException {
                return installerComponentIds;
            }

            @Override
            protected Map<String, BundleInfo> getAvailableComponents() {
                return availableComponents;
            }

            @Override
            protected PolicyEnforcementContext getContext() {
                return policyEnforcementContext;
            }

            @Override
            protected StashManagerFactory getStashManagerFactory() {
                return PolicyBundleInstallerAbstractServerAssertionTest.this.stashManagerFactory;
            }
        };
        testInstaller.list();
        assertEquals(HttpStatus.SC_OK, response.getHttpResponseKnob().getStatus());
        assertEquals(installerComponentIds, IOUtils.toString(response.getMimeKnob().getEntireMessageBodyAsInputStream()));
    }

    @Test
    public void restmanGet() throws Exception {
        // by-pass async admin method job
        final AsyncAdminMethods.JobId<PolicyBundleDryRunResult> jobId = new AsyncAdminMethods.JobId<>("1".getBytes(), PolicyBundleDryRunResult.class);
        PolicyBundleDryRunResult dryRunResult = mock(PolicyBundleDryRunResult.class);
        final AsyncAdminMethods.JobResult<PolicyBundleDryRunResult> jobResult = new AsyncAdminMethods.JobResult<>("done", dryRunResult, null, null);
        when(policyBundleInstallerAdmin.getJobStatus(jobId)).thenReturn("done");
        when(policyBundleInstallerAdmin.getJobResult(jobId)).thenReturn(jobResult);

        // setup response for action to populate
        final Message response = new Message();
        response.attachHttpResponseKnob(new AbstractHttpResponseKnob() {});
        when(policyEnforcementContext.getResponse()).thenReturn(response);

        // test restman get action retrieves the bundle xml
        PolicyBundleInstallerAbstractServerAssertion testInstaller = new PolicyBundleInstallerAbstractServerAssertion<Assertion>(assertion, applicationContext) {
            @Override
            protected String getContextVariable(@NotNull String name) throws NoSuchVariableException {
                String result = null;
                if ((CONTEXT_VARIABLE_PREFIX + "folder_goid").equals(name)) {
                    result = "0000000000000000ffffffffffffec76";   // root folder
                } else if ((CONTEXT_VARIABLE_PREFIX + "component_ids").equals(name)) {
                    result = restmanBundleId;
                }
                return result;
            }

            @Override
            protected Map<String, BundleInfo> getAvailableComponents() {
                // restman only bundle
                final HashMap<String, BundleInfo> availableComponents = new HashMap<>(1);
                availableComponents.put(restmanBundleId, restmanBundleInfo);
                return availableComponents;
            }

            @Override
            protected AsyncAdminMethods.JobId<PolicyBundleDryRunResult> callAdminDryRun(
                    List<String> componentIds, Goid folder, HashMap<String, BundleMapping> mappings, String versionModifier) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
                return jobId;
            }

            @Override
            protected PolicyEnforcementContext getContext() {
                return policyEnforcementContext;
            }

            @Override
            protected StashManagerFactory getStashManagerFactory() {
                return PolicyBundleInstallerAbstractServerAssertionTest.this.stashManagerFactory;
            }
        };
        final String bundleXml = "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:References>\n" +
                "        <l7:Item>\n" +
                "         ...\n" +
                "        </l7:Mapping>\n" +
                "    </l7:Mappings>\n" +
                "</l7:Bundle>";
        Map<String, String> componentIdToBundleXmlMap = new HashMap<>(1);
        componentIdToBundleXmlMap.put(restmanBundleId, bundleXml);
        when(dryRunResult.getComponentIdToBundleXmlMap()).thenReturn(componentIdToBundleXmlMap);
        testInstaller.restmanGet();
        assertEquals(HttpStatus.SC_OK, response.getHttpResponseKnob().getStatus());
        assertThat(IOUtils.toString(response.getMimeKnob().getEntireMessageBodyAsInputStream()), startsWith(bundleXml));   // ignore trailing newline
    }

    @Test
    public void wsmanDryRun() throws Exception {
        // by-pass async admin method job
        final AsyncAdminMethods.JobId<PolicyBundleDryRunResult> jobId = new AsyncAdminMethods.JobId<>("1".getBytes(), PolicyBundleDryRunResult.class);
        PolicyBundleDryRunResult dryRunResult = mock(PolicyBundleDryRunResult.class);
        final AsyncAdminMethods.JobResult<PolicyBundleDryRunResult> jobResult = new AsyncAdminMethods.JobResult<>("done", dryRunResult, null, null);
        when(policyBundleInstallerAdmin.getJobStatus(jobId)).thenReturn("done");
        when(policyBundleInstallerAdmin.getJobResult(jobId)).thenReturn(jobResult);

        // test HTTP 409 Conflict
        PolicyBundleInstallerAbstractServerAssertion testInstaller = new PolicyBundleInstallerAbstractServerAssertion<Assertion>(assertion, applicationContext) {
            @Override
            protected String getContextVariable(@NotNull String name) throws NoSuchVariableException {
                String result = null;
                if ((CONTEXT_VARIABLE_PREFIX + "folder_goid").equals(name)) {
                    result = "0000000000000000ffffffffffffec76";   // root folder
                } else if ((CONTEXT_VARIABLE_PREFIX + "component_ids").equals(name)) {
                    result = wsmanBundleId;
                }
                return result;
            }

            @Override
            protected Map<String, BundleInfo> getAvailableComponents() {
                // wsman only bundle
                final HashMap<String, BundleInfo> availableComponents = new HashMap<>(1);
                availableComponents.put(wsmanBundleId, wsmanBundleInfo);
                return availableComponents;
            }

            @Override
            protected AsyncAdminMethods.JobId<PolicyBundleDryRunResult> callAdminDryRun(
                    List<String> componentIds, Goid folder, HashMap<String, BundleMapping> mappings, String versionModifier) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
                return jobId;
            }
        };
        when(dryRunResult.anyConflictsForBundle(wsmanBundleId)).thenReturn(true);
        try {
            testInstaller.wsmanDryRun();
            fail("Expected HTTP 409 Conflict: wsman dry run conflicts expected");
        } catch (PolicyBundleInstallerServerAssertionException e) {
            assertEquals(HttpStatus.SC_CONFLICT, e.getHttpStatusCode());
        }
    }

    @Test
    public void wsmanInstall() throws Exception {
        // by-pass async admin method job
        final AsyncAdminMethods.JobId<ArrayList> jobId = new AsyncAdminMethods.JobId<>("1".getBytes(), ArrayList.class);
        ArrayList installedComponentIds = new ArrayList(1);
        installedComponentIds.add(wsmanBundleId);
        final AsyncAdminMethods.JobResult<ArrayList> jobResult = new AsyncAdminMethods.JobResult<>("done", installedComponentIds, null, null);
        when(policyBundleInstallerAdmin.getJobStatus(jobId)).thenReturn("done");
        when(policyBundleInstallerAdmin.getJobResult(jobId)).thenReturn(jobResult);

        // setup response for action to populate
        final Message response = new Message();
        response.attachHttpResponseKnob(new AbstractHttpResponseKnob() {});
        when(policyEnforcementContext.getResponse()).thenReturn(response);

        // test wsman install action returns successful component id
        PolicyBundleInstallerAbstractServerAssertion testInstaller = new PolicyBundleInstallerAbstractServerAssertion<Assertion>(assertion, applicationContext) {
            @Override
            protected String getContextVariable(@NotNull String name) throws NoSuchVariableException {
                String result = null;
                if ((CONTEXT_VARIABLE_PREFIX + "folder_goid").equals(name)) {
                    result = "0000000000000000ffffffffffffec76";   // root folder
                } else if ((CONTEXT_VARIABLE_PREFIX + "component_ids").equals(name)) {
                    result = wsmanBundleId;
                }
                return result;
            }

            @Override
            protected Map<String, BundleInfo> getAvailableComponents() {
                // wsman only bundle
                final HashMap<String, BundleInfo> availableComponents = new HashMap<>(1);
                availableComponents.put(wsmanBundleId, wsmanBundleInfo);
                return availableComponents;
            }

            @Override
            protected AsyncAdminMethods.JobId<ArrayList> callAdminInstall(
                    List<String> componentIds, Goid folder, HashMap<String, BundleMapping> mappings, String versionModifier) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
                return jobId;
            }

            @Override
            protected PolicyEnforcementContext getContext() {
                return policyEnforcementContext;
            }

            @Override
            protected StashManagerFactory getStashManagerFactory() {
                return PolicyBundleInstallerAbstractServerAssertionTest.this.stashManagerFactory;
            }
        };
        testInstaller.wsmanInstall();
        assertEquals(HttpStatus.SC_OK, response.getHttpResponseKnob().getStatus());
        assertEquals(join(installedComponentIds, COMPONENT_ID_SEPARATOR_CHAR), IOUtils.toString(response.getMimeKnob().getEntireMessageBodyAsInputStream()));
    }
}
