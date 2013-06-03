package com.l7tech.external.assertions.salesforceinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UnknownAssertion;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.admin.DetailedAdminEvent;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.PolicyBundleEvent;
import com.l7tech.server.event.wsman.WSManagementRequestEvent;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.BundleUtils;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.event.AdminInfo.find;

/**
 * Salesforce Installer Admin implementation, which is exposed via an admin extension interface.
 */
public class SalesforceInstallerAdminImpl extends AsyncAdminMethodsImpl implements SalesforceInstallerAdmin {

    public static final String NS_INSTALLER_VERSION = "http://ns.l7tech.com/2013/02/salesforce-bundle";
    public static final String EXECUTE_SALESFORCE_OPERATION_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
        "    <wsp:All wsp:Usage=\"Required\">\n" +
        "        <L7p:CustomAssertion>\n" +
        "            <L7p:base64SerializedValue>rO0ABXNyADFjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uQ3VzdG9tQXNzZXJ0aW9uSG9sZGVyZtcreFwddTICAANMAAhjYXRlZ29yeXQAKkxjb20vbDd0ZWNoL3BvbGljeS9hc3NlcnRpb24vZXh0L0NhdGVnb3J5O0wAD2N1c3RvbUFzc2VydGlvbnQAMUxjb20vbDd0ZWNoL3BvbGljeS9hc3NlcnRpb24vZXh0L0N1c3RvbUFzc2VydGlvbjtMAA9kZXNjcmlwdGlvblRleHR0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAlY29tLmw3dGVjaC5wb2xpY3kuYXNzZXJ0aW9uLkFzc2VydGlvbttfY5k8vaKxAgACWgAHZW5hYmxlZEwAEGFzc2VydGlvbkNvbW1lbnR0AC9MY29tL2w3dGVjaC9wb2xpY3kvYXNzZXJ0aW9uL0Fzc2VydGlvbiRDb21tZW50O3hwAXBzcgAoY29tLmw3dGVjaC5wb2xpY3kuYXNzZXJ0aW9uLmV4dC5DYXRlZ29yeVqwnGWhRP41AgACSQAFbXlLZXlMAAZteU5hbWVxAH4AA3hwAAAACHQADk1lc3NhZ2VSb3V0aW5nc3IAQ2NvbS5sN3RlY2guY3VzdG9tLnNhbGVzZm9yY2UuYXNzZXJ0aW9uLlNhbGVzZm9yY2VPcGVyYXRpb25Bc3NlcnRpb24AAAAAAAAAAQIAEUwADmNvbm5lY3Rpb25EYXRhdABHTGNvbS9sN3RlY2gvY3VzdG9tL3NhbGVzZm9yY2UvYXNzZXJ0aW9uL2hlbHBlcmFzc2VydGlvbi9Db25uZWN0aW9uRGF0YTtMABFjcmVhdGVTT2JqZWN0RGF0YXQASkxjb20vbDd0ZWNoL2N1c3RvbS9zYWxlc2ZvcmNlL2Fzc2VydGlvbi9oZWxwZXJhc3NlcnRpb24vQ3JlYXRlU09iamVjdERhdGE7TAAVY3JlYXRlU09iamVjdERhdGFMaXN0dAAQTGphdmEvdXRpbC9MaXN0O0wAF2Rlc2NTT2JqZWN0TWV0YWRhdGFMaXN0cQB+AA1MABdkZXNjU29iamVjdE1ldGFkYXRhTGlzdHEAfgANTAAQZXhlY3V0ZVF1ZXJ5RGF0YXQASUxjb20vbDd0ZWNoL2N1c3RvbS9zYWxlc2ZvcmNlL2Fzc2VydGlvbi9oZWxwZXJhc3NlcnRpb24vRXhlY3V0ZVF1ZXJ5RGF0YTtMABdsYXN0Q29ubmVjdGlvblRpbWVTdGFtcHQAFExqYXZhL3NxbC9UaW1lc3RhbXA7TAARcHJldlNPYmplY3RBY3Rpb25xAH4AA0wAEnJldHJpZXZlQWN0aW9uRGF0YXQAS0xjb20vbDd0ZWNoL2N1c3RvbS9zYWxlc2ZvcmNlL2Fzc2VydGlvbi9oZWxwZXJhc3NlcnRpb24vUmV0cmlldmVBY3Rpb25EYXRhO0wAGnJldHJpZXZlRGVsZXRlZE9iamVjdHNEYXRhdABTTGNvbS9sN3RlY2gvY3VzdG9tL3NhbGVzZm9yY2UvYXNzZXJ0aW9uL2hlbHBlcmFzc2VydGlvbi9SZXRyaWV2ZURlbGV0ZWRPYmplY3RzRGF0YTtMABFyZXRyaWV2ZUZpZWxkTGlzdHEAfgANTAAbcmV0cmlldmVNb2RpZmllZE9iamVjdHNEYXRhdABUTGNvbS9sN3RlY2gvY3VzdG9tL3NhbGVzZm9yY2UvYXNzZXJ0aW9uL2hlbHBlcmFzc2VydGlvbi9SZXRyaWV2ZU1vZGlmaWVkT2JqZWN0c0RhdGE7TAANc09iamVjdEFjdGlvbnEAfgADTAAPc2VhcmNoUXVlcnlEYXRhdABKTGNvbS9sN3RlY2gvY3VzdG9tL3NhbGVzZm9yY2UvYXNzZXJ0aW9uL2hlbHBlcmFzc2VydGlvbi9TZWFyY2hPYmplY3RzRGF0YTtMABF1cGRhdGVTT2JqZWN0RGF0YXQASkxjb20vbDd0ZWNoL2N1c3RvbS9zYWxlc2ZvcmNlL2Fzc2VydGlvbi9oZWxwZXJhc3NlcnRpb24vVXBkYXRlU09iamVjdERhdGE7TAAVdXBkYXRlU09iamVjdERhdGFMaXN0cQB+AA1bAAN2YXJ0AC5bTGNvbS9sN3RlY2gvcG9saWN5L3ZhcmlhYmxlL1ZhcmlhYmxlTWV0YWRhdGE7eHBwcHBwcHBwcHBwcHBwcHBwdXIALltMY29tLmw3dGVjaC5wb2xpY3kudmFyaWFibGUuVmFyaWFibGVNZXRhZGF0YTvCENmz6riGNgIAAHhwAAAAAHA=</L7p:base64SerializedValue>\n" +
        "        </L7p:CustomAssertion>\n" +
        "    </wsp:All>\n" +
        "</wsp:Policy>";

