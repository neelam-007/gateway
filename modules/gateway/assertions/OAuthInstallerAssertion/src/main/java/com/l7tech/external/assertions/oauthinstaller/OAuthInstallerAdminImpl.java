package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerAdminAbstractImpl;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerCallback;
import com.l7tech.server.policy.bundle.PolicyUtils;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.*;
import com.l7tech.xml.xpath.XpathUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import static com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAssertion.SECURE_ZONE_STORAGE_COMP_ID;
import static com.l7tech.server.event.AdminInfo.find;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getEntityName;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getNamespaceMap;

public class OAuthInstallerAdminImpl extends PolicyBundleInstallerAdminAbstractImpl implements OAuthInstallerAdmin {
    private static final Logger logger = Logger.getLogger(OAuthInstallerAdminImpl.class.getName());
    public static final String OAUTH_SLASH_CLIENTS = "oauth/clients";

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Inject
    private JdbcQueryingManager jdbcQueryingManager;
    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Inject
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;
    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Inject
    private JdbcConnectionManager jdbcConnectionManager;
    @Inject
    private SecurePasswordManager securePasswordManager;
    protected boolean integrateApiPortal;

    public OAuthInstallerAdminImpl(final String bundleBaseName, final String bundleInfoFileName, final String namespaceInstallerVersion, final ApplicationEventPublisher appEventPublisher) throws PolicyBundleInstallerException {
        super(bundleBaseName, bundleInfoFileName, namespaceInstallerVersion, appEventPublisher);
    }

    @NotNull
    @Override
    protected String getInstallerName() {
        return "OAuth Toolkit";
    }

