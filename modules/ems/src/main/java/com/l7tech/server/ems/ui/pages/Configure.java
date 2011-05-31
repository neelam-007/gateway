package com.l7tech.server.ems.ui.pages;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayRegistrationEvent;
import com.l7tech.server.ems.gateway.GatewayTrustTokenFactory;
import com.l7tech.server.ems.gateway.ProcessControllerContext;
import com.l7tech.server.ems.migration.MigrationRecordManager;
import com.l7tech.server.ems.standardreports.StandardReportManager;
import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.ems.monitoring.EntityMonitoringPropertySetupManager;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import javax.inject.Inject;
import javax.inject.Named;
import org.hibernate.exception.ConstraintViolationException;
import org.mortbay.util.ajax.JSON;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
@SuppressWarnings({"UnusedDeclaration"})
@NavigationPage(page="Configure",pageIndex=100,section="ManageGateways",sectionPage="Monitor",pageUrl="Configure.html")
public class Configure extends EsmStandardWebPage {

    private static final Logger logger = Logger.getLogger(Configure.class.getName());

    @Inject
    private Config config;

    @Inject
    private GatewayTrustTokenFactory gatewayTrustTokenFactory;

    @Inject
    private GatewayContextFactory gatewayContextFactory;

    @Inject
    private EnterpriseFolderManager enterpriseFolderManager;

    @Inject
    private SsgClusterManager ssgClusterManager;

    @Inject
    private SsgNodeManager ssgNodeManager;

    @Inject
    private StandardReportManager standardReportManager;

    @Inject
    private MigrationRecordManager migrationRecordManager;

    @Inject
    @Named("eventPublisher")
    private ApplicationEventPublisher publisher;

    @Inject
    private EntityMonitoringPropertySetupManager entityMonitoringPropertySetupManager;

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

