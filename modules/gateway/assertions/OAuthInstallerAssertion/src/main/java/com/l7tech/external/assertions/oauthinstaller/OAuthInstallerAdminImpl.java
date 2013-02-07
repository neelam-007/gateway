package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.admin.DetailedAdminEvent;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.InstallPolicyBundleEvent;
import com.l7tech.server.event.wsman.PolicyBundleEvent;
import com.l7tech.server.event.wsman.WSManagementRequestEvent;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.util.*;
import com.l7tech.xml.xpath.XpathUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.inject.Inject;
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
import java.util.regex.Matcher;

import static com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAssertion.SECURE_ZONE_STORAGE_COMP_ID;
import static com.l7tech.server.event.AdminInfo.find;
import static com.l7tech.policy.bundle.PolicyBundleDryRunResult.DryRunItem;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getEntityName;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getNamespaceMap;

public class OAuthInstallerAdminImpl extends AsyncAdminMethodsImpl implements OAuthInstallerAdmin {

    public static final String NS_INSTALLER_VERSION = "http://ns.l7tech.com/2012/11/oauth-toolkit-bundle";
    public static final String LOOKUP_API_KEY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:LookupApiKey>\n" +
            "            <L7p:ApiKey stringValue=\"apikey\"/>\n" +
            "        </L7p:LookupApiKey>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";

