package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.admin.DetailedAdminEvent;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.PolicyBundleEvent;
import com.l7tech.server.event.wsman.WSManagementRequestEvent;
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
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.policy.bundle.BundleResolver.*;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse;

/**
 * Abstract Policy Bundle Installer Admin implementation, which must be extended and configured in a specific modular assertion module.
 */
public abstract class PolicyBundleInstallerAdminAbstractImpl extends AsyncAdminMethodsImpl implements PolicyBundleInstallerAdmin {

    private static final Logger logger = Logger.getLogger(PolicyBundleInstallerAdminAbstractImpl.class.getName());
    private static final String INSTALLATION_MESSAGE = "Installation of the {0} {1} {2}";
    private static final String PRE_INSTALLATION_MESSAGE = "Pre installation check of the {0} {1}";

    private final AtomicBoolean isInstallInProgress = new AtomicBoolean(false);
    private final String installerVersion;
    private final List<Pair<BundleInfo, String>> bundleInfosFromJar;
    private final ApplicationEventPublisher appEventPublisher;
    private final Map<String, JobContext> taskToJobContext = new ConcurrentHashMap<>();
    private final BundleResolver bundleResolver;

    protected final ExecutorService executorService;   // TODO make private once create database moved? (OAuthInstallerImpl to here)
    protected boolean checkingAssertionExistenceRequired = true;

    public static synchronized void onModuleLoaded(ApplicationContext context) {
        // no initialization needed on load, empty method needed to pass check in ServerAssertionRegistry
        // "...Modular assertion <name of an assertion that extends PolicyBundleInstallerAdminAbstractImpl> declares a module load listener but the class doesn't have a public static method onModuleLoaded(ApplicationContext)..."
    }

    /**
     * Associates a JobId, WSManagementRequestEvent and Cancelled status with a task identifier.
     */
    private static final class JobContext {
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
    }

    public PolicyBundleInstallerAdminAbstractImpl(@NotNull final String bundleBaseName,
                                                  @NotNull final String bundleInfoFileName,
                                                  @NotNull final String namespaceInstallerVersion,
                                                  @NotNull final Class callingClass,
                                                  @NotNull final JarFile bundleJarFile,
                                                  @NotNull final ApplicationEventPublisher appEventPublisher) throws PolicyBundleInstallerException {
        this.appEventPublisher = appEventPublisher;
        executorService = Executors.newCachedThreadPool();
        installerVersion = getInstallerVersion(bundleBaseName, bundleInfoFileName, namespaceInstallerVersion, callingClass);
        try {
            bundleInfosFromJar = BundleUtils.getBundleInfos(callingClass, bundleBaseName, bundleJarFile);
            bundleResolver = newBundleResolver(bundleInfosFromJar, callingClass);
        } catch (BundleResolverException | UnknownBundleException | InvalidBundleException e) {
            throw new PolicyBundleInstallerException(e);
        }
    }

    public PolicyBundleInstallerAdminAbstractImpl(@NotNull final String bundleBaseName,
                                                  @NotNull final String bundleInfoFileName,
                                                  @NotNull final String namespaceInstallerVersion,
                                                  @NotNull final ApplicationEventPublisher appEventPublisher) throws PolicyBundleInstallerException {
        this.appEventPublisher = appEventPublisher;
        executorService = Executors.newCachedThreadPool();
        Class callingClass = getClass();
        installerVersion = getInstallerVersion(bundleBaseName, bundleInfoFileName, namespaceInstallerVersion, callingClass);
        try{
            bundleInfosFromJar = BundleUtils.getBundleInfos(callingClass, bundleBaseName);
            bundleResolver = newBundleResolver(bundleInfosFromJar, callingClass);
        } catch (BundleResolverException | UnknownBundleException | InvalidBundleException e) {
            throw new PolicyBundleInstallerException(e);
        }
    }

