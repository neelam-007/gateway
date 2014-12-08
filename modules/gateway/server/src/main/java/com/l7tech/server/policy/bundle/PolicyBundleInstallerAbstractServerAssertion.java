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
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static com.l7tech.policy.bundle.PolicyBundleDryRunResult.DryRunItem.*;
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
 *              Associated component id service conflict
 *              Associated component id policy conflict
 *              Associated component id certificate conflict
 *              Associated component id JDBC connections that don't exist
 *              Associated component id missing assertions
 *              Associated component id encapsulated assertion conflict
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
    protected static final String CONTEXT_VARIABLE_PREFIX = "request.http.parameter.";
    public static final String REQUEST_HTTP_PARAMETER_PBI = CONTEXT_VARIABLE_PREFIX + "pbi.";
    protected static final String L7 = "l7";

    protected static enum Action {
        list, restman_get, wsman_dry_run, wsman_install, custom
    }

    @Inject
    protected StashManagerFactory stashManagerFactory;

    protected final PolicyBundleInstallerAdmin policyBundleInstallerAdmin;

    protected PolicyEnforcementContext context;
    protected List<BundleInfo> availableComponents;

    /**
     * Get PolicyBundleInstallerAdmin from assertion metadata via Extension Interface binding
     */
    public PolicyBundleInstallerAbstractServerAssertion(final AT assertion, final ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);

        final AssertionMetadata meta = assertion.meta();
        Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext> factory = meta.get(AssertionMetadata.EXTENSION_INTERFACES_FACTORY);
        if (factory == null) {
            throw new PolicyAssertionException(assertion, "TODO");
        }

        Collection<ExtensionInterfaceBinding> bindings = factory.call(applicationContext);
        if (bindings == null || bindings.size() != 1) {
            throw new PolicyAssertionException(assertion, "TODO");
        }

        ExtensionInterfaceBinding<?> binding = bindings.iterator().next();
        policyBundleInstallerAdmin = (PolicyBundleInstallerAdmin) binding.getImplementationObject();
    }

    /**
     * Handle input action (server assertion entry point)
     */
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // save a handle to the context
        this.context = context;

        // set authenticated user credential
        final User currentUser = context.getDefaultAuthenticationContext().getLastAuthenticatedUser();
        final UserBean userBean = new UserBean(currentUser.getProviderId(), currentUser.getLogin());
        userBean.setUniqueIdentifier(currentUser.getId());
        policyBundleInstallerAdmin.setAuthenticatedUser(userBean);

        try {
            if (availableComponents == null) {
                availableComponents = policyBundleInstallerAdmin.getAllComponents();
            }

            final Action action = Action.valueOf(context.getVariable(REQUEST_HTTP_PARAMETER_PBI + "action").toString());
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
        } catch (Exception e) {
            throw new PolicyAssertionException(assertion, e);
        }

        return AssertionStatus.NONE;
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
                                                                                final HashMap<String, BundleMapping> mappings,
                                                                                final String versionModifier) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        return policyBundleInstallerAdmin.dryRunInstall(componentIds, mappings, versionModifier);
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
            final Message response = context.getResponse();
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(XmlUtil.nodeToFormattedString(document).getBytes());
            response.initialize(stashManagerFactory.createStashManager(), ContentTypeHeader.XML_DEFAULT, inputStream);
            response.getMimeKnob().getContentLength();
            response.getHttpResponseKnob().setStatus(200);
        } catch (IOException e) {
            throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException(e);
        }
    }

    /**
     * List the component ids in a bundle.
     */
    private void list() throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        StringBuilder componentIds = new StringBuilder();
        for (BundleInfo bundleInfo : availableComponents) {
            componentIds.append(bundleInfo.getId());
            componentIds.append(";");
        }

        final Document document = XmlUtil.createEmptyDocument("ComponentIds", L7, BundleUtils.NS_BUNDLE);
        DomUtils.setTextContent(document.getDocumentElement(), componentIds.toString());
        writeResponse(document);
    }

    /**
     * Get restman migration bundle xml by executing dry run install of given components.
     */
    private void restmanGet() throws InterruptedException, PolicyBundleInstallerAdmin.PolicyBundleInstallerException,
            NoSuchVariableException, AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException {
        final List<String> componentIds = getComponentIds();

        // call policyBundleInstallerAdmin.dryRunInstall(...)
        final AsyncAdminMethods.JobId<PolicyBundleDryRunResult> jobId = callAdminDryRun(componentIds, getMappings(componentIds), getVersionModifier());

        processJobResult(jobId, new Functions.UnaryVoidThrows<Object, PolicyBundleInstallerAdmin.PolicyBundleInstallerException>() {
            @Override
            public void call(Object jobResultOut) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
                PolicyBundleDryRunResult dryRunResult = (PolicyBundleDryRunResult) jobResultOut;
                Map<String, String> componentIdToBundleXmlMap = dryRunResult.getComponentIdToBundleXmlMap();
                if (componentIdToBundleXmlMap != null) {
                    final Document document = XmlUtil.createEmptyDocument("RestmanGet", L7, BundleUtils.NS_BUNDLE);
                    final Element rootElement = document.getDocumentElement();

                    for (String componentId : componentIdToBundleXmlMap.keySet()) {
                        Element element = DomUtils.createAndAppendElement(rootElement, "MigrationBundle");
                        element.setAttribute("componentId", componentId);
                        DomUtils.setTextContent(element, componentIdToBundleXmlMap.get(componentId));
                    }

                    writeResponse(document);
                }
            }
        });
    }

    private void processJobResult(final AsyncAdminMethods.JobId<? extends Serializable> jobId,
                                  final Functions.UnaryVoidThrows<Object, PolicyBundleInstallerAdmin.PolicyBundleInstallerException> resultCallback) throws
            InterruptedException, PolicyBundleInstallerAdmin.PolicyBundleInstallerException, AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException {

        while( true ) {
            final String status = policyBundleInstallerAdmin.getJobStatus( jobId );
            if ( status == null ) {
                throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException("Unknown jobid: " + jobId);
            } else if ( !status.startsWith( "a" ) ) {
                final AsyncAdminMethods.JobResult<? extends Serializable> jobResult = policyBundleInstallerAdmin.getJobResult( jobId );
                if ( jobResult.result != null ) {
                    resultCallback.call(jobResult.result);
                    break;
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
     */
    private void wsmanDryRun() throws InterruptedException, PolicyBundleInstallerAdmin.PolicyBundleInstallerException,
            NoSuchVariableException, AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException, PolicyBundleDryRunResult.UnknownBundleIdException {
        final List<String> componentIds = getComponentIds();

        // call policyBundleInstallerAdmin.dryRunInstall(...)
        AsyncAdminMethods.JobId<PolicyBundleDryRunResult> jobId = callAdminDryRun(componentIds, getMappings(componentIds), getVersionModifier());

        processJobResult(jobId, new Functions.UnaryVoidThrows<Object, PolicyBundleInstallerAdmin.PolicyBundleInstallerException>() {
            @Override
            public void call(Object jobResultOut) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
                PolicyBundleDryRunResult dryRunResult = (PolicyBundleDryRunResult) jobResultOut;
                try {
                    final Document document = XmlUtil.createEmptyDocument("WsmanDryRun", L7, BundleUtils.NS_BUNDLE);
                    final Element rootElement = document.getDocumentElement();

                    for (String componentId : componentIds) {
                        if (dryRunResult.anyConflictsForBundle(componentId)) {
                            Element conflictsElement = DomUtils.createAndAppendElement(rootElement, "Conflicts");
                            conflictsElement.setAttribute("componentId", componentId);

                            DomUtils.setTextContent(DomUtils.createAndAppendElement(conflictsElement, "ServiceConflict"), join(dryRunResult.getConflictsForItem(componentId, SERVICES), ';'));
                            DomUtils.setTextContent(DomUtils.createAndAppendElement(conflictsElement, "PolicyConflict"), join(dryRunResult.getConflictsForItem(componentId, POLICIES), ';'));
                            DomUtils.setTextContent(DomUtils.createAndAppendElement(conflictsElement, "CertificateConflict"), join(dryRunResult.getConflictsForItem(componentId, CERTIFICATES), ';'));
                            DomUtils.setTextContent(DomUtils.createAndAppendElement(conflictsElement, "JdbcConnectionsThatDontExist"), join(dryRunResult.getConflictsForItem(componentId, JDBC_CONNECTIONS), ';'));
                            DomUtils.setTextContent(DomUtils.createAndAppendElement(conflictsElement, "MissingAssertions"), join(dryRunResult.getConflictsForItem(componentId, ASSERTIONS), ';'));
                            DomUtils.setTextContent(DomUtils.createAndAppendElement(conflictsElement, "EncapsulatedAssertionConflict"), join(dryRunResult.getConflictsForItem(componentId, ENCAPSULATED_ASSERTION), ';'));
                        }
                    }

                    writeResponse(document);
                } catch (PolicyBundleDryRunResult.UnknownBundleIdException e) {
                    throw new PolicyBundleInstallerAdmin.PolicyBundleInstallerException(e);
                }
            }
        });
    }

    /**
     * Execute wsman install for given components.
     */
    private void wsmanInstall() throws InterruptedException, PolicyBundleInstallerAdmin.PolicyBundleInstallerException,
            NoSuchVariableException, AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException, PolicyBundleDryRunResult.UnknownBundleIdException {
        final List<String> componentIds = getComponentIds();

        // call policyBundleInstallerAdmin.install(...)
        AsyncAdminMethods.JobId<ArrayList> jobId = callAdminInstall(componentIds, getFolderGoid(), getMappings(componentIds), getVersionModifier());

        processJobResult(jobId, new Functions.UnaryVoidThrows<Object, PolicyBundleInstallerAdmin.PolicyBundleInstallerException>() {
            @Override
            public void call(Object jobResultOut) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
                ArrayList installedComponentIds = (ArrayList) jobResultOut;
                final Document document = XmlUtil.createEmptyDocument("ComponentIds", L7, BundleUtils.NS_BUNDLE);
                DomUtils.setTextContent(document.getDocumentElement(), join(installedComponentIds, ';'));
                writeResponse(document);
            }
        });
    }

    private List<String> getComponentIds() throws NoSuchVariableException {
        final String componentIdsStr = context.getVariable(REQUEST_HTTP_PARAMETER_PBI + "component_ids").toString();
        return Arrays.asList(componentIdsStr.split(";"));
    }

    private String getVersionModifier() {
        try {
            return context.getVariable(REQUEST_HTTP_PARAMETER_PBI + "version_modifier").toString();
        } catch (NoSuchVariableException e) {
            return null;
        }
    }

    private Goid getFolderGoid() {
        try {
            return new Goid(context.getVariable(REQUEST_HTTP_PARAMETER_PBI + "folder_goid").toString());
        } catch (NoSuchVariableException e) {
            return Folder.ROOT_FOLDER_ID;
        }
    }

    private HashMap<String, BundleMapping> getMappings(final List<String> componentIds) throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException {
        final HashMap<String, BundleMapping> mappings = new HashMap<>();
        for (String componentId : componentIds) {
            try {
                final String oldName = context.getVariable(REQUEST_HTTP_PARAMETER_PBI + "jdbc_connection.map." + componentId + ".name").toString();
                final String newName = context.getVariable(REQUEST_HTTP_PARAMETER_PBI + "jdbc_connection.map." + componentId + ".new_name").toString();

                final BundleMapping mapping = new BundleMapping();
                mapping.addMapping(BundleMapping.EntityType.JDBC_CONNECTION, oldName, newName);

                mappings.put(componentId, mapping);
            } catch (NoSuchVariableException e) {
                // don't add mapping
            }
        }
        return mappings;
    }
}
