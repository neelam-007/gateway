package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.PolicyBundleEvent;
import com.l7tech.server.event.wsman.WSManagementRequestEvent;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import static com.l7tech.server.event.AdminInfo.find;
import static com.l7tech.policy.bundle.PolicyBundleDryRunResult.DryRunItem;

public class OAuthInstallerAdminImpl extends AsyncAdminMethodsImpl implements OAuthInstallerAdmin {

    public static final String NS_INSTALLER_VERSION = "http://ns.l7tech.com/2012/11/oauth-toolkit-bundle";

    public OAuthInstallerAdminImpl(final String bundleBaseName, ApplicationEventPublisher spring) throws OAuthToolkitInstallationException {
        this.spring = spring;
        this.executorService = Executors.newCachedThreadPool();

        final String oauthBundleInfo = bundleBaseName + "OAuthToolkitBundleInfo.xml";
        final URL oauthBundleInfoUrl = getClass().getResource(oauthBundleInfo);
        if (oauthBundleInfoUrl == null) {
            throw new OAuthToolkitInstallationException("Could not find OAuthToolkitBundleInfo.xml");
        }

        final byte[] bundleBytes;
        try {
            bundleBytes = IOUtils.slurpUrl(oauthBundleInfoUrl);
        } catch (IOException e) {
            throw new OAuthToolkitInstallationException(e);
        }

        final Document oauthInfoDoc;
        try {
            oauthInfoDoc = XmlUtil.parse(new ByteArrayInputStream(bundleBytes));
        } catch (IOException e) {
            throw new OAuthToolkitInstallationException("Unable to parse resource: " + oauthBundleInfo, e);
        } catch (SAXException e) {
            throw new OAuthToolkitInstallationException("Unable to parse resource: " + oauthBundleInfo, e);
        }

        try {
            final Element versionElm = XmlUtil.findExactlyOneChildElementByName(oauthInfoDoc.getDocumentElement(), NS_INSTALLER_VERSION, "Version");
            oAuthInstallerVersion = DomUtils.getTextValue(versionElm, true);
            if (oAuthInstallerVersion.isEmpty()) {
                throw new OAuthToolkitInstallationException("Could not get version information for OAuth Toolkit");
            }
        } catch (Exception e) {
            throw new OAuthToolkitInstallationException("Could not find version information in: " + oauthBundleInfo, e);
        }

        try {
            bundleInfosFromJar = BundleUtils.getBundleInfos(getClass(), bundleBaseName);

            final OAuthToolkitBundleResolver bundleResolver = new OAuthToolkitBundleResolver(bundleInfosFromJar);
            final List<BundleInfo> resultList1 = bundleResolver.getResultList();
            for (BundleInfo bundleInfo : resultList1) {
                BundleUtils.findReferences(bundleInfo, bundleResolver);
            }
        } catch (BundleResolver.BundleResolverException e) {
            throw new OAuthToolkitInstallationException(e);
        } catch (BundleResolver.UnknownBundleException e) {
            throw new OAuthToolkitInstallationException(e);
        } catch (BundleResolver.InvalidBundleException e) {
            throw new OAuthToolkitInstallationException(e);
        }
    }

    @NotNull
    @Override
    public String getOAuthToolkitVersion() throws OAuthToolkitInstallationException {
        return oAuthInstallerVersion;
    }

    @NotNull
    @Override
    public JobId<PolicyBundleDryRunResult> dryRunOtkInstall(@NotNull final Collection<String> otkComponentId,
                                                            @NotNull final Map<String, BundleMapping> bundleMappings,
                                                            @Nullable final String installationPrefix) {

        final String taskIdentifier = UUID.randomUUID().toString();
        final JobContext jobContext = new JobContext(taskIdentifier);
        taskToJobContext.put(taskIdentifier, jobContext);

        final Future<PolicyBundleDryRunResult> future = executorService.submit(find(false).wrapCallable(new Callable<PolicyBundleDryRunResult>() {
            @Override
            public PolicyBundleDryRunResult call() throws Exception {
                try {
                    return doDryRunOtkInstall(taskIdentifier, otkComponentId, bundleMappings, installationPrefix);
                } finally {
                    taskToJobContext.remove(taskIdentifier);
                }
            }
        }));

        final JobId<PolicyBundleDryRunResult> jobId = registerJob(future, PolicyBundleDryRunResult.class);
        jobContext.jobId = jobId;

        return jobId;
    }