    @NotNull
    protected AuditDetail newAuditDetailInstallError(PolicyBundleInstallerException e) {
        return new AuditDetail(AssertionMessages.OTK_INSTALLER_ERROR, new String[]{ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
    }

    @NotNull
    protected AuditDetailMessage getAssertionMessageDryRunConflict() {
        return AssertionMessages.OTK_DRY_RUN_CONFLICT;
    }

    @NotNull
    @Override
    public JobId<PolicyBundleDryRunResult> dryRunInstall(@NotNull Collection<String> componentIds,
                                                         @NotNull Map<String, BundleMapping> bundleMappings,
                                                         @Nullable String installationPrefix,
                                                         boolean integrateApiPortal) {
        super.checkingAssertionExistenceRequired = integrateApiPortal;
        this.integrateApiPortal = integrateApiPortal;
        return dryRunInstall(componentIds, bundleMappings, installationPrefix);
    }

    @NotNull
    @Override
    public JobId<ArrayList> install(@NotNull Collection<String> componentIds,
                                    @NotNull Goid folderGoid,
                                    @NotNull Map<String, BundleMapping> bundleMappings,
                                    @Nullable String installationPrefix,
                                    boolean integrateApiPortal) throws PolicyBundleInstallerException {
        super.checkingAssertionExistenceRequired = integrateApiPortal;
        this.integrateApiPortal = integrateApiPortal;
        return install(componentIds, folderGoid, bundleMappings, installationPrefix, null);
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
                                           final Goid otkUserPasswordGoid,
                                           final String newJdbcConnName,
                                           final List<String> grantHostNames,
                                           final boolean createUser,
                                           final boolean failIfUserExists) {

        final Future<String> future = executorService.submit(find(false).wrapCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                // Validate that the newJdbcConnName is unique before doing any work
                final JdbcConnection jdbcConnection = jdbcConnectionManager.getJdbcConnection(newJdbcConnName);
                if (jdbcConnection != null) {
                    return "A JDBC Connection with name '" + newJdbcConnName + "' already exists";
                }

                // Validate the grants before dong any work
                for (String userGrant : grantHostNames) {
                    if (!ValidationUtils.isValidMySQLHostName(userGrant)) {
                        return "Invalid mysql hostname grant '" + userGrant + "'";
                    }
                }

                // SK-17 and SK-30 validate the length of the otk database name and user name.
                if(otkDbName.length() > 64){
                    return "OTK Database name is too long. Max length is 64 characters.";
                }
                if(otkDbUsername.length() > 16){
                    return "OTK user name is too long. Max length is 16 characters.";
                }

                final String otkSchema = getOAuthDatabaseSchema();

                final JdbcConnection jdbcConn = new JdbcConnection();
                jdbcConn.setName("Temp conn " + UUID.randomUUID().toString());
                // no db applicable
                final String jdbcUrl = "jdbc:mysql://" + mysqlHost + ":" + mysqlPort;

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

                    final String anyError = transactionTemplate.execute(new TransactionCallback<String>() {
                        @Override
                        public String doInTransaction(TransactionStatus transactionStatus) {
                            // Note the transactionStatus is not useful here because the DDL statement cause an auto commit

                            //Create the database, Don't use 'if not exists' an error will be returned if the database exists.
                            String query = "CREATE DATABASE " + otkDbName + " CHARACTER SET utf8";
                            Object result = jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 100, Collections.emptyList());
                            if (result instanceof String) {
                                //This happens if the database already exists
                                //We don't want to rollback here since nothing has changed yet.
                                return "Error creating OTK database: " + result;
                            }

                            // we are pessimistic, assuming it will fail.
                            boolean rollback = true;
                            //These are the users that were created during the process. We keep them for rollback purposes
                            List<String> createdUsers = new ArrayList<>(grantHostNames.size());
                            //These are the users that were modified during the process. We keep them for rollback purposes
                            Map<String, String> savedUserPasswords = new HashMap<>(grantHostNames.size());
                            try {
                                String msg = checkCreateDbInterrupted();
                                if (msg != null) {
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
                                        return "Error creating OTK database: " + result;
                                    }

                                    msg = checkCreateDbInterrupted();
                                    if (msg != null) {
                                        return msg;
                                    }
                                }

                                String otkUserPassword = findPassword(otkUserPasswordGoid);
                                if (otkUserPassword == null) {
                                    return "Could not find password referenced by: " + otkUserPasswordGoid;
                                }

                                //create the user and grants.
                                for (String grantHost : grantHostNames) {
                                    if (createUser) {
                                        query = "CREATE USER '" + otkDbUsername + "'@'" + grantHost + "' IDENTIFIED BY '" + otkUserPassword + "'";
                                        result = jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 10, Collections.emptyList());
                                        if (result instanceof String) {
                                            //We are here if the user already exists.
                                            if (failIfUserExists) {
                                                return "User '" + otkDbUsername + "'@'" + grantHost + "' already exists: " + result;
                                            } else {
                                                // Update the users password toString() the one given.

                                                //save user password (needed for restoring)
                                                query = "SELECT password from mysql.user where user = '" + otkDbUsername + "' AND host = '" + grantHost + "'";
                                                result = jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 10, Collections.emptyList());
                                                if (result instanceof String) {
                                                    return "Error retrieving existing '" + otkDbUsername + "'@'" + grantHost + "' user password: " + result;
                                                }
                                                //noinspection unchecked
                                                Map<String, List<Object>> selectResult = (Map<String, List<Object>>) result;
                                                List<Object> passwordResult = selectResult.get("password");
                                                if(passwordResult != null && !passwordResult.isEmpty() && passwordResult.get(0) != null) {
                                                    String password = (String) selectResult.get("password").get(0);
                                                    savedUserPasswords.put(grantHost, password);
                                                }

                                                //need to set user password;
                                                query = "SET PASSWORD FOR '" + otkDbUsername + "'@'" + grantHost + "' = PASSWORD('" + otkUserPassword + "')";
                                                result = jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 10, Collections.emptyList());
                                                if (result instanceof String) {
                                                    return "Error updating '" + otkDbUsername + "'@'" + grantHost + "' user password: " + result;
                                                }
                                            }
                                        } else {
                                            // save the newly created user so they can be rolled back if an error occurs.
                                            createdUsers.add(grantHost);
                                        }
                                    } else {
                                        //need to test if user exists because grant will create it if it doesn't
                                        query = "SELECT user from mysql.user where user = '" + otkDbUsername + "' AND host = '" + grantHost + "' AND password = PASSWORD('" + otkUserPassword + "')";
                                        result = jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 10, Collections.emptyList());
                                        if (result instanceof String) {
                                            return "Error checking if user '" + otkDbUsername + "'@'" + grantHost + "' exists: " + result;
                                        }
                                        //noinspection unchecked
                                        Map<String, List<Object>> selectResult = (Map<String, List<Object>>) result;
                                        // the user must exist
                                        if (!selectResult.values().iterator().hasNext()) {
                                            return "Database user '" + otkDbUsername + "'@'" + grantHost + "' does not exists or password is incorrect";
                                        }
                                    }
                                    // grant access to db
                                    query = "GRANT ALL ON " + otkDbName + ".* TO '" + otkDbUsername + "'@'" + grantHost + "'";
                                    result = jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 100, Collections.emptyList());
                                    if (result instanceof String) {
                                        return "Error granting privileges for OTK database to user '" + otkDbUsername + "'@'" + grantHost + "': " + result;
                                    }

                                    msg = checkCreateDbInterrupted();
                                    if (msg != null) {
                                        return msg;
                                    }
                                }

                                //save the jdbc connection
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
                                    return "Could not create JDBC Connection due to SaveException: " + ExceptionUtils.getMessage(e);
                                }
                                rollback = false;
                            } finally {
                                if (rollback) {
                                    //drop the database
                                    query = "DROP DATABASE " + otkDbName;
                                    jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 100, Collections.emptyList());

                                    //remove created users
                                    for (String grant : createdUsers) {
                                        query = "DROP USER '" + otkDbUsername + "'@'" + grant + "'";
                                        jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 10, Collections.emptyList());
                                    }
                                    //reset changed passwords.
                                    for (String grant : savedUserPasswords.keySet()) {
                                        query = "SET PASSWORD FOR '" + otkDbUsername + "'@'" + grant + "' = '" + savedUserPasswords.get(grant) + "'";
                                        jdbcQueryingManager.performJdbcQuery(dataSource, query, null, 10, Collections.emptyList());
                                    }
                                }
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

    private String findPassword(Goid otkUserPasswordGoid) {
        try {
            SecurePassword securePassword = securePasswordManager.findByPrimaryKey(otkUserPasswordGoid);
            if (securePassword == null) {
                return null;
            }
            return new String(securePasswordManager.decryptPassword(securePassword.getEncodedPassword()));
        } catch (FindException | ParseException e) {
            return null;
        }
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
        List<Object> result = new ArrayList<>();

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

    private String checkCreateDbInterrupted(){
        if (Thread.interrupted()) {
            return "Create OTK Database cancelled";
        }

        return null;
    }

    @NotNull
    @Override
    protected PolicyBundleInstallerCallback getPolicyBundleInstallerCallback(final String installationPrefix) throws PolicyBundleInstallerException {
        return new PolicyBundleInstallerCallback() {
            @Override
            public void prePolicySave(@NotNull BundleInfo bundleInfo, @NotNull Element entityDetailElmReadOnly, @NotNull Document writeablePolicyDoc) throws CallbackException {

                // add in the version comment for every policy saved, whether it's a policy fragment or a service policy.
                addVersionCommentAssertionsToPolicy(bundleInfo, writeablePolicyDoc.getDocumentElement());

                if (installationPrefix != null) {
                    // if we have prefixed the installation, then we need to be able to update routing assertions to route to the prefixed URIs

                    // 1 - find routing URIs
                    updateProtectedServiceUrlForHost(writeablePolicyDoc.getDocumentElement(), installationPrefix);

                    // 2 - find context variables
                    updateBase64ContextVariableExpressionForHost(writeablePolicyDoc.getDocumentElement(), installationPrefix);
                }

                // Is the API portal being integrated? If not then we need to remove assertions from the policy
                if (SECURE_ZONE_STORAGE_COMP_ID.equals(bundleInfo.getId()) && !integrateApiPortal) {
                    // check the service being published
                    final String entityName = getEntityName(entityDetailElmReadOnly);
                    if (OAUTH_SLASH_CLIENTS.equals(entityName)) {
                        // we do not need to check for modular assertion dependencies, like any other
                        // if the user wants the API Portal integrated, then we will do that, it can be fixed manually later.
                        removeApiPortalIntegration(writeablePolicyDoc.getDocumentElement());
                    }
                }
            }
        };
    }

    protected void addVersionCommentAssertionsToPolicy(final BundleInfo bundleInfo, final Element policyElement) {
        final CommentAssertion ca = new CommentAssertion("Component version " + bundleInfo.getVersion() + " installed by OAuth installer version " + getVersion());

        final Element governingAllAssertion = XmlUtil.findFirstChildElement(policyElement);
        final Element firstChild = XmlUtil.findFirstChildElement(governingAllAssertion);

        final Document assertionDoc = WspWriter.getPolicyDocument(ca);
        final Element commentDocElement = assertionDoc.getDocumentElement();
        final Element versionCommentAssertion = XmlUtil.findFirstChildElement(commentDocElement);
        final Node node = firstChild.getParentNode().getOwnerDocument().importNode(versionCommentAssertion, true);
        governingAllAssertion.insertBefore(node, firstChild);
    }

    protected void updateProtectedServiceUrlForHost(final Element policyElement, final String installationPrefix) {
        final List<Element> protectedUrls = PolicyUtils.findProtectedUrls(policyElement);
        for (Element protectedUrl : protectedUrls) {
            final String routingUrlValue = protectedUrl.getAttribute("stringValue");
            final String updatedHostValue = getUpdatedHostValue(installationPrefix, routingUrlValue);
            if (updatedHostValue != null) {
                protectedUrl.setAttribute("stringValue", updatedHostValue);
                logger.fine("Updated routing URL from '" + routingUrlValue + "' to '" + updatedHostValue + "'");
            }
        }
    }

    protected void updateBase64ContextVariableExpressionForHost(final Element policyElement, final String installationPrefix) throws PolicyBundleInstallerCallback.CallbackException {
        final List<Element> contextVariables = PolicyUtils.findContextVariables(policyElement);
        for (Element contextVariable : contextVariables) {
            final Element variableToSetElm;
            final Element base64ExpressionElm;
            try {
                base64ExpressionElm = XmlUtil.findExactlyOneChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "Base64Expression");
                variableToSetElm = XmlUtil.findExactlyOneChildElementByName(contextVariable, "http://www.layer7tech.com/ws/policy", "VariableToSet");
            } catch (TooManyChildElementsException | MissingRequiredElementException e) {
                throw new PolicyBundleInstallerCallback.CallbackException("Problem finding variable value: " + ExceptionUtils.getMessage(e));
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

    /**
     * The SecureZone storage document is pre configured with the API Portal. If this integration is not needed then
     * it needs to be removed from the policy before publishing.
     *
     * The policy includes 'PORTAL_INTEGRATION' on each assertion (composite or non composite) specific to the API Portal.
     *
     * The policy has been written so that these items can be removed with no consequence on the remaining logic of the policy.
     * Note: This will remove 'branches' of policy in addition to individual assertions.
     *
     * @param writeableDocElm pre save write-able layer 7 policy document
     */
    protected void removeApiPortalIntegration(Element writeableDocElm) {
        final List<Element> foundComments = XpathUtil.findElements(writeableDocElm, ".//L7p:value[@stringValue='PORTAL_INTEGRATION']", getNamespaceMap());
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