    private BundleResolver newBundleResolver(@NotNull final List<Pair<BundleInfo, String>> bundleInfosFromJar, @NotNull final Class callingClass) throws BundleResolverException, UnknownBundleException, InvalidBundleException {
        BundleResolver bundleResolver = new BundleResolverImpl(bundleInfosFromJar, callingClass);
        final List<BundleInfo> bundleInfoResultList = bundleResolver.getResultList();
        for (BundleInfo bundleInfoResult : bundleInfoResultList) {
            BundleUtils.findReferences(bundleInfoResult, bundleResolver);
        }
        return bundleResolver;
    }

    private String getInstallerVersion(@NotNull final String bundleBaseName,
                                       @NotNull final String bundleInfoFileName,
                                       @NotNull final String namespaceInstallerVersion,
                                       @NotNull final Class callingClass) throws PolicyBundleInstallerException {
        String installerVersion;

        final String bundleInfo = bundleBaseName + bundleInfoFileName;
        final URL bundleInfoUrl = callingClass.getResource(bundleInfo);
        if (bundleInfoUrl == null) {
            throw new PolicyBundleInstallerException("Could not find " + bundleInfo + " for calling class " + callingClass.getName());
        }

        final byte[] bundleBytes;
        try {
            bundleBytes = IOUtils.slurpUrl(bundleInfoUrl);
        } catch (IOException e) {
            throw new PolicyBundleInstallerException(e);
        }
        final Document infoDocument;
        try {
            infoDocument = XmlUtil.parse(new ByteArrayInputStream(bundleBytes));
        } catch (IOException | SAXException e) {
            throw new PolicyBundleInstallerException("Unable to parse resource: " + bundleInfo, e);
        }
        try {
            final Element versionElm = XmlUtil.findExactlyOneChildElementByName(infoDocument.getDocumentElement(), namespaceInstallerVersion, "Version");
            installerVersion = DomUtils.getTextValue(versionElm, true);
            if (installerVersion.isEmpty()) {
                throw new PolicyBundleInstallerException("Could not get version information for " + getInstallerName());
            }
        } catch (Exception e) {
            throw new PolicyBundleInstallerException("Could not find version information in: " + bundleInfo, e);
        }

        return installerVersion;
    }

    @NotNull
    public String getVersion()  {
        return installerVersion;
    }

    @NotNull
    public List<BundleInfo> getAllComponents() throws PolicyBundleInstallerException {
        return bundleResolver.getResultList();
    }

