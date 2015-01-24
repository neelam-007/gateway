package com.l7tech.server.policy.bundle;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.util.Functions;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static com.l7tech.server.policy.bundle.PolicyBundleInstallerAbstractServerAssertion.Action.*;
import static org.junit.Assert.assertEquals;
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

    private final String restmanBundleId = "33b16742-d62d-4095-8f8d-4db707e9ad52";
    @Mock
    private BundleInfo restmanBundleInfo;
    private final String wsmanBundleId = "33b16742-d62d-4095-8f8d-4db707e9ad50";
    @Mock
    private BundleInfo wsmanBundleInfo;

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
    }

    @Test
    public void getComponentIds() throws Exception {
        final HashMap<String, BundleInfo> availableComponents = new HashMap<>(2);
        availableComponents.put(restmanBundleId, restmanBundleInfo);
        availableComponents.put(wsmanBundleId, wsmanBundleInfo);

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
                HashMap<String, BundleInfo> availableComponents = new HashMap<>(1);
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
}