    AssertionChecker assertionChecker = new AssertionChecker();

    public SalesforceInstallerAdminImpl(final String bundleBaseName, ApplicationEventPublisher appEventPublisher) throws SalesforceInstallationException {
        this.appEventPublisher = appEventPublisher;
        this.executorService = Executors.newCachedThreadPool();

        final String salesforceBundleInfo = bundleBaseName + "SalesforceBundleInfo.xml";
        final URL salesforceBundleInfoUrl = getClass().getResource(salesforceBundleInfo);
        if (salesforceBundleInfoUrl == null) {
            throw new SalesforceInstallationException("Could not find SalesforceBundleInfo.xml");
        }

        final byte[] bundleBytes;
        try {
            bundleBytes = IOUtils.slurpUrl(salesforceBundleInfoUrl);
        } catch (IOException e) {
            throw new SalesforceInstallationException(e);
        }

        final Document salesforceInfoDoc;
        try {
            salesforceInfoDoc = XmlUtil.parse(new ByteArrayInputStream(bundleBytes));
        } catch (IOException e) {
            throw new SalesforceInstallationException("Unable to parse resource: " + salesforceBundleInfo, e);
        } catch (SAXException e) {
            throw new SalesforceInstallationException("Unable to parse resource: " + salesforceBundleInfo, e);
        }

        try {
            final Element versionElm = XmlUtil.findExactlyOneChildElementByName(salesforceInfoDoc.getDocumentElement(), NS_INSTALLER_VERSION, "Version");
            installerVersion = DomUtils.getTextValue(versionElm, true);
            if (installerVersion.isEmpty()) {
                throw new SalesforceInstallationException("Could not get version information for Salesforce Toolkit");
            }
        } catch (Exception e) {
            throw new SalesforceInstallationException("Could not find version information in: " + salesforceBundleInfo, e);
        }

        try {
            bundleInfosFromJar = BundleUtils.getBundleInfos(getClass(), bundleBaseName);

            final SalesforceBundleResolver bundleResolver = new SalesforceBundleResolver(bundleInfosFromJar);
            final List<BundleInfo> resultList1 = bundleResolver.getResultList();
            for (BundleInfo bundleInfo : resultList1) {
                BundleUtils.findReferences(bundleInfo, bundleResolver);
            }
        } catch (BundleResolver.BundleResolverException e) {
            throw new SalesforceInstallationException(e);
        } catch (BundleResolver.UnknownBundleException e) {
            throw new SalesforceInstallationException(e);
        } catch (BundleResolver.InvalidBundleException e) {
            throw new SalesforceInstallationException(e);
        }
    }