    /**
     * All bundles in bundleNames MUST USE the same GUIDS for all policies which have the same name. The name of a policy
     * is unique on a Gateway. If the bundles contain the same policy with different guids the bundles will not install.
     *
     * @param otkComponentId     names of all bundles to install. Bundles may depend on each others items, but there is no
     *                           install dependency order.
     * @param folderOid          oid of the folder to install into.
     * @param installationPrefix prefix to version the installation with
     * @return Job ID, which will report on which bundles were installed.
     * @throws IOException for any problem installing. Installation is cancelled on the first error.
     */
    @NotNull
    @Override
    public JobId<ArrayList> installOAuthToolkit(@NotNull final Collection<String> otkComponentId,
                                                final long folderOid,
                                                @NotNull final Map<String, BundleMapping> bundleMappings,
                                                @Nullable final String installationPrefix) throws OAuthToolkitInstallationException {

        final String taskIdentifier = UUID.randomUUID().toString();
        final JobContext jobContext = new JobContext(taskIdentifier);
        taskToJobContext.put(taskIdentifier, jobContext);

        final Future<ArrayList> future = executorService.submit(find(false).wrapCallable(new Callable<ArrayList>() {
            @Override
            public ArrayList call() throws Exception {
                try {
                    return new ArrayList<String>(doInstallOAuthToolkit(taskIdentifier, otkComponentId, folderOid, bundleMappings, installationPrefix));
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
    public List<BundleInfo> getAllOtkComponents() throws OAuthToolkitInstallationException {
        return new OAuthToolkitBundleResolver(bundleInfosFromJar).getResultList();
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

    // - PROTECTED

    protected PolicyBundleDryRunResult doDryRunOtkInstall(@NotNull final String taskIdentifier,
                                                          @NotNull final Collection<String> otkComponentId,
                                                          @NotNull final Map<String, BundleMapping> bundleMappings,
                                                          @Nullable final String installationPrefix) throws OAuthToolkitInstallationException {
        final OAuthToolkitBundleResolver bundleResolver = new OAuthToolkitBundleResolver(bundleInfosFromJar);

        final HashMap<String, Map<DryRunItem, List<String>>> bundleToConflicts = new HashMap<String, Map<DryRunItem, List<String>>>();
        outer:
        for (String bundleId : otkComponentId) {
            final List<BundleInfo> resultList = bundleResolver.getResultList();
            for (BundleInfo bundleInfo : resultList) {
                if (bundleInfo.getId().equals(bundleId)) {
                    final JobContext jobContext = taskToJobContext.get(taskIdentifier);
                    if (jobContext.cancelled) {
                        logger.info("Pre installation check was cancelled.");
                        break outer;
                    }

                    //todo fix folder id
                    final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                            bundleInfo, -5002, new HashMap<String, Object>(), bundleMappings.get(bundleId), installationPrefix);

                    final DryRunInstallPolicyBundleEvent dryRunEvent =
                            new DryRunInstallPolicyBundleEvent(bundleMappings, bundleResolver, context);
                    jobContext.currentEvent = dryRunEvent;

                    spring.publishEvent(dryRunEvent);
                    if (validateEventProcessed(dryRunEvent)) {
                        // this is logged at fine as it's not as important as only a dry run.
                        logger.fine("Pre installation check was cancelled.");
                        break outer;
                    }
                    final List<String> urlPatternWithConflict = dryRunEvent.getUrlPatternWithConflict();
                    final List<String> jdbcConnsThatDontExist = dryRunEvent.getJdbcConnsThatDontExist();
                    final List<String> policyWithNameConflict = dryRunEvent.getPolicyWithNameConflict();
                    final Map<DryRunItem, List<String>> itemToConflicts = new HashMap<DryRunItem, List<String>>();
                    itemToConflicts.put(DryRunItem.SERVICES, urlPatternWithConflict);
                    itemToConflicts.put(DryRunItem.POLICIES, policyWithNameConflict);
                    itemToConflicts.put(DryRunItem.JDBC_CONNECTIONS, jdbcConnsThatDontExist);

                    bundleToConflicts.put(bundleId, itemToConflicts);
                }
            }
        }

        return new PolicyBundleDryRunResultImpl(bundleToConflicts);
    }

    /**
     * Perform the work of installing the OTK.
     *
     * @param taskIdentifier used to look up the context to see if task has been cancelled.
     * @param otkComponentId component to install
     * @param folderOid folder to install component into
     * @param bundleMappings any mappings.
     * @param installationPrefix installation prefix
     * @return Ids of installed bundles
     * @throws OAuthToolkitInstallationException
     *
     */
    protected List<String> doInstallOAuthToolkit(@NotNull final String taskIdentifier,
                                                 @NotNull final Collection<String> otkComponentId,
                                                 final long folderOid,
                                                 @NotNull Map<String, BundleMapping> bundleMappings,
                                                 @Nullable final String installationPrefix) throws OAuthToolkitInstallationException {

        //todo check version of bundle to ensure it's supported.

        // When installing more than one bundle, allow for optimization of not trying to recreate items already created.
        final Map<String, Object> contextMap = new HashMap<String, Object>();

        final List<String> installedBundles = new ArrayList<String>();
        final OAuthToolkitBundleResolver bundleResolver = new OAuthToolkitBundleResolver(bundleInfosFromJar);
        if (isInstallInProgress.compareAndSet(false, true)) {
            try {
                if (installationPrefix != null) {
                    bundleResolver.setInstallationPrefix(installationPrefix);
                }

                //iterate through all the bundle names to install
                outer:
                for (String bundleId : otkComponentId) {
                    final JobContext jobContext = taskToJobContext.get(taskIdentifier);
                    if (jobContext.cancelled) {
                        logger.info("Installation of the OAuth toolkit was cancelled.");
                        break;
                    }

                    final List<BundleInfo> resultList1 = bundleResolver.getResultList();
                    for (BundleInfo bundleInfo : resultList1) {
                        if (bundleInfo.getId().equals(bundleId)) {

                            final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                                    bundleInfo, folderOid, contextMap, bundleMappings.get(bundleId), installationPrefix);
                            final InstallPolicyBundleEvent installEvent =
                                    new InstallPolicyBundleEvent(this, bundleResolver,
                                            context,
                                            getSavePolicyCallback(installationPrefix));
                            jobContext.currentEvent = installEvent;

                            spring.publishEvent(installEvent);
                            if (validateEventProcessed(installEvent)) {
                                logger.info("Installation of the OAuth toolkit was cancelled.");
                                break outer;
                            }
                        }
                    }
                    installedBundles.add(bundleId);
                }

            } finally {
                isInstallInProgress.set(false);
            }
        } else {
            throw new OAuthToolkitInstallationException("Install is already in progress");
        }

        return installedBundles;

    }

    /**
     * If the valueWithPossibleHost starts with a context variable that begins with 'host_', then update the value
     * of the string to have the installationPrefix inserted after the varaible reference.
     *
     * @param installationPrefix    prefix to insert
     * @param valueWithPossibleHost value which may need to be udpated in a prefixed installation.
     * @return Updated value. Null if no new value is needed.
     */
    @Nullable
    protected static String getUpdatedHostValue(@NotNull final String installationPrefix,
                                                @NotNull final String valueWithPossibleHost) {

        final List<String> vars = Arrays.asList(Syntax.getReferencedNames(valueWithPossibleHost));
        if (vars.size() == 1 && Syntax.getVariableExpression(vars.get(0)).equals(valueWithPossibleHost)) {
            return null;
        }

        Matcher matcher = Syntax.regexPattern.matcher(valueWithPossibleHost);
        List<Object> result = new ArrayList<Object>();

        int previousMatchEndIndex = 0;
        boolean hostVarFound = false;
        while (matcher.find()) {
            int matchingCount = matcher.groupCount();
            if (matchingCount != 1) {
                throw new IllegalStateException("Expecting 1 matching group, received: " + matchingCount);
            }
            final String preceedingText = valueWithPossibleHost.substring(previousMatchEndIndex, matcher.start());
            //note if there is actually an empty space, we will preserve it, so no .trim() before .isEmpty()
            if (!preceedingText.isEmpty()) {
                result.add(valueWithPossibleHost.substring(previousMatchEndIndex, matcher.start()));
            }

            final String group = matcher.group(1);
            if (group.startsWith("host_")) {
                result.add(Syntax.getVariableExpression(group) + "/" + installationPrefix);
                hostVarFound = true;
            } else {
                result.add(Syntax.getVariableExpression(group));
            }

            previousMatchEndIndex = matcher.end();
        }
        if (previousMatchEndIndex < valueWithPossibleHost.length())
            result.add(valueWithPossibleHost.substring(previousMatchEndIndex, valueWithPossibleHost.length()));

        if (!hostVarFound) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            for (Object o : result) {
                sb.append(o);
            }
            return sb.toString();
        }
    }

    // - PRIVATE

    private final AtomicBoolean isInstallInProgress = new AtomicBoolean(false);
    private static final Logger logger = Logger.getLogger(OAuthInstallerAdminImpl.class.getName());
    private final String oAuthInstallerVersion;
    private final List<Pair<BundleInfo, String>> bundleInfosFromJar;
    private final ApplicationEventPublisher spring;
    private final ExecutorService executorService;
    private final Map<String, JobContext> taskToJobContext = new ConcurrentHashMap<String, JobContext>();

    /**
     * Associates a JobId, WSManagementRequestEvent and Cancelled status with a task identifier.
     */
    private static final class JobContext {

        private JobContext(String taskIdentifier) {
            this.taskIdentifier = taskIdentifier;
        }

        /**
         * Identity is based solely on the task identifier.
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JobContext that = (JobContext) o;

            if (!taskIdentifier.equals(that.taskIdentifier)) return false;

            return true;
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
     * @throws OAuthToolkitInstallationException
     *
     */
    private boolean validateEventProcessed(PolicyBundleEvent bundleEvent) throws OAuthToolkitInstallationException {
        if (!bundleEvent.isProcessed()) {
            throw new OAuthToolkitInstallationException("Policy Bundle Installer module is not installed.");
        }

        final Exception processingException = bundleEvent.getProcessingException();
        if (processingException != null) {
            if (!(processingException instanceof BundleResolver.UnknownBundleException)) {
                if (processingException instanceof InterruptedException) {
                    return true;
                }

                logger.warning("Exception type: " + processingException.getClass().getName());
                logger.warning("Unexpected error during installation: " + ExceptionUtils.getMessage(processingException));
                throw new OAuthToolkitInstallationException(processingException);
            } else {
                throw new OAuthToolkitInstallationException(processingException);
            }
        }

        return bundleEvent.isCancelled();

    }

    @NotNull
    private PreBundleSavePolicyCallback getSavePolicyCallback(final String installationPrefix) {
        return new PreBundleSavePolicyCallback() {
            @Override
            public void prePublishCallback(BundleInfo bundleInfo, String resourceType, Document writeablePolicyDoc) throws PolicyUpdateException {
                if (installationPrefix != null) {
                    // if we have prefixed the installation, then we need to be able to update routing assertions to route to the
                    // prefixed URIs

                    // 1 - find routing URIs
                    final List<Element> protectedUrls = PolicyUtils.findProtectedUrls(writeablePolicyDoc.getDocumentElement());
                    for (Element protectedUrl : protectedUrls) {
                        final String routingUrlValue = protectedUrl.getAttribute("stringValue");
                        final String updatedHostValue = getUpdatedHostValue(installationPrefix, routingUrlValue);
                        if (updatedHostValue != null) {
                            protectedUrl.setAttribute("stringValue", updatedHostValue);
                            logger.fine("Updated routing URL from '" + routingUrlValue + "' to '" + updatedHostValue + "'");
                        }
                    }

                    // 2 - find context variables
                    final List<Element> contextVariables = PolicyUtils.findContextVariables(writeablePolicyDoc.getDocumentElement());
                    for (Element contextVariable : contextVariables) {
                        final Element variableToSetElm;
                        final Element base64ExpressionElm;
                        try {
                            base64ExpressionElm = XmlUtil.findExactlyOneChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "Base64Expression");
                            variableToSetElm = XmlUtil.findExactlyOneChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "VariableToSet");
                        } catch (TooManyChildElementsException e) {
                            throw new PolicyUpdateException("Problem finding variable value: " + ExceptionUtils.getMessage(e));
                        } catch (MissingRequiredElementException e) {
                            throw new PolicyUpdateException("Problem finding variable value: " + ExceptionUtils.getMessage(e));
                        }
                        final String variableName = variableToSetElm.getAttribute("stringValue");
                        if (!variableName.startsWith("host_")) {
                            final String base64Value = base64ExpressionElm.getAttribute("stringValue");
                            final String decodedValue = new String(HexUtils.decodeBase64(base64Value, true), Charsets.UTF8);
                            final String updatedHostValue = getUpdatedHostValue(installationPrefix, decodedValue);
                            if (updatedHostValue != null) {
                                base64ExpressionElm.setAttribute("stringValue", HexUtils.encodeBase64(HexUtils.encodeUtf8(updatedHostValue), true));
                                logger.fine("Updated context variable value from from '" + decodedValue + "' to '" + updatedHostValue + "'");
                            }
                        }
                    }
                }
            }
        };
    }
}
