package com.l7tech.server.ems.ui.pages;

import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.ems.ui.EsmSecurityManager;
import com.l7tech.server.ems.migration.MigrationRecordManager;
import com.l7tech.server.ems.standardreports.StandardReportManager;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.server.ems.gateway.GatewayTrustTokenFactory;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.gateway.GatewayRegistrationEvent;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAny;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.exception.ConstraintViolationException;
import org.mortbay.util.ajax.JSON;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 */
@SuppressWarnings({"UnusedDeclaration"})
@NavigationPage(page="Configure",pageIndex=100,section="ManageGateways",sectionPage="Configure",pageUrl="Configure.html")
public class Configure extends EsmStandardWebPage {

    private static final Logger logger = Logger.getLogger(Configure.class.getName());

    @SpringBean
    private Config config;

    @SpringBean
    private GatewayTrustTokenFactory gatewayTrustTokenFactory;

    @SpringBean
    private GatewayContextFactory gatewayContextFactory;

    @SpringBean
    private EnterpriseFolderManager enterpriseFolderManager;

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    @SpringBean
    private SsgNodeManager ssgNodeManager;

    @SpringBean
    private EsmSecurityManager securityManager;

    @SpringBean
    private UserPropertyManager userPropertyManager;

    @SpringBean
    private StandardReportManager standardReportManager;

    @SpringBean
    private MigrationRecordManager migrationRecordManager;

    @SpringBean
    ApplicationEventPublisher publisher;

