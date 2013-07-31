package com.l7tech.external.assertions.salesforceinstaller;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;

import static com.l7tech.external.assertions.salesforceinstaller.SalesforceInstallerAdminImpl.SalesforceInstallationAuditEvent;
import static org.junit.Assert.*;

public class SalesforceInstallerAdminImplTest {

    private final String baseName = "/com/l7tech/external/assertions/salesforceinstaller/bundles/";

    /**
     * Validates that the correct number and type of spring events are published for a dry run.
     */
    @Test
    public void testValidateSpringRequestsForDryRun() throws Exception {
        final Map<String, Boolean> foundBundles = new HashMap<String, Boolean>();
        final Set<String> foundAuditEvents = new HashSet<String>();

        final SalesforceInstallerAdminImpl admin = new SalesforceInstallerAdminImpl(baseName, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {
                // note this code will run on a separate thread to the actual test so failures here will not
                // be reported in junit

                if (applicationEvent instanceof DryRunInstallPolicyBundleEvent) {
                    DryRunInstallPolicyBundleEvent dryRunEvent = (DryRunInstallPolicyBundleEvent) applicationEvent;

                    foundBundles.put(dryRunEvent.getContext().getBundleInfo().getId(), true);
                    dryRunEvent.setProcessed(true);
                } else if (applicationEvent instanceof SalesforceInstallationAuditEvent) {
                    final SalesforceInstallationAuditEvent auditEvent = (SalesforceInstallationAuditEvent) applicationEvent;
                    foundAuditEvents.add(auditEvent.getNote() + auditEvent.getAuditDetails().toString());
                }
            }
        });

        final AsyncAdminMethods.JobId<PolicyBundleDryRunResult> jobId = admin.dryRunInstall(
                Arrays.asList("6a88602e-df72-414a-9f88-9849197c8b7f"), new HashMap<String, BundleMapping>(), null);

        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }

        final AsyncAdminMethods.JobResult<PolicyBundleDryRunResult> dryRunResultJobResult = admin.getJobResult(jobId);

        final PolicyBundleDryRunResult dryRunResult = dryRunResultJobResult.result;

        assertNotNull(dryRunResult);

        assertEquals("One bundle was configured so one event should have been published", 1, foundBundles.size());

        assertTrue(foundBundles.containsKey("6a88602e-df72-414a-9f88-9849197c8b7f"));

        // validate audits
        assertFalse("Installation audits should have been generated.", foundAuditEvents.isEmpty());
        for (String foundAuditEvent : foundAuditEvents) {
            System.out.println(foundAuditEvent);
        }
        assertTrue(foundAuditEvents.contains("Pre installation check of the Toolkit started[]"));
        assertTrue(foundAuditEvents.contains("Pre installation check of the Toolkit completed[]"));
    }

    /**
     * Validates that the correct number and type of spring events are published for install.
     */
    @Test
    public void testValidateSpringRequestsForInstall() throws Exception {
        final Set<String> foundAuditEvents = new HashSet<String>();
        final SalesforceInstallerAdminImpl admin = new SalesforceInstallerAdminImpl(baseName, new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent applicationEvent) {
                // note this code will run on a separate thread to the actual test so failures here will not
                // be reported in junit

                if (applicationEvent instanceof InstallPolicyBundleEvent) {
                    InstallPolicyBundleEvent installEvent = (InstallPolicyBundleEvent) applicationEvent;
                    installEvent.setProcessed(true);
                } else if (applicationEvent instanceof SalesforceInstallerAdminImpl.SalesforceInstallationAuditEvent) {
                    final SalesforceInstallerAdminImpl.SalesforceInstallationAuditEvent auditEvent = (SalesforceInstallerAdminImpl.SalesforceInstallationAuditEvent) applicationEvent;
                    foundAuditEvents.add(auditEvent.getNote() + auditEvent.getAuditDetails().toString());
                }
            }
        });

        final AsyncAdminMethods.JobId<ArrayList> jobId = admin.install(Arrays.asList("6a88602e-df72-414a-9f88-9849197c8b7f"), new HashMap<String, BundleMapping>(), null);

        while (!admin.getJobStatus(jobId).startsWith("inactive")) {
            Thread.sleep(10L);
        }

        final AsyncAdminMethods.JobResult<ArrayList> installResult = admin.getJobResult(jobId);
        final ArrayList results = installResult.result;

        assertNotNull(results);
        assertEquals("One bundle was configured so event should have been published", 1, results.size());

        assertTrue(results.contains("6a88602e-df72-414a-9f88-9849197c8b7f"));

        // validate audits
        assertFalse("Installation audits should have been generated.", foundAuditEvents.isEmpty());
        for (String foundAuditEvent : foundAuditEvents) {
            System.out.println(foundAuditEvent);
        }
        assertTrue(foundAuditEvents.contains("Installation of the Toolkit completed []"));
        assertTrue(foundAuditEvents.contains("Installation of the Toolkit started []"));
    }
}