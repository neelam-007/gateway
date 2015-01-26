package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static com.l7tech.policy.bundle.PolicyBundleDryRunResult.DryRunItem.*;
import static com.l7tech.server.policy.bundle.PolicyBundleInstallerAbstractServerAssertion.Action.*;
import static org.apache.commons.lang.StringUtils.join;

/**
 * Abstract Server Assertion implementation, which must be extended and configured in a specific modular assertion module.
 * E.g. ServerOAuthInstallerAssertion and ServerSimplePolicyBundleInstallerAssertion to extend this class.
 *
 * This implementation provides a no-GUI Policy Bundle installer ("headless").
 * For restman, get migration bundle.  For wsman, execute dry run and install.
 * The following actions are supported: list components, restman get migration bundle XML, wsman dry run install, wsman install and custom.
 *
 * Provides actions through the following HTTP parameters:
 *      List components: list component id(s) available for this installer bundle.
 *          Input: Set pbi.action to list
 *          Output: list of component id(s), ";" separated,
 *
 *      Restman get migration bundle: get the restman migration bundle XML for the given component(s).
 *          Input:
 *              Set pbi.action to restman_get
 *              Set pbi.component_ids to a ";" separated list of component ids
 *              Optionally set pbi.version_modifier - optional version modifier
 *              Optionally set pbi.folder_goid - optional install folder (if not set, defaults to root folder)
 *          Output: the restman migration bundle XML for each component.
 *
 *      Wsman dry run install: execute wsman dry run install for given component(s).
 *          Input:
 *              Set pbi.action to wsman_dry_run
 *              Set pbi.component_ids to a ";" separated list of component ids
 *              Optionally set pbi.version_modifier - optional version modifier
 *              Optionally map JDBC connection name to new name
 *                  Set pbi.jdbc_connection.(component_id).name with existing name in bundle (e.g. OAuth)
 *                  Set pbi.jdbc_connection.(component_id).new_name with a new desired name (e.g. OAuth Dev)
 *          Output: the conflicts for each component.
 *              Associated component id
 *              Associated service conflict
 *              Associated policy conflict
 *              Associated certificate conflict
 *              Associated JDBC connections that don't exist
 *              Associated missing assertions
 *              Associated encapsulated assertion conflict
 *
 *      Wsman install: execute wsman install for given component(s).
 *          Input:
 *              Set pbi.action to wsman_install
 *              Set pbi.component_ids to a ";" separated list of component ids
 *              Optionally set pbi.version_modifier - optional version modifier
 *              Optionally set pbi.folder_goid - optional install folder (if not set, defaults to root folder)
 *              Optionally map JDBC connection name to new name
 *                  Set pbi.jdbc_connection.(component_id).name with existing name in bundle (e.g. OAuth)
 *                  Set pbi.jdbc_connection.(component_id).new_name with a new desired name (e.g. OAuth Dev)
 *          Output: ";" separated list of installed component ids
 *
 *      Custom: handle custom action in an implementation class.  E.g. ServerOAuthInstallerAssertion can implement customActionCallback() to get the OAuth DB schema.
 */
public abstract class PolicyBundleInstallerAbstractServerAssertion<AT extends Assertion> extends AbstractServerAssertion<AT>  {
    public static final String CONTEXT_VARIABLE_PREFIX = "";
    public static final String REQUEST_HTTP_PARAMETER = "request.http.parameter.";
    protected static final String L7 = "l7";
    protected static final char COMPONENT_ID_SEPARATOR_CHAR = ';';
    protected static final String COMPONENT_ID_SEPARATOR = String.valueOf(COMPONENT_ID_SEPARATOR_CHAR);

    protected static enum Action {
        list, restman_get, wsman_dry_run, wsman_install, custom
    }

    @Inject
    protected StashManagerFactory stashManagerFactory;

    protected final PolicyBundleInstallerAdmin policyBundleInstallerAdmin;

    private PolicyEnforcementContext context;
    private Map<String, BundleInfo> availableComponents;