    @NotNull
    public JobId<PolicyBundleDryRunResult> dryRunInstall(@NotNull final Collection<String> componentIds,
                                                         @NotNull final Map<String, BundleMapping> bundleMappings,
                                                         @Nullable final String installationPrefix) {
        final String taskIdentifier = UUID.randomUUID().toString();
        final JobContext jobContext = new JobContext(taskIdentifier);
        taskToJobContext.put(taskIdentifier, jobContext);

        final Future<PolicyBundleDryRunResult> future = executorService.submit(AdminInfo.find(false).wrapCallable(new Callable<PolicyBundleDryRunResult>() {
            @Override
            public PolicyBundleDryRunResult call() throws Exception {
                try {
                    return doDryRunInstall(taskIdentifier, componentIds, bundleMappings, installationPrefix);
                } catch (PolicyBundleInstallerException e) {
                    final DetailedAdminEvent problemEvent = new DetailedAdminEvent(this, "Problem during pre installation check of the " + getInstallerName(), Level.WARNING);
                    problemEvent.setAuditDetails(Arrays.asList(newAuditDetailInstallError(e)));
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

    @NotNull
    public JobId<ArrayList> install(@NotNull final Collection<String> componentIds,
                                    @NotNull final Goid folderGoid,
                                    @NotNull final Map<String, BundleMapping> bundleMappings,
                                    @Nullable final String installationPrefix) throws PolicyBundleInstallerException {

        final String taskIdentifier = UUID.randomUUID().toString();
        final JobContext jobContext = new JobContext(taskIdentifier);
        taskToJobContext.put(taskIdentifier, jobContext);

        final Future<ArrayList> future = executorService.submit(AdminInfo.find(false).wrapCallable(new Callable<ArrayList>() {
            @Override
            public ArrayList call() throws Exception {
                try {
                    return new ArrayList<>(doInstall(taskIdentifier, componentIds, folderGoid, bundleMappings, installationPrefix));
                } catch(PolicyBundleInstallerException e) {
                    final DetailedAdminEvent problemEvent = new DetailedAdminEvent(this, "Problem during installation of the " + getInstallerName(), Level.WARNING);
                    problemEvent.setAuditDetails(Arrays.asList(newAuditDetailInstallError(e)));
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

    protected PolicyBundleDryRunResult doDryRunInstall(@NotNull final String taskIdentifier,
                                                       @NotNull final Collection<String> componentIds,
                                                       @NotNull final Map<String, BundleMapping> bundleMappings,
                                                       @Nullable final String installationPrefix) throws PolicyBundleInstallerException {
        final DetailedAdminEvent startedEvent = new DetailedAdminEvent(this, MessageFormat.format(PRE_INSTALLATION_MESSAGE, getInstallerName(), "started"), Level.INFO);
        appEventPublisher.publishEvent(startedEvent);

        final HashMap<String, Map<PolicyBundleDryRunResult.DryRunItem, List<String>>> bundleToConflicts = new HashMap<>();
        final Set<String> processedComponents = new HashSet<>();
        outer:
        for (String bundleId : componentIds) {
            final List<BundleInfo> resultList = bundleResolver.getResultList();
            for (BundleInfo bundleInfo : resultList) {
                if (bundleInfo.getId().equals(bundleId)) {
                    final JobContext jobContext = taskToJobContext.get(taskIdentifier);
                    if (jobContext.cancelled) {
                        break outer;
                    }

                    final String prefixToUse = (installationPrefix != null && !installationPrefix.isEmpty()) ? installationPrefix : null;
                    final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                            bundleInfo, bundleMappings.get(bundleId), prefixToUse, bundleResolver, checkingAssertionExistenceRequired);

                    final DryRunInstallPolicyBundleEvent dryRunEvent =
                            new DryRunInstallPolicyBundleEvent(bundleMappings, context);
                    jobContext.currentEvent = dryRunEvent;

                    appEventPublisher.publishEvent(dryRunEvent);
                    if (validateEventProcessed(dryRunEvent)) {
                        // this is logged at fine as it's not as important as only a dry run.
                        break outer;
                    }

                    final List<AuditDetail> details = new ArrayList<>();
                    final List<String> urlPatternWithConflict = dryRunEvent.getServiceConflict();
                    if (!urlPatternWithConflict.isEmpty()) {
                        details.add(
                                new AuditDetail(
                                        getAssertionMessageDryRunConflict(),
                                        bundleInfo.getName(),
                                        "Services",
                                        urlPatternWithConflict.toString()));
                    }

                    final List<String> policyWithNameConflict = dryRunEvent.getPolicyConflict();
                    if (!policyWithNameConflict.isEmpty()) {
                        details.add(
                                new AuditDetail(
                                        getAssertionMessageDryRunConflict(),
                                        bundleInfo.getName(),
                                        "Policies",
                                        policyWithNameConflict.toString()));
                    }

                    final List<String> certificateWithConflict = dryRunEvent.getCertificateConflict();
                    if (!certificateWithConflict.isEmpty()) {
                        details.add(
                                new AuditDetail(
                                        getAssertionMessageDryRunConflict(),
                                        bundleInfo.getName(),
                                        "Certificates",
                                        certificateWithConflict.toString()));
                    }

                    final List<String> encapsulatedAssertionWithConflict = dryRunEvent.getEncapsulatedAssertionConflict();
                    if (! encapsulatedAssertionWithConflict.isEmpty()) {
                        details.add(
                                new AuditDetail(
                                        AssertionMessages.POLICY_BUNDLE_INSTALLER_DRY_RUN_CONFLICT,
                                        bundleInfo.getName(),
                                        "Assertions",
                                        encapsulatedAssertionWithConflict.toString()));
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

                    final List<String> missingRequiredAssertions = dryRunEvent.getMissingAssertions();
                    if (! missingRequiredAssertions.isEmpty()) {
                        details.add(
                                new AuditDetail(
                                        getAssertionMessageDryRunConflict(),
                                        bundleInfo.getName(),
                                        "Missing Assertions",
                                        missingRequiredAssertions.toString()));
                    }

                    final Map<PolicyBundleDryRunResult.DryRunItem, List<String>> itemToConflicts = new HashMap<>();
                    itemToConflicts.put(PolicyBundleDryRunResult.DryRunItem.SERVICES, urlPatternWithConflict);
                    itemToConflicts.put(PolicyBundleDryRunResult.DryRunItem.POLICIES, policyWithNameConflict);
                    itemToConflicts.put(PolicyBundleDryRunResult.DryRunItem.CERTIFICATES, certificateWithConflict);
                    itemToConflicts.put(PolicyBundleDryRunResult.DryRunItem.ENCAPSULATED_ASSERTION, encapsulatedAssertionWithConflict);
                    itemToConflicts.put(PolicyBundleDryRunResult.DryRunItem.JDBC_CONNECTIONS, jdbcConnsThatDontExist);
                    itemToConflicts.put(PolicyBundleDryRunResult.DryRunItem.ASSERTIONS, missingRequiredAssertions);

                    bundleToConflicts.put(bundleId, itemToConflicts);

                    // any conflicts found?
                    if (!details.isEmpty()) {
                        final DetailedAdminEvent problemEvent =
                                new DetailedAdminEvent(this,
                                        MessageFormat.format("Pre installation check conflicts for component {0} found.", bundleInfo.getName()),
                                        Level.INFO);
                        problemEvent.setAuditDetails(details);
                        appEventPublisher.publishEvent(problemEvent);
                    }
                    processedComponents.add(bundleId);
                }
            }
        }

        if (processedComponents.containsAll(componentIds)) {
            final DetailedAdminEvent stoppedEvent =
                    new DetailedAdminEvent(this, MessageFormat.format(PRE_INSTALLATION_MESSAGE, getInstallerName(), "completed"), Level.INFO);
            appEventPublisher.publishEvent(stoppedEvent);
        } else {
            final DetailedAdminEvent cancelledEvent =
                    new DetailedAdminEvent(this, MessageFormat.format(PRE_INSTALLATION_MESSAGE, getInstallerName(), "cancelled"), Level.INFO);
            appEventPublisher.publishEvent(cancelledEvent);
        }

        return new PolicyBundleDryRunResult(bundleToConflicts);
    }

    private boolean validateEventProcessed(PolicyBundleEvent bundleEvent) throws PolicyBundleInstallerException {
        if (!bundleEvent.isProcessed()) {
            final String reason = bundleEvent.getReasonNotProcessed();
            if (reason != null) {
                throw new PolicyBundleInstallerException(reason);
            } else {
                throw new PolicyBundleInstallerException("Policy Bundle Installer module is not installed.");
            }
        }

        final Exception processingException = bundleEvent.getProcessingException();
        if (processingException != null) {
            if (!(processingException instanceof BundleResolver.UnknownBundleException)) {
                if (processingException instanceof InterruptedException) {
                    return true;
                }

                if (processingException instanceof AccessDeniedManagementResponse) {
                    AccessDeniedManagementResponse accessDeniedException = (AccessDeniedManagementResponse) processingException;
                    throw new PolicyBundleInstallerException("Problem installing " +
                            bundleEvent.getContext().getBundleInfo().getName() + ": " + accessDeniedException.getMessage());
                }

                // unexpected exception
                logger.warning("Exception type: " + processingException.getClass().getName());
                logger.warning("Unexpected error during installation: " + ExceptionUtils.getMessage(processingException));
                throw new PolicyBundleInstallerException(processingException);
            } else {
                throw new PolicyBundleInstallerException(processingException);
            }
        }
        return Thread.interrupted() || bundleEvent.isCancelled();

    }

    protected List<String> doInstall(@NotNull final String taskIdentifier,
                                     @NotNull final Collection<String> componentIds,
                                     @NotNull final Goid folderGoid,
                                     @NotNull Map<String, BundleMapping> bundleMappings,
                                     @Nullable final String installationPrefix) throws PolicyBundleInstallerException {

        // When installing more than one bundle, allow for optimization of not trying to recreate items already created.
        // final Map<String, Object> contextMap = new HashMap<>();

        final List<String> installedBundles = new ArrayList<>();
        if (isInstallInProgress.compareAndSet(false, true)) {

            final String prefixToUse = (installationPrefix != null && !installationPrefix.trim().isEmpty()) ?
                    installationPrefix : null;

            final String prefixMsg = (prefixToUse == null) ? "" : "with installation prefix '" + prefixToUse + "'";
            final DetailedAdminEvent startedEvent = new DetailedAdminEvent(this, MessageFormat.format(INSTALLATION_MESSAGE, getInstallerName(), "started", prefixMsg), Level.INFO);
            appEventPublisher.publishEvent(startedEvent);

            try {
                final Set<String> processedComponents = new HashSet<>();
                //iterate through all the bundle names to install
                outer:
                for (String bundleId : componentIds) {
                    final JobContext jobContext = taskToJobContext.get(taskIdentifier);
                    if (jobContext.cancelled) {
                        break;
                    }

                    final List<BundleInfo> resultList = bundleResolver.getResultList();
                    for (BundleInfo bundleInfo : resultList) {
                        if (bundleInfo.getId().equals(bundleId)) {

                            final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                                    bundleInfo, folderGoid, bundleMappings.get(bundleId), prefixToUse, bundleResolver, checkingAssertionExistenceRequired);
                            final InstallPolicyBundleEvent installEvent =
                                    new InstallPolicyBundleEvent(this, context, getSavePolicyCallback(prefixToUse));
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
                    final DetailedAdminEvent completedEvent =
                            new DetailedAdminEvent(this, MessageFormat.format(INSTALLATION_MESSAGE, getInstallerName(), "completed", prefixMsg), Level.INFO);
                    appEventPublisher.publishEvent(completedEvent);
                } else {
                    final DetailedAdminEvent cancelledEvent =
                            new DetailedAdminEvent(this, MessageFormat.format(INSTALLATION_MESSAGE, getInstallerName(), "cancelled", prefixMsg), Level.INFO);
                    appEventPublisher.publishEvent(cancelledEvent);
                }

            } finally {
                isInstallInProgress.set(false);
            }
        } else {
            throw new PolicyBundleInstallerException("Install is already in progress");
        }

        return installedBundles;

    }

    /**
     * Override in subclass to add pre policy save logic.
     */
    protected PreBundleSavePolicyCallback getSavePolicyCallback(final String installationPrefix) throws PolicyBundleInstallerException {
        return null;
    }

    @NotNull
    protected String getInstallerName() {
        return this.getClass().getSimpleName();
    }

    @NotNull
    protected AuditDetail newAuditDetailInstallError(PolicyBundleInstallerException e) {
        return new AuditDetail(AssertionMessages.POLICY_BUNDLE_INSTALLER_ERROR, new String[]{getInstallerName(), ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
    }

    @NotNull
    protected AuditDetailMessage getAssertionMessageDryRunConflict() {
        return AssertionMessages.POLICY_BUNDLE_INSTALLER_DRY_RUN_CONFLICT;
    }
}