    @NotNull
    @Override
    public String getVersion() throws SalesforceInstallationException {
        return installerVersion;
    }

    @NotNull
    @Override
    public JobId<PolicyBundleDryRunResult> dryRunInstall(@NotNull final Collection<String> componentId,
                                                         @NotNull final Map<String, BundleMapping> bundleMappings,
                                                         @Nullable final String installationPrefix) {

        final String taskIdentifier = UUID.randomUUID().toString();
        final JobContext jobContext = new JobContext(taskIdentifier);
        taskToJobContext.put(taskIdentifier, jobContext);

        final Future<PolicyBundleDryRunResult> future = executorService.submit(find(false).wrapCallable(new Callable<PolicyBundleDryRunResult>() {
            @Override
            public PolicyBundleDryRunResult call() throws Exception {
                try {
                    return doDryRunInstall(taskIdentifier, componentId, bundleMappings, installationPrefix);
                } catch (SalesforceInstallationException e) {
                    final SalesforceInstallationAuditEvent problemEvent = new SalesforceInstallationAuditEvent(this, "Problem during pre installation check of the Toolkit", Level.WARNING);
                    problemEvent.setAuditDetails(Arrays.asList(
                        new AuditDetail(AssertionMessages.OTK_INSTALLER_ERROR,
                            new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e))));
                    appEventPublisher.publishEvent(problemEvent);
                    throw e;
                } finally {
                    taskToJobContext.remove(taskIdentifier);
                }
            }
        }));

        final JobId<PolicyBundleDryRunResult> jobId = registerJob(future, PolicyBundleDryRunResult.class);
        jobContext.jobId = jobId;