    private boolean usesRequestHttpParams;

    /**
     * Get PolicyBundleInstallerAdmin from assertion metadata via Extension Interface binding
     */
    public PolicyBundleInstallerAbstractServerAssertion(final AT assertion, final ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);

        final AssertionMetadata meta = assertion.meta();
        Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext> factory = meta.get(AssertionMetadata.EXTENSION_INTERFACES_FACTORY);
        if (factory == null) {
            throw new PolicyAssertionException(assertion, "Unexpected exception, Extension Interfaces Factory must not be null.");
        }

        Collection<ExtensionInterfaceBinding> bindings = factory.call(applicationContext);
        if (bindings == null || bindings.size() != 1) {
            throw new PolicyAssertionException(assertion, "Unexpected exception, must have exactly one Extension Interfaces Binding.");
        }

        ExtensionInterfaceBinding<?> binding = bindings.iterator().next();
        policyBundleInstallerAdmin = (PolicyBundleInstallerAdmin) binding.getImplementationObject();
    }

    /**
     * Handle input action (server assertion entry point)
     */
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // save a handle to the context
        setContext(context);

        // set authenticated user credential
        final User currentUser = getContext().getDefaultAuthenticationContext().getLastAuthenticatedUser();
        final UserBean userBean = new UserBean(currentUser.getProviderId(), currentUser.getLogin());
        userBean.setUniqueIdentifier(currentUser.getId());
        policyBundleInstallerAdmin.setAuthenticatedUser(userBean);

        try {
            if (getAvailableComponents() == null) {
                List<BundleInfo> allComponents = policyBundleInstallerAdmin.getAllComponents();
                setAvailableComponents(new HashMap<String, BundleInfo>(allComponents.size()));
                for (BundleInfo bundleInfo : allComponents) {
                    getAvailableComponents().put(bundleInfo.getId(), bundleInfo);
                }
            }

            Action action;
            try {
                action = Action.valueOf(getContextVariable(CONTEXT_VARIABLE_PREFIX + "action"));
            } catch (NoSuchVariableException e) {
                action = Action.list;   // default when action not specified
            } catch (IllegalArgumentException e) {
                throw new PolicyAssertionException(assertion, "A valid installer action must be specified.", e);
            }

            switch (action) {
                case list:
                    list();
                    break;
                case restman_get:
                    restmanGet();
                    break;
                case wsman_dry_run:
                    wsmanDryRun();
                    break;
                case wsman_install:
                    wsmanInstall();
                    break;
                default:
                    customActionCallback();
                    break;
            }
        } catch (PolicyBundleInstallerServerAssertionException e) {
            throw e;
        } catch (Exception e) {
            throw new PolicyAssertionException(assertion, ExceptionUtils.getMessage(e));
        }

        return AssertionStatus.NONE;
    }

    public void setUsesRequestHttpParams(boolean usesRequestHttpParams) {
        this.usesRequestHttpParams = usesRequestHttpParams;
    }

    protected String getContextVariable(@NotNull final String name) throws NoSuchVariableException {
        if (usesRequestHttpParams) {
            return getContext().getVariable(REQUEST_HTTP_PARAMETER + name).toString();
        } else {
            return getContext().getVariable(name).toString();
        }
    }

    /**
     * Override in subclass for opportunity to support a custom action.
     */
    protected void customActionCallback() throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        // do nothing, extend in subclass if custom action needed
    }

    /**
     * Override in subclass for opportunity to customize admin dry run.
     */
    protected AsyncAdminMethods.JobId<PolicyBundleDryRunResult> callAdminDryRun(final List<String> componentIds,
                                                                                final Goid folder,
                                                                                final HashMap<String, BundleMapping> mappings,
                                                                                final String versionModifier) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        return policyBundleInstallerAdmin.dryRunInstall(componentIds, mappings, folder, versionModifier);
    }

    /**
     * Override in subclass for opportunity to customize admin install.
     */
    protected AsyncAdminMethods.JobId<ArrayList> callAdminInstall(final List<String> componentIds,
                                                                  final Goid folder,
                                                                  final HashMap<String, BundleMapping> mappings,
                                                                  final String versionModifier) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        return policyBundleInstallerAdmin.install(componentIds, folder, mappings, versionModifier, null);
    }

    /**
     * Write a XML document to the message response body.
     */
    protected void writeResponse(final Document document) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        try {
            writeResponse(XmlUtil.nodeToFormattedString(document));
        } catch (IOException e) {
            throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException(e);
        }
    }

    /**
     * Write a string to the message response body.
     */
    protected void writeResponse(final String string) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        try {
            final Message response = getContext().getResponse();
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(string.getBytes());
            response.initialize(getStashManagerFactory().createStashManager(), ContentTypeHeader.XML_DEFAULT, inputStream);
            response.getMimeKnob().getContentLength();
            response.getHttpResponseKnob().setStatus(HttpStatus.SC_OK);
        } catch (IOException e) {
            throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException(e);
        }
    }

    protected Map<String, BundleInfo> getAvailableComponents() {
        return availableComponents;
    }

    protected void setAvailableComponents(Map<String, BundleInfo> availableComponents) {
        this.availableComponents = availableComponents;
    }

    protected PolicyEnforcementContext getContext() {
        return context;
    }

    protected void setContext(PolicyEnforcementContext context) {
        this.context = context;
    }

    protected StashManagerFactory getStashManagerFactory() {
        return stashManagerFactory;
    }

    /**
     * List the component ids in a bundle.
     * Use no access modifier so method is accessible in test class, but not in subclass.
     */
    void list() throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        StringBuilder componentIds = new StringBuilder();
        for (String bundleInfoId : getAvailableComponents().keySet()) {
            componentIds.append(bundleInfoId);
            componentIds.append(COMPONENT_ID_SEPARATOR);
        }
        writeResponse(componentIds.toString());
    }

    /**
     * Get restman migration bundle xml by executing dry run install of given components.
     * Use no access modifier so method is accessible in test class, but not in subclass.
     */
    void restmanGet() throws InterruptedException, PolicyBundleInstallerAdmin.PolicyBundleInstallerException,
            NoSuchVariableException, AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException, PolicyAssertionException {
        final List<String> componentIds = getComponentIds(restman_get);

        // call policyBundleInstallerAdmin.dryRunInstall(...)
        final AsyncAdminMethods.JobId<PolicyBundleDryRunResult> jobId = callAdminDryRun(componentIds, getFolderGoid(), getMappings(componentIds), getVersionModifier());

        processJobResult(jobId, new Functions.UnaryVoidThrows<Object, Exception>() {
            @Override
            public void call(Object jobResultOut) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
                PolicyBundleDryRunResult dryRunResult = (PolicyBundleDryRunResult) jobResultOut;
                Map<String, String> componentIdToBundleXmlMap = dryRunResult.getComponentIdToBundleXmlMap();
                if (componentIdToBundleXmlMap != null) {
                    final StringBuilder sb = new StringBuilder();
                    for (String componentId : componentIdToBundleXmlMap.keySet()) {
                        sb.append(componentIdToBundleXmlMap.get(componentId)).append(System.lineSeparator());
                    }

                    writeResponse(sb.toString());
                }
            }
        });
    }

    private void processJobResult(final AsyncAdminMethods.JobId<? extends Serializable> jobId,
                                  final Functions.UnaryVoidThrows<Object, Exception> resultCallback) throws
            InterruptedException, PolicyBundleInstallerAdmin.PolicyBundleInstallerException,
            AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException, PolicyBundleInstallerServerAssertionException {

        while( true ) {
            final String status = policyBundleInstallerAdmin.getJobStatus( jobId );
            if ( status == null ) {
                throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException("Unknown jobid: " + jobId);
            } else if ( !status.startsWith( "a" ) ) {
                final AsyncAdminMethods.JobResult<? extends Serializable> jobResult = policyBundleInstallerAdmin.getJobResult( jobId );
                if ( jobResult.result != null ) {
                    try {
                        resultCallback.call(jobResult.result);
                        break;
                    } catch (PolicyBundleInstallerServerAssertionException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException(e);
                    }
                } else {
                    throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException(jobResult.throwableMessage);
                }
            } else {
                Thread.sleep( 5000L );
            }
        }
    }

    /**
     * Execute wsman dry run install for given components.
     * Use no access modifier so method is accessible in test class, but not in subclass.
     */
    void wsmanDryRun() throws InterruptedException, PolicyBundleInstallerAdmin.PolicyBundleInstallerException,
            NoSuchVariableException, AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException,
            PolicyBundleDryRunResult.UnknownBundleIdException, PolicyBundleInstallerServerAssertionException {
        final List<String> componentIds = getComponentIds(wsman_dry_run);

        // call policyBundleInstallerAdmin.dryRunInstall(...)
        AsyncAdminMethods.JobId<PolicyBundleDryRunResult> jobId = callAdminDryRun(componentIds, getFolderGoid(), getMappings(componentIds), getVersionModifier());

        processJobResult(jobId, new Functions.UnaryVoidThrows<Object, Exception>() {
            @Override
            public void call(Object jobResultOut) throws Exception {
                PolicyBundleDryRunResult dryRunResult = (PolicyBundleDryRunResult) jobResultOut;
                try {
                    final StringBuilder sb = new StringBuilder();
                    for (String componentId : componentIds) {
                        if (dryRunResult.anyConflictsForBundle(componentId)) {
                            sb.append("ComponentId: ").append(componentId).append(System.lineSeparator());
                            sb.append("ServiceConflict: ").append(join(dryRunResult.getConflictsForItem(componentId, SERVICES), COMPONENT_ID_SEPARATOR_CHAR)).append(System.lineSeparator());
                            sb.append("PolicyConflict: ").append(join(dryRunResult.getConflictsForItem(componentId, POLICIES), COMPONENT_ID_SEPARATOR_CHAR)).append(System.lineSeparator());
                            sb.append("CertificateConflict: ").append(join(dryRunResult.getConflictsForItem(componentId, CERTIFICATES), COMPONENT_ID_SEPARATOR_CHAR)).append(System.lineSeparator());
                            sb.append("JdbcConnectionsThatDontExist: ").append(join(dryRunResult.getConflictsForItem(componentId, JDBC_CONNECTIONS), COMPONENT_ID_SEPARATOR_CHAR)).append(System.lineSeparator());
                            sb.append("MissingAssertions: ").append(join(dryRunResult.getConflictsForItem(componentId, ASSERTIONS), COMPONENT_ID_SEPARATOR_CHAR)).append(System.lineSeparator());
                            sb.append("EncapsulatedAssertionConflict: ").append(join(dryRunResult.getConflictsForItem(componentId, ENCAPSULATED_ASSERTION), COMPONENT_ID_SEPARATOR_CHAR)).append(System.lineSeparator());
                        }
                    }

                    if (sb.length() > 0) {
                        throw new PolicyBundleInstallerServerAssertionException(assertion, sb.toString(), HttpStatus.SC_CONFLICT);
                    } else {
                        writeResponse(join(componentIds, COMPONENT_ID_SEPARATOR_CHAR));
                    }
                } catch (PolicyBundleDryRunResult.UnknownBundleIdException e) {
                    throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException(e);
                }
            }
        });
    }

    /**
     * Execute wsman install for given components.
     * Use no access modifier so method is accessible in test class, but not in subclass.
     */
    void wsmanInstall() throws InterruptedException, PolicyBundleInstallerAdmin.PolicyBundleInstallerException,
            NoSuchVariableException, AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException,
            PolicyBundleDryRunResult.UnknownBundleIdException, PolicyBundleInstallerServerAssertionException {
        final List<String> componentIds = getComponentIds(wsman_install);

        // call policyBundleInstallerAdmin.install(...)
        AsyncAdminMethods.JobId<ArrayList> jobId = callAdminInstall(componentIds, getFolderGoid(), getMappings(componentIds), getVersionModifier());

        processJobResult(jobId, new Functions.UnaryVoidThrows<Object, Exception>() {
            @Override
            public void call(Object jobResultOut) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
                final ArrayList installedComponentIds = (ArrayList) jobResultOut;
                writeResponse(join(installedComponentIds, COMPONENT_ID_SEPARATOR_CHAR));
            }
        });
    }

    /**
     * Return component IDs applicable for that action.  If action is null, return all component IDs for the installer.
     */
    List<String> getComponentIds(@NotNull Action action) throws NoSuchVariableException, PolicyBundleInstallerServerAssertionException {
        final String componentIdsStr = getContextVariable(CONTEXT_VARIABLE_PREFIX + "component_ids");
        List<String> componentIds;

        if ("all".equalsIgnoreCase(componentIdsStr)) {
            componentIds = new ArrayList<>(getAvailableComponents().size());
            for (BundleInfo bundleInfo : getAvailableComponents().values()) {
                addComponentId(componentIds, action, bundleInfo);
            }
        } else {
            List<String> inputIds = Arrays.asList(componentIdsStr.split(COMPONENT_ID_SEPARATOR));
            componentIds = new ArrayList<>(inputIds.size());
            for (String inputId : inputIds) {
                BundleInfo bundleInfo = getAvailableComponents().get(inputId);
                if (bundleInfo != null) {
                    addComponentId(componentIds, action, bundleInfo);
                }
            }
        }

        if (componentIds.size() <= 0) {
            throw new PolicyBundleInstallerServerAssertionException(assertion, "No matching component ID found for this installer.", HttpStatus.SC_NOT_FOUND);
        }

        return componentIds;
    }

    private void addComponentId(@NotNull List<String> componentIds, @NotNull Action action, @NotNull BundleInfo bundleInfo) throws PolicyBundleInstallerServerAssertionException {
        if ((bundleInfo.hasActiveVersionMigrationBundleFile() && action == restman_get) ||
                (bundleInfo.hasWsmanFile() && (action == wsman_dry_run || action == wsman_install))) {
            componentIds.add(bundleInfo.getId());
        } else {
            throw new PolicyBundleInstallerServerAssertionException(assertion, "Component ID: " +  bundleInfo.getId() + " not compatible with action: " + action, HttpStatus.SC_BAD_REQUEST);
        }
    }

    private String getVersionModifier() {
        try {
            return getContextVariable(CONTEXT_VARIABLE_PREFIX + "version_modifier");
        } catch (NoSuchVariableException e) {
            return null;
        }
    }

    private Goid getFolderGoid() {
        try {
            return new Goid(getContextVariable(CONTEXT_VARIABLE_PREFIX + "folder_goid"));
        } catch (NoSuchVariableException e) {
            return Folder.ROOT_FOLDER_ID;
        }
    }

    private HashMap<String, BundleMapping> getMappings(final List<String> componentIds) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        final HashMap<String, BundleMapping> mappings = new HashMap<>();
        for (String componentId : componentIds) {
            try {
                final String oldName = getContextVariable(CONTEXT_VARIABLE_PREFIX + "jdbc_connection." + componentId + ".name");
                final String newName = getContextVariable(CONTEXT_VARIABLE_PREFIX + "jdbc_connection." + componentId + ".new_name");

                final BundleMapping mapping = new BundleMapping();
                mapping.addMapping(BundleMapping.Type.JDBC_CONNECTION_NAME, oldName, newName);

                mappings.put(componentId, mapping);
            } catch (NoSuchVariableException e) {
                // don't add mapping
            }
        }
        return mappings;
    }
}