                for ( final SsgCluster childCluster : ssgClusterManager.findChildSsgClusters(folder, true) ) {
                    nodes.add( new JSONSupport( childCluster ){
                        @Override
                        protected void writeJson() {
                            super.writeJson();
                            add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_SSG_CLUSTER, childCluster)) );
                            if ( !childCluster.isOffline() ) {
                                add(JSONConstants.ACCESS_STATUS, userProperties.containsKey("cluster." +  childCluster.getGuid() + ".trusteduser"));
                            }
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

        final HiddenField<String> addFolderDialogInputParentId = new HiddenField<String>("addFolderDialog_parentId", new Model<String>(""));
        final RequiredTextField<String> addFolderInputName = new RequiredTextField<String>("addFolderDialog_name", new Model<String>(""));
        Form addFolderForm = new JsonDataResponseForm("addFolderForm", new AttemptedCreate( EntityType.ESM_ENTERPRISE_FOLDER )){
            @Override
            protected Object getJsonResponseData() {
                String newFolderName = addFolderInputName.getConvertedInput();
                String parentFolderGuid = addFolderDialogInputParentId.getConvertedInput();
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
                            return new JSONException("A folder with the name '" + newFolderName + "' already exists in the folder '"
                                    + parentFolder.getName() + "'. Please specify a different name.");
                        } catch (FindException e1) {
                            return new JSONException("A folder with the name '" + newFolderName + "' already exists. Please specify a different name.");
                        }
                    } else {
                        return new JSONException(e);
                    }
                }
            }
        };
        addFolderForm.add(addFolderDialogInputParentId);
        addFolderForm.add(addFolderInputName);

        final HiddenField<String> editFolderDialogInputId = new HiddenField<String>("editFolderDialog_id", new Model<String>(""));
        final RequiredTextField<String> editFolderInputName = new RequiredTextField<String>("editFolderDialog_name", new Model<String>(""));
        Form editFolderForm = new JsonDataResponseForm("editFolderForm", new AttemptedUpdateAny( EntityType.ESM_ENTERPRISE_FOLDER )){
            @Override
            protected Object getJsonResponseData() {
                String editedFolderGuid = editFolderDialogInputId.getConvertedInput();
                String newFolderName = editFolderInputName.getConvertedInput();
                try {
                    logger.fine("Editing folder (GUID = "+ editedFolderGuid + ") by changing a new name, " + newFolderName);

                    enterpriseFolderManager.renameByGuid(newFolderName, editedFolderGuid);
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            EnterpriseFolder editedFolder = enterpriseFolderManager.findByGuid(editedFolderGuid);
                            EnterpriseFolder parentFolder = editedFolder.getParentFolder();
                            return new JSONException("A folder with the name '" + newFolderName + "' already exists in the folder '"
                                    + parentFolder.getName() + "'. Please specify a different name.");
                        } catch (FindException e1) {
                            return new JSONException("A folder with the name '" + newFolderName + "' already exists. Please specify a different name.");
                        }
                    } else {
                        return new JSONException(e);
                    }
                }
            }
        };
        editFolderForm.add(editFolderDialogInputId);
        editFolderForm.add(editFolderInputName);

        final HiddenField<String> moveFolderDialogEntityId = new HiddenField<String>("moveFolderDialog_entityId", new Model<String>(""));
        final HiddenField<String> moveFolderDialogDestFolderId = new HiddenField<String>("moveFolderDialog_destFolderId", new Model<String>(""));
        final Form moveFolderForm = new JsonDataResponseForm("moveFolderForm", new AttemptedUpdateAny(EntityType.ESM_ENTERPRISE_FOLDER)){
            @Override
            protected Object getJsonResponseData() {
                String entityId = moveFolderDialogEntityId.getConvertedInput();
                String destFolderId = moveFolderDialogDestFolderId.getConvertedInput();
                try {
                    logger.fine("Moving folder (GUID = "+ entityId + ") into folder with GUID = " + destFolderId + ".");
                    enterpriseFolderManager.moveByGuid(entityId, destFolderId);
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        moveFolderForm.add(moveFolderDialogEntityId);
        moveFolderForm.add(moveFolderDialogDestFolderId);

        final HiddenField<String> deleteFolderDialogInputId = new HiddenField<String>("deleteFolderDialog_id", new Model<String>(""));
        Form deleteFolderForm = new JsonDataResponseForm("deleteFolderForm", new AttemptedDeleteAll( EntityType.ESM_ENTERPRISE_FOLDER )){
            @Override
            protected Object getJsonResponseData() {
                try {
                    String deletedFolderGuid = deleteFolderDialogInputId.getConvertedInput();
                    logger.fine("Deleting folder (GUID = "+ deletedFolderGuid + ").");

                    enterpriseFolderManager.deleteByGuid(deletedFolderGuid);
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    if (ExceptionUtils.causedBy(e, NonEmptyFolderDeletionException.class)) {
                        return new JSONException(e.getMessage());
                    }
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        deleteFolderForm.add(deleteFolderDialogInputId );

        final HiddenField<String> addSSGClusterDialogInputParentId = new HiddenField<String>("addSSGClusterDialog_parentId", new Model<String>(""));
        final RequiredTextField<String> addSSGClusterInputName = new RequiredTextField<String>("addSSGClusterDialog_name", new Model<String>(""));
        final RequiredTextField<String> addSSGClusterInputHostName = new RequiredTextField<String>("addSSGClusterDialog_hostName", new Model<String>(""));
        final RequiredTextField<String> addSSGClusterInputPort = new RequiredTextField<String>("addSSGClusterDialog_port", new Model<String>(""));
        Form addSSGClusterForm = new JsonDataResponseForm("addSSGClusterForm", new AttemptedCreate( EntityType.ESM_SSG_CLUSTER )){
            @Override
            protected Object getJsonResponseData() {
                String newClusterName = addSSGClusterInputName.getConvertedInput();
                String parentFolderGuid = addSSGClusterDialogInputParentId.getConvertedInput();
                String hostname = addSSGClusterInputHostName.getConvertedInput();
                try {
                    int port = Integer.parseInt(addSSGClusterInputPort.getConvertedInput());
                    logger.fine("Adding Gateway Cluster \""+ newClusterName +
                        "\" (parent folder GUID = "+ parentFolderGuid + ").");
                    return ssgClusterManager.create(newClusterName, hostname, port, parentFolderGuid);
                } catch (Exception e) {
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            EnterpriseFolder parentFolder = enterpriseFolderManager.findByGuid(parentFolderGuid);
                            return new JSONException("A cluster with the name '" + newClusterName + "' already exists in the folder '"
                                    + parentFolder.getName() + "'. Please specify a different name.");
                        } catch (FindException e1) {
                            return new JSONException("A cluster with the same name '" + newClusterName + "' already exists. Please specify a different name.");
                        }
                    } else if (ExceptionUtils.causedBy(e, DuplicateHostnameException.class)) {
                        return new JSONException("A Gateway cluster/node with the host name '" + hostname
                                + "' already exists in the enterprise tree. Please specify a different host name.");
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

        final HiddenField<String> editSSGClusterDialogInputId = new HiddenField<String>("editSSGClusterDialog_id", new Model<String>(""));
        final RequiredTextField<String> editSSGClusterInputName = new RequiredTextField<String>("editSSGClusterDialog_name", new Model<String>(""));
        final TextField<String> editSSGClusterInputSslHostname = new TextField<String>("editSSGClusterDialog_hostName", new Model<String>(""));
        final TextField<String> editSSGClusterInputAdminPort = new TextField<String>("editSSGClusterDialog_adminPort", new Model<String>(""));
        Form editSSGClusterForm = new JsonDataResponseForm("editSSGClusterForm", new AttemptedUpdateAny( EntityType.ESM_SSG_CLUSTER )){
            @Override
            protected Object getJsonResponseData() {
                String editedSSGClusterGuid = editSSGClusterDialogInputId.getConvertedInput();
                String newClusterName = editSSGClusterInputName.getConvertedInput();
                String newSslHostname = editSSGClusterInputSslHostname.getConvertedInput();
                String newAdminPort = editSSGClusterInputAdminPort.getConvertedInput();

                try {
                    logger.fine("Editing Gateway Cluster (GUID = "+ editedSSGClusterGuid + ") by changing the name, the ssl hostname, or the admin port.");

                    ssgClusterManager.editByGuid(editedSSGClusterGuid, newClusterName, newSslHostname, newAdminPort);
                    return null;    // No response object expected if successful.
                } catch (Exception e) { // e could be FindException, UpdateException, DuplicateHostnameException, or DuplicateObjectException.
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            SsgCluster editedCluster = ssgClusterManager.findByGuid(editedSSGClusterGuid);
                            EnterpriseFolder parentFolder = editedCluster.getParentFolder();
                            return new JSONException("A cluster with the name '" + newClusterName + "' already exists in the folder '"
                                    + parentFolder.getName() + "'. Please specify a different name.");
                        } catch (FindException e1) {
                            return new JSONException("A cluster with the name '" + newClusterName + "' already exists. Please specify a different name.");
                        }
                    } else if (ExceptionUtils.causedBy(e, DuplicateHostnameException.class)) {
                        return new JSONException("A cluster with the hostname (" + newSslHostname
                                + ") already exists in the enterprise tree. Please specify a different hostname.");
                    } else {
                        return new JSONException(e);
                    }
                }
            }
        };
        editSSGClusterForm.add(editSSGClusterDialogInputId);
        editSSGClusterForm.add(editSSGClusterInputName);
        editSSGClusterForm.add(editSSGClusterInputSslHostname);
        editSSGClusterForm.add(editSSGClusterInputAdminPort);

        final HiddenField<String> moveSSGClusterDialogEntityId = new HiddenField<String>("moveSSGClusterDialog_entityId", new Model<String>(""));
        final HiddenField<String> moveSSGClusterDialogDestFolderId = new HiddenField<String>("moveSSGClusterDialog_destFolderId", new Model<String>(""));
        final Form moveSSGClusterForm = new JsonDataResponseForm("moveSSGClusterForm", new AttemptedUpdateAny(EntityType.ESM_ENTERPRISE_FOLDER)){
            @Override
            protected Object getJsonResponseData() {
                String entityId = moveSSGClusterDialogEntityId.getConvertedInput();
                String destSSGClusterId = moveSSGClusterDialogDestFolderId.getConvertedInput();
                try {
                    logger.fine("Moving Gateway Cluster (GUID = "+ entityId + ") into folder with GUID = " + destSSGClusterId + ".");
                    ssgClusterManager.moveByGuid(entityId, destSSGClusterId);
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        moveSSGClusterForm.add(moveSSGClusterDialogEntityId);
        moveSSGClusterForm.add(moveSSGClusterDialogDestFolderId);

        final HiddenField<String> deleteSSGClusterDialogInputId = new HiddenField<String>("deleteSSGClusterDialog_id", new Model<String>(""));
        Form deleteSSGClusterForm = new JsonDataResponseForm("deleteSSGClusterForm", new AttemptedDeleteAll( EntityType.ESM_SSG_CLUSTER )){
            @Override
            protected Object getJsonResponseData() {
                try {
                    String guid = deleteSSGClusterDialogInputId.getConvertedInput();
                    logger.fine("Deleting Gateway Cluster (GUID = "+ guid + ").");

                    // Delete all monitoring property setups of the SSG cluster and all its SSG nodes.
                    entityMonitoringPropertySetupManager.deleteBySsgClusterGuid(guid);

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

        final HiddenField<String> reconfirmSSGClusterDeletionDialogInputId = new HiddenField<String>("reconfirmSSGClusterDeletionDialog_id", new Model<String>(""));
        Form reconfirmSSGClusterDeletionForm = new JsonDataResponseForm("reconfirmSSGClusterDeletionForm", new AttemptedDeleteAll( EntityType.ESM_SSG_CLUSTER )){
            @Override
            protected Object getJsonResponseData() {
                try {
                    String guid = reconfirmSSGClusterDeletionDialogInputId.getConvertedInput();
                    SsgCluster ssgCluster = ssgClusterManager.findByGuid(guid);

                    logger.fine("Deleting Gateway Cluster (GUID = "+ guid + ") and other related information such as Standard Reports and Migration Records.");
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

        final HiddenField<String> startSsgNodeInputId = new HiddenField<String>("startSsgNode_id", new Model<String>(""));
        Form startSsgNodeForm = new JsonDataResponseForm("startSsgNodeForm") {
            @Override
            protected Object getJsonResponseData() {
                String ssgNodeGuid = startSsgNodeInputId.getConvertedInput();
                try {
                    logger.fine("Starting Gateway Node (GUID = " + ssgNodeGuid + ").");
                    
                    SsgNode node = ssgNodeManager.findByGuid(ssgNodeGuid);
                    SsgCluster cluster = node.getSsgCluster();
                    if ( securityManager.hasPermission( new AttemptedReadSpecific( EntityType.ESM_SSG_CLUSTER, cluster.getId() ) ) ) {
                        ProcessControllerContext pc = gatewayContextFactory.createProcessControllerContext(node);
                        NodeManagementApi nodeManagementApi = pc.getManagementApi();

                        // Start the node
                        nodeManagementApi.startNode("default");

                        // Todo: the below updating node is for demo only.  We will remove the part later on, since GatewayPoller will update node status periodically.
                        // Update the node online status
                        node.setOnlineStatus("on");
                        publisher.publishEvent( new GatewayRegistrationEvent(this) );
                    }

                    // Return the response to the client
                    return node;
                } catch (NodeManagementApi.StartupException e) {
                    return new JSONException(e.getMessage());
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        startSsgNodeForm.add(startSsgNodeInputId);

        final HiddenField<String> stopSsgNodeInputId = new HiddenField<String>("stopSsgNode_id", new Model<String>(""));
        Form stopSsgNodeForm = new JsonDataResponseForm("stopSsgNodeForm") {
            @Override
            protected Object getJsonResponseData() {
                String ssgNodeGuid = stopSsgNodeInputId.getConvertedInput();
                try {
                    logger.fine("Stoping Gateway Node (GUID = " + ssgNodeGuid + ").");

                    SsgNode node = ssgNodeManager.findByGuid(ssgNodeGuid);
                    SsgCluster cluster = node.getSsgCluster();
                    if ( securityManager.hasPermission( new AttemptedReadSpecific( EntityType.ESM_SSG_CLUSTER, cluster.getId() ) ) ) {
                        ProcessControllerContext pc = gatewayContextFactory.createProcessControllerContext(node);
                        NodeManagementApi nodeManagementApi = pc.getManagementApi();

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
        add(editFolderForm);
        add(moveFolderForm);
        add(deleteFolderForm);
        add(addSSGClusterForm);
        add(editSSGClusterForm);
        add(moveSSGClusterForm);
        add(deleteSSGClusterForm);
        add(reconfirmSSGClusterDeletionForm);
        add(getGatewayTrustServletInputsForm);
        add(startSsgNodeForm);
        add(stopSsgNodeForm);
        add(interaction);
    }
}