        return jobId;
    }

    /*
    * All bundles in bundleNames MUST USE the same GUIDS for all policies which have the same name. The name of a policy
    * is unique on a Gateway. If the bundles contain the same policy with different guids the bundles will not install.
    *
            * @param componentId     names of all bundles to install. Bundles may depend on each others items, but there is no
    *                           install dependency order.
    * @param folderOid          oid of the folder to install into.
            * @param installationPrefix prefix to version the installation with
    * @return Job ID, which will report on which bundles were installed.
    * @throws IOException for any problem installing. Installation is cancelled on the first error.
    */
    @NotNull
    @Override
    public JobId<ArrayList> install(@NotNull final Collection<String> componentId,
                                    final long folderOid,
                                    @NotNull final Map<String, BundleMapping> bundleMappings,
                                    @Nullable final String installationPrefix) throws SalesforceInstallationException {

        final String taskIdentifier = UUID.randomUUID().toString();
        final JobContext jobContext = new JobContext(taskIdentifier);
        taskToJobContext.put(taskIdentifier, jobContext);

        final Future<ArrayList> future = executorService.submit(find(false).wrapCallable(new Callable<ArrayList>() {
            @Override
            public ArrayList call() throws Exception {
                try {
                    return new ArrayList<String>(doInstall(taskIdentifier, componentId, folderOid, bundleMappings, installationPrefix));
                } catch(SalesforceInstallationException e) {
                    final SalesforceInstallationAuditEvent problemEvent = new SalesforceInstallationAuditEvent(this, "Problem during installation of the Toolkit", Level.WARNING);
                    problemEvent.setAuditDetails(Arrays.asList(
                        new AuditDetail(AssertionMessages.OTK_INSTALLER_ERROR,
                            new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e))));
                    appEventPublisher.publishEvent(problemEvent);
                    throw e;
                } finally {
                    taskToJobContext.remove(taskIdentifier);
                }
            }
        }));

        final JobId<ArrayList> jobId = registerJob(future, ArrayList.class);
        jobContext.jobId = jobId;

        return jobId;
    }

    @NotNull
    @Override
    public List<BundleInfo> getAllComponents() throws SalesforceInstallationException {
        return new SalesforceBundleResolver(bundleInfosFromJar).getResultList();
    }

    public static class PolicyBundleDryRunResultImpl implements PolicyBundleDryRunResult {

        public PolicyBundleDryRunResultImpl(final Map<String, Map<DryRunItem, List<String>>> bundleToConflicts) {
            for (Map.Entry<String, Map<DryRunItem, List<String>>> entry : bundleToConflicts.entrySet()) {
                final Map<DryRunItem, List<String>> itemsForBundle = entry.getValue();
                final Map<DryRunItem, List<String>> copiedItems = new HashMap<DryRunItem, List<String>>();
                for (Map.Entry<DryRunItem, List<String>> bundleItemEntry : itemsForBundle.entrySet()) {
                    copiedItems.put(bundleItemEntry.getKey(), new ArrayList<String>(bundleItemEntry.getValue()));
                }

                conflictsForItemMap.put(entry.getKey(), copiedItems);
            }
        }

        @Override
        public boolean anyConflictsForBundle(String bundleId) throws UnknownBundleIdException {
            if (!conflictsForItemMap.containsKey(bundleId)) {
                throw new UnknownBundleIdException("Unknown bundle id #{" + bundleId + "}");
            }

            final Map<DryRunItem, List<String>> itemMap = conflictsForItemMap.get(bundleId);
            for (Map.Entry<DryRunItem, List<String>> entry : itemMap.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public List<String> getConflictsForItem(String bundleId, DryRunItem dryRunItem) throws UnknownBundleIdException {
            if (!conflictsForItemMap.containsKey(bundleId)) {
                throw new UnknownBundleIdException("Unknown bundle id #{" + bundleId + "}");
            }

            final Map<DryRunItem, List<String>> itemMap = conflictsForItemMap.get(bundleId);
            if (!itemMap.containsKey(dryRunItem)) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(itemMap.get(dryRunItem));
        }

        // - PRIVATE
        final Map<String, Map<DryRunItem, List<String>>> conflictsForItemMap = new HashMap<String, Map<DryRunItem, List<String>>>();
    }

    @Override
    public <OUT extends Serializable> void cancelJob(JobId<OUT> jobId, boolean interruptIfRunning) {

        // find the JobContext and cancel if found - this ensures that the event's cancelled status is always set
        // if the thread is interrupted due to this cancel event.
        for (final Map.Entry<String, JobContext> entry : taskToJobContext.entrySet()) {
            final JobContext jobContext = entry.getValue();
            final JobId currentJobId = jobContext.jobId;
            if (currentJobId != null && jobId.equals(currentJobId)) {
                jobContext.cancelled = true;
                final WSManagementRequestEvent currentEvent = jobContext.currentEvent;
                if (currentEvent != null) {
                    // cancel event so Policy Bundle Installer module can cancel if it's currently processing
                    currentEvent.setCancelled(true);
                }
            }
        }

        // this may cause an Interrupted Exception to be thrown if execution happens to be in this module.
        super.cancelJob(jobId, interruptIfRunning);
    }

    /**
     * Wired via SalesforceAssertion meta data.
     *
     * @param context spring application context
     */
    public static synchronized void onModuleLoaded(final ApplicationContext context) {

        if (spring != null) {
            logger.log(Level.WARNING, "Installer module is already initialized");
        } else {
            spring = context;
        }
    }

    public void setAssertionChecker(AssertionChecker assertionChecker) {
        this.assertionChecker = assertionChecker;
    }

    // - PROTECTED
    protected PolicyBundleDryRunResult doDryRunInstall(@NotNull final String taskIdentifier,
                                                       @NotNull final Collection<String> componentIds,
                                                       @NotNull final Map<String, BundleMapping> bundleMappings,
                                                       @Nullable final String installationPrefix) throws SalesforceInstallationException {
        final SalesforceInstallationAuditEvent startedEvent = new SalesforceInstallationAuditEvent(this, MessageFormat.format(PRE_INSTALLATION_MESSAGE, "started"), Level.INFO);
        appEventPublisher.publishEvent(startedEvent);

        final SalesforceBundleResolver bundleResolver = new SalesforceBundleResolver(bundleInfosFromJar);

        final HashMap<String, Map<PolicyBundleDryRunResult.DryRunItem, List<String>>> bundleToConflicts = new HashMap<String, Map<PolicyBundleDryRunResult.DryRunItem, List<String>>>();
        final Set<String> processedComponents = new HashSet<String>();
        outer:
        for (String bundleId : componentIds) {
            final List<BundleInfo> resultList = bundleResolver.getResultList();
            for (BundleInfo bundleInfo : resultList) {
                if (bundleInfo.getId().equals(bundleId)) {
                    final JobContext jobContext = taskToJobContext.get(taskIdentifier);
                    if (jobContext.cancelled) {
                        break outer;
                    }

                    //todo fix folder id
                    final String prefixToUse = (installationPrefix != null && !installationPrefix.isEmpty()) ? installationPrefix : null;
                    final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                        bundleInfo, bundleMappings.get(bundleId), prefixToUse, bundleResolver);

                    final DryRunInstallPolicyBundleEvent dryRunEvent =
                        new DryRunInstallPolicyBundleEvent(bundleMappings, context);
                    jobContext.currentEvent = dryRunEvent;

                    appEventPublisher.publishEvent(dryRunEvent);
                    if (validateEventProcessed(dryRunEvent)) {
                        // this is logged at fine as it's not as important as only a dry run.
                        break outer;
                    }

                    final List<AuditDetail> details = new ArrayList<AuditDetail>();
                    final List<String> urlPatternWithConflict = dryRunEvent.getUrlPatternWithConflict();
                    if (!urlPatternWithConflict.isEmpty()) {
                        details.add(
                            new AuditDetail(
                                AssertionMessages.OTK_DRY_RUN_CONFLICT,
                                bundleInfo.getName(),
                                "Services",
                                urlPatternWithConflict.toString()));
                    }

                    final List<String> policyWithNameConflict = dryRunEvent.getPolicyWithNameConflict();
                    if (!policyWithNameConflict.isEmpty()) {
                        details.add(
                            new AuditDetail(
                                AssertionMessages.OTK_DRY_RUN_CONFLICT,
                                bundleInfo.getName(),
                                "Policies",
                                policyWithNameConflict.toString()));
                    }

                    // work around for lack of MODULAR_ASSERTION dry run item, which became available after SSG 7.0
                    final List<String> policyConflictAndAssertionMissing = new ArrayList<String>(policyWithNameConflict);
                    if (!assertionChecker.isAssertionAvailable(EXECUTE_SALESFORCE_OPERATION_XML)) {
                        policyConflictAndAssertionMissing.add("Missing custom assertion: Execute Salesforce Operation, verify execute_salesforce_operation.jar exists under /opt/SecureSpan/Gateway/runtime/modules/lib/.");
                    }

                    final List<String> jdbcConnsThatDontExist = dryRunEvent.getJdbcConnsThatDontExist();
                    if (!jdbcConnsThatDontExist.isEmpty()) {
                        details.add(
                            new AuditDetail(
                                AssertionMessages.OTK_DRY_RUN_CONFLICT,
                                bundleInfo.getName(),
                                "Missing JDBC Connections",
                                jdbcConnsThatDontExist.toString()));
                    }

                    final Map<PolicyBundleDryRunResult.DryRunItem, List<String>> itemToConflicts = new HashMap<PolicyBundleDryRunResult.DryRunItem, List<String>>();
                    itemToConflicts.put(PolicyBundleDryRunResult.DryRunItem.SERVICES, urlPatternWithConflict);
                    itemToConflicts.put(PolicyBundleDryRunResult.DryRunItem.POLICIES, policyConflictAndAssertionMissing);
                    itemToConflicts.put(PolicyBundleDryRunResult.DryRunItem.JDBC_CONNECTIONS, jdbcConnsThatDontExist);

                    bundleToConflicts.put(bundleId, itemToConflicts);

                    // any conflicts found?
                    if (!details.isEmpty()) {
                        final SalesforceInstallationAuditEvent problemEvent =
                            new SalesforceInstallationAuditEvent(this,
                                MessageFormat.format("OAuth Toolkit pre installation check conflicts for component {0} found.", bundleInfo.getName()),
                                Level.INFO);
                        problemEvent.setAuditDetails(details);
                        appEventPublisher.publishEvent(problemEvent);
                    }
                    processedComponents.add(bundleId);
                }
            }
        }

        if (processedComponents.containsAll(componentIds)) {
            final SalesforceInstallationAuditEvent stoppedEvent =
                new SalesforceInstallationAuditEvent(this, MessageFormat.format(PRE_INSTALLATION_MESSAGE, "completed"), Level.INFO);
            appEventPublisher.publishEvent(stoppedEvent);
        } else {
            final SalesforceInstallationAuditEvent cancelledEvent =
                new SalesforceInstallationAuditEvent(this, MessageFormat.format(PRE_INSTALLATION_MESSAGE, "cancelled"), Level.INFO);
            appEventPublisher.publishEvent(cancelledEvent);
        }

        return new PolicyBundleDryRunResultImpl(bundleToConflicts);
    }


    /**
     * Perform the work of installing the OTK.
     *
     * @param taskIdentifier used to look up the context to see if task has been cancelled.
     * @param componentIds component to install
     * @param folderOid folder to install component into
     * @param bundleMappings any mappings.
     * @param installationPrefix installation prefix
     * @return Ids of installed bundles
     * @throws SalesforceInstallationException
     *
     */
    protected List<String> doInstall(@NotNull final String taskIdentifier,
                                     @NotNull final Collection<String> componentIds,
                                     final long folderOid,
                                     @NotNull Map<String, BundleMapping> bundleMappings,
                                     @Nullable final String installationPrefix) throws SalesforceInstallationException {

        // When installing more than one bundle, allow for optimization of not trying to recreate items already created.
        final Map<String, Object> contextMap = new HashMap<String, Object>();

        final List<String> installedBundles = new ArrayList<String>();
        final SalesforceBundleResolver bundleResolver = new SalesforceBundleResolver(bundleInfosFromJar);
        if (isInstallInProgress.compareAndSet(false, true)) {

            final String prefixToUse = (installationPrefix != null && !installationPrefix.trim().isEmpty()) ?
                installationPrefix : null;

            final String prefixMsg = (prefixToUse == null) ? "" : "with installation prefix '" + prefixToUse + "'";
            final SalesforceInstallationAuditEvent startedEvent = new SalesforceInstallationAuditEvent(this, MessageFormat.format(INSTALLATION_MESSAGE, "started", prefixMsg), Level.INFO);
            appEventPublisher.publishEvent(startedEvent);

            try {
                bundleResolver.setInstallationPrefix(prefixToUse);

                final Set<String> processedComponents = new HashSet<String>();
                //iterate through all the bundle names to install
                outer:
                for (String bundleId : componentIds) {
                    final JobContext jobContext = taskToJobContext.get(taskIdentifier);
                    if (jobContext.cancelled) {
                        break;
                    }

                    final List<BundleInfo> resultList1 = bundleResolver.getResultList();
                    for (BundleInfo bundleInfo : resultList1) {
                        if (bundleInfo.getId().equals(bundleId)) {

                            final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                                bundleInfo, folderOid, bundleMappings.get(bundleId), prefixToUse, bundleResolver);
                            final InstallPolicyBundleEvent installEvent =
                                new InstallPolicyBundleEvent(this, context, null);
                            jobContext.currentEvent = installEvent;

                            appEventPublisher.publishEvent(installEvent);
                            if (validateEventProcessed(installEvent)) {
                                break outer;
                            }
                            processedComponents.add(bundleId);
                        }
                    }
                    installedBundles.add(bundleId);
                }
                if (processedComponents.containsAll(componentIds)) {
                    final SalesforceInstallationAuditEvent completedEvent =
                        new SalesforceInstallationAuditEvent(this, MessageFormat.format(INSTALLATION_MESSAGE, "completed", prefixMsg), Level.INFO);
                    appEventPublisher.publishEvent(completedEvent);
                } else {
                    final SalesforceInstallationAuditEvent cancelledEvent =
                        new SalesforceInstallationAuditEvent(this, MessageFormat.format(INSTALLATION_MESSAGE, "cancelled", prefixMsg), Level.INFO);
                    appEventPublisher.publishEvent(cancelledEvent);
                }

            } finally {
                isInstallInProgress.set(false);
            }
        } else {
            throw new SalesforceInstallationException("Install is already in progress");
        }

        return installedBundles;

    }

    // - PACKAGE

    static class SalesforceInstallationAuditEvent extends DetailedAdminEvent {
        public SalesforceInstallationAuditEvent(final Object source, final String note, final Level level) {
            super(source, note, level);
        }
    }

    /**
     * Allow mock unit test to stub Custom Assertion check.
     * Custom Assertion class not found because they reside in a different Subversion project.
     */
    class AssertionChecker {
        public boolean isAssertionAvailable(final String assertionAsXml) {
            if (spring == null) {
                throw new IllegalStateException("Installer is not configured. ApplicationContext is missing");
            }

            final WspReader wspReader = spring.getBean("wspReader", WspReader.class);
            try {
                Assertion assertions = wspReader.parseStrictly(assertionAsXml, WspReader.Visibility.omitDisabled);

                // scan for UnknownAssertion
                Iterator it = assertions.preorderIterator();
                while (it.hasNext()) {
                    final Object assertion = it.next();
                    if (assertion instanceof UnknownAssertion)
                        return false;
                }
            } catch (IOException e) {
                // assertion is not installed
                return false;
            }
            return true;
        }
    }

    // - PRIVATE

    private final AtomicBoolean isInstallInProgress = new AtomicBoolean(false);
    private static final Logger logger = Logger.getLogger(SalesforceInstallerAdminImpl.class.getName());
    private final String installerVersion;
    private final List<Pair<BundleInfo, String>> bundleInfosFromJar;
    private final ApplicationEventPublisher appEventPublisher;
    private final ExecutorService executorService;
    private final Map<String, JobContext> taskToJobContext = new ConcurrentHashMap<String, JobContext>();
    private static final String INSTALLATION_MESSAGE = "Installation of the Toolkit {0} {1}";
    private static final String PRE_INSTALLATION_MESSAGE = "Pre installation check of the Toolkit {0}";

    /**
     * Needed to get wspReader, cannot inject wspReader as it is in creation when assertions are loaded causing a
     * circular dependency.
     */
    private static ApplicationContext spring;

    /**
     * Associates a JobId, WSManagementRequestEvent and Cancelled status with a task identifier.
     */
    private static final class JobContext {

        private JobContext(String taskIdentifier) {
            this.taskIdentifier = taskIdentifier;
        }

        /**
         * Identity is based solely on the task identifier.
         * @param o object
         * @return boolean
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JobContext that = (JobContext) o;

            return taskIdentifier.equals(that.taskIdentifier);

        }

        @Override
        public int hashCode() {
            return taskIdentifier.hashCode();
        }

        // - PRIVATE

        private final String taskIdentifier;
        private boolean cancelled;

        /**
         * It is not safe to specify the JobId in construction as the future may have started executing before the
         * JobContext has been created.
         * This is because a future is required in order to register a job and get a JobId.
         * To protect against this client code needs to be prepared for jobId to be null.
         */
        @Nullable
        private JobId jobId;
        @Nullable
        private WSManagementRequestEvent currentEvent;
    }

    /**
     * Check the status of the bundle event. Throws when a client should know about an unexpected situation.
     *
     * @param bundleEvent event to validate
     * @return true if the thread processing the event was interrupted.
     * @throws SalesforceInstallationException
     *
     */
    private boolean validateEventProcessed(PolicyBundleEvent bundleEvent) throws SalesforceInstallationException {
        if (!bundleEvent.isProcessed()) {
            throw new SalesforceInstallationException("Policy Bundle Installer module is not installed.");
        }

        final Exception processingException = bundleEvent.getProcessingException();
        if (processingException != null) {
            if (!(processingException instanceof BundleResolver.UnknownBundleException)) {
                if (processingException instanceof InterruptedException) {
                    return true;
                }

                logger.warning("Exception type: " + processingException.getClass().getName());
                logger.warning("Unexpected error during installation: " + ExceptionUtils.getMessage(processingException));
                throw new SalesforceInstallationException(processingException);
            } else {
                throw new SalesforceInstallationException(processingException);
            }
        }
        return Thread.interrupted() || bundleEvent.isCancelled();

    }
}