    public Configure() {
        Map<String,String> up = Collections.emptyMap();
        try {
            up = userPropertyManager.getUserProperties( getUser() );
        } catch ( FindException fe ){
            logger.log( Level.WARNING, "Error loading user properties", fe );
        }
        final Map<String,String> userProperties = up;

        JsonInteraction interaction = new JsonInteraction("actiondiv", "jsonUrl", new JsonDataProvider(){
            @Override
            public Object getData() {
                try {
                    final List<Object> entities = new ArrayList<Object>();
                    final EnterpriseFolder rootFolder = enterpriseFolderManager.findRootFolder();
                    entities.add(new JSONSupport( rootFolder ){
                        @Override
                        protected void writeJson() {
                            super.writeJson();
                            add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_ENTERPRISE_FOLDER, rootFolder)) );
                        }
                    } );
                    addChildren(entities, rootFolder);
                    return entities;
                } catch (FindException e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }

            @Override
            public void setData(Object jsonData) {
                throw new UnsupportedOperationException("setData not required in JsonInteraction");
            }

            private void addChildren(final List<Object> nodes, final EnterpriseFolder folder) throws FindException {
                // Display order is alphabetical on name, with folders before clusters.
                for ( final EnterpriseFolder childFolder : enterpriseFolderManager.findChildFolders(folder)) {
                    nodes.add( new JSONSupport( childFolder ){
                        @Override
                        protected void writeJson() {
                            super.writeJson();
                            add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_ENTERPRISE_FOLDER, childFolder)) );
                        }
                    }  );
                    addChildren(nodes, childFolder);
                }

                for ( final SsgCluster childCluster : ssgClusterManager.findChildSsgClusters(folder) ) {
                    nodes.add( new JSONSupport( childCluster ){
                        @Override
                        protected void writeJson() {
                            super.writeJson();
                            add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_SSG_CLUSTER, childCluster)) );
                            add(JSONConstants.ACCESS_STATUS, userProperties.containsKey("cluster." +  childCluster.getGuid() + ".trusteduser"));
                        }
                    });

                    // Add SSG nodes
                    for ( SsgNode node : new TreeSet<SsgNode>(childCluster.getNodes()) ) {
                        nodes.add( new JSONSupport( node ){
                            @Override
                            protected void writeJson() {
                                super.writeJson();
                                add( JSONConstants.ACCESS_STATUS, securityManager.hasPermission( new AttemptedReadSpecific(EntityType.ESM_SSG_CLUSTER, childCluster.getId())) );
                            }
                        } );
                    }
                }
            }
        });

        final HiddenField addFolderDialogInputParentId = new HiddenField("addFolderDialog_parentId", new Model(""));
        final RequiredTextField addFolderInputName = new RequiredTextField("addFolderDialog_name", new Model(""));
        Form addFolderForm = new JsonDataResponseForm("addFolderForm", new AttemptedCreate( EntityType.ESM_ENTERPRISE_FOLDER )){
            @Override
            protected Object getJsonResponseData() {
                String newFolderName = (String)addFolderInputName.getConvertedInput();
                String parentFolderGuid = (String)addFolderDialogInputParentId.getConvertedInput();
                try {
                    logger.fine("Adding folder \"" + newFolderName + "\" (parent folder GUID = " + parentFolderGuid + ").");
                    //noinspection UnnecessaryLocalVariable
                    final EnterpriseFolder newFolder = enterpriseFolderManager.create(newFolderName, parentFolderGuid);
                    return newFolder;
                } catch (Exception e) {
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            EnterpriseFolder parentFolder = enterpriseFolderManager.findByGuid(parentFolderGuid);
                            String errorMsg = "A folder with the name '" + newFolderName + "' already exists in the folder '"
                                + parentFolder.getName() + "'.<br/>Please specify a different name.";
                            return new JSONException(new DuplicateObjectException(errorMsg, e));
                        } catch (FindException e1) {
                            return new JSONException(e1);
                        }
                    } else {
                        return new JSONException(e);
                    }
                }
            }
        };
        addFolderForm.add(addFolderDialogInputParentId);
        addFolderForm.add(addFolderInputName);

        final HiddenField renameFolderDialogInputId = new HiddenField("renameFolderDialog_id", new Model(""));
        final RequiredTextField renameFolderInputName = new RequiredTextField("renameFolderDialog_name", new Model(""));
        Form renameFolderForm = new JsonDataResponseForm("renameFolderForm", new AttemptedUpdateAny( EntityType.ESM_ENTERPRISE_FOLDER )){
            @Override
            protected Object getJsonResponseData() {
                String renamedFolderGuid = (String)renameFolderDialogInputId.getConvertedInput();
                String newFolderName = (String)renameFolderInputName.getConvertedInput();
                try {
                    logger.fine("Renaming folder (GUID = "+ renamedFolderGuid + ") with a new name, " + newFolderName);

                    enterpriseFolderManager.renameByGuid(newFolderName, renamedFolderGuid);
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            EnterpriseFolder renamedFolder = enterpriseFolderManager.findByGuid(renamedFolderGuid);
                            EnterpriseFolder parentFolder = renamedFolder.getParentFolder();
                            String errorMsg = "A folder with the name '" + newFolderName + "' already exists in the folder '"
                                + parentFolder.getName() + "'.<br/>Please specify a different name.";
                            return new JSONException(new DuplicateObjectException(errorMsg, e));
                        } catch (FindException e1) {
                            return new JSONException(e1);
                        }
                    } else {
                        return new JSONException(e);
                    }
                }
            }
        };
        renameFolderForm.add(renameFolderDialogInputId);
        renameFolderForm.add(renameFolderInputName);

        final HiddenField deleteFolderDialogInputId = new HiddenField("deleteFolderDialog_id", new Model(""));
        Form deleteFolderForm = new JsonDataResponseForm("deleteFolderForm", new AttemptedDeleteAll( EntityType.ESM_ENTERPRISE_FOLDER )){
            @Override
            protected Object getJsonResponseData() {
                try {
                    String deletedFolderGuid = (String)deleteFolderDialogInputId.getConvertedInput();
                    logger.fine("Deleting folder (GUID = "+ deletedFolderGuid + ").");

                    enterpriseFolderManager.deleteByGuid(deletedFolderGuid);
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        deleteFolderForm.add(deleteFolderDialogInputId );

        final HiddenField addSSGClusterDialogInputParentId = new HiddenField("addSSGClusterDialog_parentId", new Model(""));
        final RequiredTextField addSSGClusterInputName = new RequiredTextField("addSSGClusterDialog_name", new Model(""));
        final RequiredTextField addSSGClusterInputHostName = new RequiredTextField("addSSGClusterDialog_hostName", new Model(""));
        final RequiredTextField addSSGClusterInputPort = new RequiredTextField("addSSGClusterDialog_port", new Model(""));
        Form addSSGClusterForm = new JsonDataResponseForm("addSSGClusterForm", new AttemptedCreate( EntityType.ESM_SSG_CLUSTER )){
            @Override
            protected Object getJsonResponseData() {
                String newClusterName = (String)addSSGClusterInputName.getConvertedInput();
                String parentFolderGuid = (String)addSSGClusterDialogInputParentId.getConvertedInput();
                String hostname = (String)addSSGClusterInputHostName.getConvertedInput();
                int port = Integer.parseInt((String)addSSGClusterInputPort.getConvertedInput());
                try {
                    logger.fine("Adding SSG Cluster \""+ newClusterName +
                        "\" (parent folder GUID = "+ parentFolderGuid + ").");
                    return ssgClusterManager.create(newClusterName, hostname, port, parentFolderGuid);
                } catch (Exception e) {
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            EnterpriseFolder parentFolder = enterpriseFolderManager.findByGuid(parentFolderGuid);
                            String errorMsg = "A cluster with the name '" + newClusterName + "' already exists in the folder '"
                                + parentFolder.getName() + "'.<br/>Please specify a different name.";
                            return new JSONException(new DuplicateObjectException(errorMsg, e));
                        } catch (FindException e1) {
                            return new JSONException(e1);
                        }
                    } else if (ExceptionUtils.causedBy(e, DuplicateHostnameException.class)) {
                        String errorMsg = "A cluster with the hostname (" + hostname + ") already exists in the enterprise tree." +
                            "<br/>Please specify a different hostname.";
                        return new JSONException(new DuplicateObjectException(errorMsg, e));
                    } else {
                        return new JSONException(e);
                    }
                }
            }
        };
        addSSGClusterForm.add(addSSGClusterDialogInputParentId);
        addSSGClusterForm.add(addSSGClusterInputName);
        addSSGClusterForm.add(addSSGClusterInputHostName);
        addSSGClusterForm.add(addSSGClusterInputPort);

        final HiddenField renameSSGClusterDialogInputId = new HiddenField("renameSSGClusterDialog_id", new Model(""));
        final RequiredTextField renameSSGClusterInputName = new RequiredTextField("renameSSGClusterDialog_name", new Model(""));
        Form renameSSGClusterForm = new JsonDataResponseForm("renameSSGClusterForm", new AttemptedUpdateAny( EntityType.ESM_SSG_CLUSTER )){
            @Override
            protected Object getJsonResponseData() {
                String renamedSSGClusterGuid = (String)renameSSGClusterDialogInputId.getConvertedInput();
                String newClusterName = (String)renameSSGClusterInputName.getConvertedInput();
                try {
                    logger.fine("Renaming SSG Cluster (GUID = "+ renamedSSGClusterGuid + ") with a new name, " + newClusterName);

                    ssgClusterManager.renameByGuid(newClusterName, renamedSSGClusterGuid);
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            SsgCluster renamedCluster = ssgClusterManager.findByGuid(renamedSSGClusterGuid);
                            EnterpriseFolder parentFolder = renamedCluster.getParentFolder();
                            String errorMsg = "A cluster with the name '" + newClusterName + "' already exists in the folder '"
                                + parentFolder.getName() + "'.<br/>Please specify a different name.";
                            return new JSONException(new DuplicateObjectException(errorMsg, e));
                        } catch (FindException e1) {
                            return new JSONException(e1);
                        }
                    } else {
                        return new JSONException(e);
                    }
                }
            }
        };
        renameSSGClusterForm.add(renameSSGClusterDialogInputId);
        renameSSGClusterForm.add(renameSSGClusterInputName);

        final HiddenField deleteSSGClusterDialogInputId = new HiddenField("deleteSSGClusterDialog_id", new Model(""));
        Form deleteSSGClusterForm = new JsonDataResponseForm("deleteSSGClusterForm", new AttemptedDeleteAll( EntityType.ESM_SSG_CLUSTER )){
            @Override
            protected Object getJsonResponseData() {
                try {
                    String guid = (String)deleteSSGClusterDialogInputId.getConvertedInput();
                    logger.fine("Deleting SSG Cluster (GUID = "+ guid + ").");

                    ssgClusterManager.deleteByGuid(guid);
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, ConstraintViolationException.class)) {
                        return new JSON.Convertible() {
                            @Override
                            public void toJSON(JSON.Output output) {
                                output.add("reconfirm", true);
                            }

                            @Override
                            public void fromJSON(Map map) {
                                throw new UnsupportedOperationException("Mapping from JSON not supported.");
                            }
                        };
                    } else {
                        return new JSONException(e);
                    }
                }
            }
        };
        deleteSSGClusterForm.add(deleteSSGClusterDialogInputId );

        final HiddenField reconfirmSSGClusterDeletionDialogInputId = new HiddenField("reconfirmSSGClusterDeletionDialog_id", new Model(""));
        Form reconfirmSSGClusterDeletionForm = new JsonDataResponseForm("reconfirmSSGClusterDeletionForm", new AttemptedDeleteAll( EntityType.ESM_SSG_CLUSTER )){
            @Override
            protected Object getJsonResponseData() {
                try {
                    String guid = (String)reconfirmSSGClusterDeletionDialogInputId.getConvertedInput();
                    SsgCluster ssgCluster = ssgClusterManager.findByGuid(guid);

                    logger.fine("Deleting SSG Cluster (GUID = "+ guid + ") and other related information such as Standard Reports and Migration Records.");
                    // Delete other related info such as standard reports and migration records.
                    standardReportManager.deleteBySsgCluster(ssgCluster);
                    migrationRecordManager.deleteBySsgCluster(ssgCluster);
                    // Finally delete the SSG cluster
                    ssgClusterManager.deleteByGuid(guid);

                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        reconfirmSSGClusterDeletionForm.add(reconfirmSSGClusterDeletionDialogInputId );

        final Form getGatewayTrustServletInputsForm = new JsonDataResponseForm("getGatewayTrustServletInputsForm"){
            @Override
            protected Object getJsonResponseData() {
                try {
                    logger.fine("Responding to request for trust token.");
                    final String token = gatewayTrustTokenFactory.getTrustToken();
                    return new JSONSupport() {
                        @Override
                        protected void writeJson() {
                            add("token", token);
                        }
                    };
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        deleteSSGClusterForm.add(deleteSSGClusterDialogInputId );

        final HiddenField startSsgNodeInputId = new HiddenField("startSsgNode_id", new Model(""));
        Form startSsgNodeForm = new JsonDataResponseForm("startSsgNodeForm") {
            @Override
            protected Object getJsonResponseData() {
                String ssgNodeGuid = (String)startSsgNodeInputId.getConvertedInput();
                try {
                    logger.fine("Starting SSG Node (GUID = " + ssgNodeGuid + ").");
                    
                    SsgNode node = ssgNodeManager.findByGuid(ssgNodeGuid);
                    SsgCluster cluster = node.getSsgCluster();
                    if ( securityManager.hasPermission( new AttemptedReadSpecific( EntityType.ESM_SSG_CLUSTER, cluster.getId() ) ) ) {
                        GatewayContext gatewayContext = gatewayContextFactory.getGatewayContext(null, node.getIpAddress(), cluster.getAdminPort());
                        NodeManagementApi nodeManagementApi = gatewayContext.getManagementApi();

                        // Start the node
                        nodeManagementApi.startNode("default");

                        // Todo: the below updating node is for demo only.  We will remove the part later on, since GatewayPoller will update node status periodically.
                        // Update the node online status
                        node.setOnlineStatus("on");
                        publisher.publishEvent( new GatewayRegistrationEvent(this) );
                    }

                    // Return the response to the client
                    return node;
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        startSsgNodeForm.add(startSsgNodeInputId);

        final HiddenField stopSsgNodeInputId = new HiddenField("stopSsgNode_id", new Model(""));
        Form stopSsgNodeForm = new JsonDataResponseForm("stopSsgNodeForm") {
            @Override
            protected Object getJsonResponseData() {
                String ssgNodeGuid = (String)stopSsgNodeInputId.getConvertedInput();
                try {
                    logger.fine("Stoping SSG Node (GUID = " + ssgNodeGuid + ").");

                    SsgNode node = ssgNodeManager.findByGuid(ssgNodeGuid);
                    SsgCluster cluster = node.getSsgCluster();
                    if ( securityManager.hasPermission( new AttemptedReadSpecific( EntityType.ESM_SSG_CLUSTER, cluster.getId() ) ) ) {
                        GatewayContext gatewayContext = gatewayContextFactory.getGatewayContext(null, node.getIpAddress(), cluster.getAdminPort());
                        NodeManagementApi nodeManagementApi = gatewayContext.getManagementApi();

                        // Stop the node
                        nodeManagementApi.stopNode("default", 20000);

                        // Todo: the below updating node is for demo only.  We will remove this part later on, since GatewayPoller will update node status periodically.
                        // Update the node online status
                        node.setOnlineStatus("off");
                        publisher.publishEvent( new GatewayRegistrationEvent(this) );
                    }

                    // Return the response to the client
                    return node;
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        stopSsgNodeForm.add(stopSsgNodeInputId);

        add(addFolderForm);
        add(renameFolderForm);
        add(deleteFolderForm);
        add(addSSGClusterForm);
        add(renameSSGClusterForm);
        add(deleteSSGClusterForm);
        add(reconfirmSSGClusterDeletionForm);
        add(getGatewayTrustServletInputsForm);
        add(startSsgNodeForm);
        add(stopSsgNodeForm);
        add(interaction);
    }
}