    public OAuthInstallerAdminImpl(final String bundleBaseName, ApplicationEventPublisher appEventPublisher) throws OAuthToolkitInstallationException {
        this.appEventPublisher = appEventPublisher;
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
                                                            @Nullable final String installationPrefix,
                                                            final boolean integrateApiPortal) {

        final String taskIdentifier = UUID.randomUUID().toString();
        final JobContext jobContext = new JobContext(taskIdentifier);
        taskToJobContext.put(taskIdentifier, jobContext);

        final Future<PolicyBundleDryRunResult> future = executorService.submit(find(false).wrapCallable(new Callable<PolicyBundleDryRunResult>() {
            @Override
            public PolicyBundleDryRunResult call() throws Exception {
                try {
                    return doDryRunOtkInstall(taskIdentifier, otkComponentId, bundleMappings, installationPrefix, integrateApiPortal);
                } catch (OAuthToolkitInstallationException e) {
                    final OtkInstallationAuditEvent problemEvent = new OtkInstallationAuditEvent(this, "Problem during pre installation check of the OAuth Toolkit", Level.WARNING);
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

    /**
     * All bundles in bundleNames MUST USE the same GUIDS for all policies which have the same name. The name of a policy
     * is unique on a Gateway. If the bundles contain the same policy with different guids the bundles will not install.
     *
     * @param otkComponentId     names of all bundles to install. Bundles may depend on each others items, but there is no
     *                           install dependency order.
     * @param folderOid          oid of the folder to install into.
     * @param installationPrefix prefix to version the installation with
     * @param integrateApiPortal true if API portal should be integrated. See interface javadoc.
     * @return Job ID, which will report on which bundles were installed.
     * @throws IOException for any problem installing. Installation is cancelled on the first error.
     */
    @NotNull
    @Override
    public JobId<ArrayList> installOAuthToolkit(@NotNull final Collection<String> otkComponentId,
                                                final long folderOid,
                                                @NotNull final Map<String, BundleMapping> bundleMappings,
                                                @Nullable final String installationPrefix,
                                                final boolean integrateApiPortal) throws OAuthToolkitInstallationException {

        final String prefixToUse = (installationPrefix != null && !installationPrefix.isEmpty()) ? installationPrefix : null;
        if (prefixToUse != null) {
            final String errorMsg = BundleInfo.getPrefixedUrlErrorMsg(prefixToUse);
            if (errorMsg != null) {
                throw new OAuthToolkitInstallationException(errorMsg);
            }
        }

        final String taskIdentifier = UUID.randomUUID().toString();
        final JobContext jobContext = new JobContext(taskIdentifier);
        taskToJobContext.put(taskIdentifier, jobContext);

        final Future<ArrayList> future = executorService.submit(find(false).wrapCallable(new Callable<ArrayList>() {
            @Override
            public ArrayList call() throws Exception {
                try {
                    return new ArrayList<String>(doInstallOAuthToolkit(taskIdentifier, otkComponentId, folderOid, bundleMappings, prefixToUse, integrateApiPortal));
                } catch(OAuthToolkitInstallationException e) {
                    final OtkInstallationAuditEvent problemEvent = new OtkInstallationAuditEvent(this, "Problem during installation of the OAuth Toolkit", Level.WARNING);
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
    public List<BundleInfo> getAllOtkComponents() throws OAuthToolkitInstallationException {
        return new OAuthToolkitBundleResolver(bundleInfosFromJar).getResultList();
    }

    @NotNull
    @Override
    public String getOAuthDatabaseSchema() {

        final URL schemaResourceUrl = getClass().getResource("db/OAuth_Toolkit_Schema.sql");
        final byte[] bytes;
        try {
            bytes = IOUtils.slurpUrl(schemaResourceUrl);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error obtaining OTK database schema");
        }
        return new String(bytes, Charsets.UTF8);
    }

    @Override
    public JobId<String> createOtkDatabase(final String mysqlHost,
                                           final String mysqlPort,
                                           final String adminUsername,
                                           final String adminPassword,
                                           final String otkDbName,
                                           final String otkDbUsername,
                                           final String otkUserPassword,
                                           final String newJdbcConnName) {

        final Future<String> future = executorService.submit(find(false).wrapCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                final String otkSchema = getOAuthDatabaseSchema();

                final JdbcConnection jdbcConn = new JdbcConnection();
                jdbcConn.setName("Temp conn " + UUID.randomUUID().toString());
                // no db applicable
                final String jdbcUrl = "jdbc:mysql://"+mysqlHost+":"+mysqlPort;

                jdbcConn.setJdbcUrl(jdbcUrl);
                jdbcConn.setUserName(adminUsername);
                jdbcConn.setPassword(adminPassword);
                final String driverClass = "com.mysql.jdbc.Driver";
                jdbcConn.setDriverClass(driverClass);
                jdbcConn.setMinPoolSize(1);
                jdbcConn.setMaxPoolSize(1);

                String msg = checkCreateDbInterrupted();
                if (msg != null) {
                    return msg;
                }

                try {
                    final Pair<ComboPooledDataSource, String> pair = jdbcConnectionPoolManager.updateConnectionPool(jdbcConn, false);

                    final ComboPooledDataSource dataSource = pair.left;
                    TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

                    //todo rollback is not working for database and tables.
                    final String anyError = transactionTemplate.execute(new TransactionCallback<String>() {
                        @Override
                        public String doInTransaction(TransactionStatus transactionStatus) {
                            String query = "CREATE DATABASE IF NOT EXISTS " + otkDbName + " CHARACTER SET utf8";
                            Object result = jdbcQueryingManager.performJdbcQuery(dataSource, query, null,100, Collections.emptyList());
                            if (result instanceof String) {
                                transactionStatus.setRollbackOnly();
                                return (String) result;
                            }

                            String msg = checkCreateDbInterrupted();
                            if (msg != null) {
                                transactionStatus.setRollbackOnly();
                                return msg;
                            }

                            //use database
                            jdbcQueryingManager.performJdbcQuery(dataSource, "use " + otkDbName, null, 100, Collections.emptyList());

                            // create tables
                            int index = 0;
                            int oldIndex;
                            while (otkSchema.indexOf(";", index) > 0) {
                                oldIndex = index + 1;
                                index = otkSchema.indexOf(";", index);

                                query = otkSchema.substring(oldIndex - 1, index);
                                index++;
                                result = jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 2, Collections.emptyList());
                                if (result instanceof String) {
                                    transactionStatus.setRollbackOnly();
                                    return (String) result;
                                }

                                msg = checkCreateDbInterrupted();
                                if (msg != null) {
                                    transactionStatus.setRollbackOnly();
                                    return msg;
                                }
                            }

                            msg = checkCreateDbInterrupted();
                            if (msg != null) {
                                transactionStatus.setRollbackOnly();
                                return msg;
                            }

                            // grant access to db
                            query = "GRANT ALL ON " + otkDbName + ".* TO '" + otkDbUsername + "'@'%' IDENTIFIED BY '" + otkUserPassword + "'";
                            result = jdbcQueryingManager.performJdbcQuery(dataSource, query,  null,100, Collections.emptyList());
                            if (result instanceof String) {
                                transactionStatus.setRollbackOnly();
                                return (String) result;
                            }

                            query = "GRANT ALL ON " + otkDbName + ".* TO '" + otkDbUsername + "'@'localhost' IDENTIFIED BY '" + otkUserPassword + "'";
                            result = jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 100, Collections.emptyList());
                            if (result instanceof String) {
                                transactionStatus.setRollbackOnly();
                                return (String) result;
                            }

                            query = "GRANT ALL ON " + otkDbName + ".* TO '" + otkDbUsername + "'@'localhost.localdomain' IDENTIFIED BY '" + otkUserPassword + "'";
                            result = jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 100, Collections.emptyList());
                            if (result instanceof String) {
                                transactionStatus.setRollbackOnly();
                                return (String) result;
                            }

                            msg = checkCreateDbInterrupted();
                            if (msg != null) {
                                return msg;
                            }

                            final JdbcConnection otkJdbcConnection = new JdbcConnection();
                            otkJdbcConnection.setName(newJdbcConnName);
                            otkJdbcConnection.setDriverClass(driverClass);

                            otkJdbcConnection.setJdbcUrl(jdbcUrl + "/" + otkDbName);
                            otkJdbcConnection.setUserName(otkDbUsername);
                            otkJdbcConnection.setPassword(otkUserPassword);
                            otkJdbcConnection.setDriverClass(driverClass);
                            otkJdbcConnection.setMinPoolSize(3);
                            otkJdbcConnection.setMaxPoolSize(15);

                            try {
                                jdbcConnectionManager.save(otkJdbcConnection);
                            } catch (SaveException e) {
                                logger.log(Level.WARNING,
                                        "Could not create JDBC Connection with name '" + otkDbName + "'. SaveException: "
                                                + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                                transactionStatus.setRollbackOnly();
                                return "Could not create JDBC Connection due to SaveException: " + ExceptionUtils.getMessage(e);
                            }
                            // no error
                            return "";
                        }
                    });


                    if (!anyError.trim().isEmpty()) {
                        return anyError;
                    }
                } catch (Exception e) {
                    return ExceptionUtils.getMessage(e);
                } finally {
                    // do not remember this connection
                    jdbcConnectionPoolManager.deleteConnectionPool(jdbcConn.getName());
                }

                return "";
            }
        }));

        return registerJob(future, String.class);
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
                                                          @NotNull final Collection<String> otkComponentIds,
                                                          @NotNull final Map<String, BundleMapping> bundleMappings,
                                                          @Nullable final String installationPrefix,
                                                          final boolean integrateApiPortal) throws OAuthToolkitInstallationException {
        final OtkInstallationAuditEvent startedEvent = new OtkInstallationAuditEvent(this, MessageFormat.format(preInstallationMessage, "started"), Level.INFO);
        appEventPublisher.publishEvent(startedEvent);

        final String prefixToUse = (installationPrefix != null && !installationPrefix.isEmpty()) ? installationPrefix : null;
        if (prefixToUse != null) {
            final String errorMsg = BundleInfo.getPrefixedUrlErrorMsg(prefixToUse);
            if (errorMsg != null) {
                throw new OAuthToolkitInstallationException(errorMsg);
            }
        }

        final OAuthToolkitBundleResolver bundleResolver = new OAuthToolkitBundleResolver(bundleInfosFromJar);

        final HashMap<String, Map<DryRunItem, List<String>>> bundleToConflicts = new HashMap<String, Map<DryRunItem, List<String>>>();
        final Set<String> processedComponents = new HashSet<String>();
        outer:
        for (String bundleId : otkComponentIds) {
            final List<BundleInfo> resultList = bundleResolver.getResultList();
            for (BundleInfo bundleInfo : resultList) {
                if (bundleInfo.getId().equals(bundleId)) {
                    final JobContext jobContext = taskToJobContext.get(taskIdentifier);
                    if (jobContext.cancelled) {
                        break outer;
                    }

                    final PolicyBundleInstallerContext context = new PolicyBundleInstallerContext(
                            bundleInfo, bundleMappings.get(bundleId), prefixToUse, bundleResolver);

                    final DryRunInstallPolicyBundleEvent dryRunEvent =
                            new DryRunInstallPolicyBundleEvent(this, context);
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

                    final List<String> jdbcConnsThatDontExist = dryRunEvent.getJdbcConnsThatDontExist();
                    if (!jdbcConnsThatDontExist.isEmpty()) {
                        details.add(
                                new AuditDetail(
                                        AssertionMessages.OTK_DRY_RUN_CONFLICT,
                                        bundleInfo.getName(),
                                        "Missing JDBC Connections",
                                        jdbcConnsThatDontExist.toString()));
                    }

                    final List<String> missingModularAssertions = new ArrayList<String>();
                    if (SECURE_ZONE_STORAGE_COMP_ID.equals(bundleId) && integrateApiPortal) {
                        //check if API Portal integration is possible if it was requested.
                        final boolean isLookUpApiAvailable = isLookupApiKeyAssertionAvailable();
                        if (!isLookUpApiAvailable) {
                            missingModularAssertions.add("Look Up API Key");
                        }
                    }

                    final Map<DryRunItem, List<String>> itemToConflicts = new HashMap<DryRunItem, List<String>>();
                    itemToConflicts.put(DryRunItem.SERVICES, urlPatternWithConflict);
                    itemToConflicts.put(DryRunItem.POLICIES, policyWithNameConflict);
                    itemToConflicts.put(DryRunItem.JDBC_CONNECTIONS, jdbcConnsThatDontExist);
                    itemToConflicts.put(DryRunItem.MODULAR_ASSERTION, missingModularAssertions);


                    bundleToConflicts.put(bundleId, itemToConflicts);

                    // any conflicts found?
                    if (!details.isEmpty()) {
                        final OtkInstallationAuditEvent problemEvent =
                                new OtkInstallationAuditEvent(this,
                                        MessageFormat.format("OAuth Toolkit pre installation check conflicts for component {0} found.", bundleInfo.getName()),
                                        Level.INFO);
                        problemEvent.setAuditDetails(details);
                        appEventPublisher.publishEvent(problemEvent);
                    }
                    processedComponents.add(bundleId);
                }
            }
        }

        if (processedComponents.containsAll(otkComponentIds)) {
            final OtkInstallationAuditEvent stoppedEvent =
                    new OtkInstallationAuditEvent(this, MessageFormat.format(preInstallationMessage, "completed"), Level.INFO);
            appEventPublisher.publishEvent(stoppedEvent);
        } else {
            final OtkInstallationAuditEvent cancelledEvent =
                    new OtkInstallationAuditEvent(this, MessageFormat.format(preInstallationMessage, "cancelled"), Level.INFO);
            appEventPublisher.publishEvent(cancelledEvent);
        }

        return new PolicyBundleDryRunResultImpl(bundleToConflicts);
    }

    /**
     * Perform the work of installing the OTK.
     *
     * @param taskIdentifier used to look up the context to see if task has been cancelled.
     * @param otkComponentIds component to install
     * @param folderOid folder to install component into
     * @param bundleMappings any mappings.
     * @param installationPrefix installation prefix
     * @return Ids of installed bundles
     * @throws OAuthToolkitInstallationException
     *
     */
    protected List<String> doInstallOAuthToolkit(@NotNull final String taskIdentifier,
                                                 @NotNull final Collection<String> otkComponentIds,
                                                 final long folderOid,
                                                 @NotNull Map<String, BundleMapping> bundleMappings,
                                                 @Nullable final String installationPrefix,
                                                 final boolean integrateApiPortal) throws OAuthToolkitInstallationException {

        final List<String> installedBundles = new ArrayList<String>();
        final OAuthToolkitBundleResolver bundleResolver = new OAuthToolkitBundleResolver(bundleInfosFromJar);
        if (isInstallInProgress.compareAndSet(false, true)) {

            final String prefixToUse = (installationPrefix != null && !installationPrefix.trim().isEmpty()) ?
                    installationPrefix : null;

            final String prefixMsg = (prefixToUse == null) ? "" : "with installation prefix '" + prefixToUse + "'";
            final OtkInstallationAuditEvent startedEvent = new OtkInstallationAuditEvent(this, MessageFormat.format(installationMessage, "started", prefixMsg), Level.INFO);
            appEventPublisher.publishEvent(startedEvent);

            try {
                bundleResolver.setInstallationPrefix(prefixToUse);

                final Set<String> processedComponents = new HashSet<String>();
                //iterate through all the bundle names to install
                outer:
                for (String bundleId : otkComponentIds) {
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
                                    new InstallPolicyBundleEvent(this,
                                            context,
                                            getSavePolicyCallback(prefixToUse, integrateApiPortal));
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
                if (processedComponents.containsAll(otkComponentIds)) {
                    final OtkInstallationAuditEvent completedEvent =
                            new OtkInstallationAuditEvent(this, MessageFormat.format(installationMessage, "completed", prefixMsg), Level.INFO);
                    appEventPublisher.publishEvent(completedEvent);
                } else {
                    final OtkInstallationAuditEvent cancelledEvent =
                            new OtkInstallationAuditEvent(this, MessageFormat.format(installationMessage, "cancelled", prefixMsg), Level.INFO);
                    appEventPublisher.publishEvent(cancelledEvent);
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
     * of the string to have the installationPrefix inserted after the variable reference.
     *
     * @param installationPrefix    prefix to insert
     * @param valueWithPossibleHost value which may need to be updated in a prefixed installation.
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

    /**
     * Wired via OAuthInstallerAssertion meta data.
     *
     * @param context spring application context
     */
    public static synchronized void onModuleLoaded(final ApplicationContext context) {

        if (spring != null) {
            logger.log(Level.WARNING, "OAuth Installer module is already initialized");
        } else {
            spring = context;
        }
    }

    // - PACKAGE

    static class OtkInstallationAuditEvent extends DetailedAdminEvent {
        public OtkInstallationAuditEvent(final Object source, final String note, final Level level) {
            super(source, note, level);
        }
    }

    // - PRIVATE

    private final AtomicBoolean isInstallInProgress = new AtomicBoolean(false);
    private static final Logger logger = Logger.getLogger(OAuthInstallerAdminImpl.class.getName());
    private final String oAuthInstallerVersion;
    private final List<Pair<BundleInfo, String>> bundleInfosFromJar;
    private final ApplicationEventPublisher appEventPublisher;
    private final ExecutorService executorService;
    private final Map<String, JobContext> taskToJobContext = new ConcurrentHashMap<String, JobContext>();
    private final String installationMessage = "Installation of the OAuth Toolkit {0} {1}";
    private final String preInstallationMessage = "Pre installation check of the OAuth Toolkit {0}";

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Inject
    private JdbcQueryingManager jdbcQueryingManager;
    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Inject
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;
    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Inject
    private JdbcConnectionManager jdbcConnectionManager;

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
            final String reason = bundleEvent.getReasonNotProcessed();
            if (reason != null) {
                throw new OAuthToolkitInstallationException(reason);
            } else {
                throw new OAuthToolkitInstallationException("Policy Bundle Installer module is not installed.");
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
                    throw new OAuthToolkitInstallationException("Problem installing " +
                            bundleEvent.getContext().getBundleInfo().getName() + ": " + accessDeniedException.getMessage());
                }

                // unexpected exception
                logger.warning("Exception type: " + processingException.getClass().getName());
                logger.warning("Unexpected error during installation: " + ExceptionUtils.getMessage(processingException));
                throw new OAuthToolkitInstallationException(processingException);
            } else {
                throw new OAuthToolkitInstallationException(processingException);
            }
        }
        return Thread.interrupted() || bundleEvent.isCancelled();

    }

    private String checkCreateDbInterrupted(){
        if (Thread.interrupted()) {
            return "Create OTK Database cancelled";
        }

        return null;
    }

    @NotNull
    private PreBundleSavePolicyCallback getSavePolicyCallback(final String installationPrefix,
                                                              final boolean integrateApiPortal) {
        return new PreBundleSavePolicyCallback() {
            @Override
            public void prePublishCallback(@NotNull BundleInfo bundleInfo, @NotNull Element entityDetailElmReadOnly, @NotNull Document writeablePolicyDoc) throws PolicyUpdateException {

                // add in the version comment for every policy saved, whether it's a policy fragment or a service policy.
                final String version = bundleInfo.getVersion();
                final CommentAssertion ca = new CommentAssertion("Component version " + version + " installed by OAuth installer version " + oAuthInstallerVersion);

                final Element governingAllAssertion = XmlUtil.findFirstChildElement(writeablePolicyDoc.getDocumentElement());
                final Element firstChild = XmlUtil.findFirstChildElement(governingAllAssertion);

                final Document assertionDoc = WspWriter.getPolicyDocument(ca);
                final Element commentDocElement = assertionDoc.getDocumentElement();
                final Element versionCommentAssertion = XmlUtil.findFirstChildElement(commentDocElement);
                final Node node = firstChild.getParentNode().getOwnerDocument().importNode(versionCommentAssertion, true);
                governingAllAssertion.insertBefore(node, firstChild);

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

                // Is the API portal being integrated? If not then we need to remove assertions from the policy
                if (SECURE_ZONE_STORAGE_COMP_ID.equals(bundleInfo.getId()) && !integrateApiPortal) {
                    // check the service being published
                    final String entityName = getEntityName(entityDetailElmReadOnly);
                    if ("oauth/clients".equals(entityName)) {
                        // we do not need to check for modular assertion dependencies, like any other
                        // if the user wants the API Portal integrated, then we will do that, it can be fixed manually later.
                        removeApiPortalIntegration(writeablePolicyDoc);
                    }
                }
            }
        };
    }

    private boolean isLookupApiKeyAssertionAvailable() {
        if (spring == null) {
            throw new IllegalStateException("OAuth Installer is not configured. ApplicationContext is missing");
        }

        final WspReader wspReader = spring.getBean("wspReader", WspReader.class);
        try {
            wspReader.parseStrictly(LOOKUP_API_KEY_XML, WspReader.Visibility.omitDisabled);
        } catch (IOException e) {
            // Lookup API Key assertion is not installed
            return false;
        }
        return true;
    }

    /**
     * The SecureZone storage document is pre configured with the API Portal. If this integration is not needed then
     * it needs to be removed from the policy before publishing.
     *
     * The policy includes 'PORTAL_INTEGRATION' on each assertion (composite or non composite) specific to the API Portal.
     *
     * The policy has been written so that these items can be removed with no consequence on the remaining logic of the policy.
     * Note: This will remove 'branches' of policy in addition to individual assertions.
     *
     * @param writeableDoc pre save writeable layer 7 policy document
     */
    private void removeApiPortalIntegration(Document writeableDoc) {
        // find all portal assertions:
        final List<Element> foundComments = XpathUtil.findElements(writeableDoc.getDocumentElement(), ".//L7p:value[@stringValue='PORTAL_INTEGRATION']", getNamespaceMap());
        for (Element foundComment : foundComments) {
            // verify it's a left comment
            final Element entryParent = (Element) foundComment.getParentNode();
            final Node keyElm = DomUtils.findFirstChildElement(entryParent);

            final Node stringValue = keyElm.getAttributes().getNamedItem("stringValue");
            if (! "LEFT.COMMENT".equals(stringValue.getNodeValue())) {
                continue;
            }

            // get the assertion element
            final Element assertionElm = (Element) entryParent.getParentNode().getParentNode().getParentNode();
            final Node assertionParentElm = assertionElm.getParentNode();
            assertionParentElm.removeChild(assertionElm);
        }
    }
}
