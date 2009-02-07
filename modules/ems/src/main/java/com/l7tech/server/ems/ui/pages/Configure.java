package com.l7tech.server.ems.ui.pages;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayRegistrationEvent;
import com.l7tech.server.ems.gateway.GatewayTrustTokenFactory;
import com.l7tech.server.ems.migration.MigrationRecordManager;
import com.l7tech.server.ems.standardreports.StandardReportManager;
import com.l7tech.server.ems.ui.EsmSecurityManager;
import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
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

        final HiddenField editFolderDialogInputId = new HiddenField("editFolderDialog_id", new Model(""));
        final RequiredTextField editFolderInputName = new RequiredTextField("editFolderDialog_name", new Model(""));
        Form editFolderForm = new JsonDataResponseForm("editFolderForm", new AttemptedUpdateAny( EntityType.ESM_ENTERPRISE_FOLDER )){
            @Override
            protected Object getJsonResponseData() {
                String editedFolderGuid = (String)editFolderDialogInputId.getConvertedInput();
                String newFolderName = (String)editFolderInputName.getConvertedInput();
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
        editFolderForm.add(editFolderDialogInputId);
        editFolderForm.add(editFolderInputName);

        final HiddenField moveFolderDialogEntityId = new HiddenField("moveFolderDialog_entityId", new Model(""));
        final HiddenField moveFolderDialogDestFolderId = new HiddenField("moveFolderDialog_destFolderId", new Model(""));
        final Form moveFolderForm = new JsonDataResponseForm("moveFolderForm", new AttemptedUpdateAny(EntityType.ESM_ENTERPRISE_FOLDER)){
            @Override
            protected Object getJsonResponseData() {
                String entityId = (String)moveFolderDialogEntityId.getConvertedInput();
                String destFolderId = (String)moveFolderDialogDestFolderId.getConvertedInput();
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

        final HiddenField editSSGClusterDialogInputId = new HiddenField("editSSGClusterDialog_id", new Model(""));
        final RequiredTextField editSSGClusterInputName = new RequiredTextField("editSSGClusterDialog_name", new Model(""));
        final RequiredTextField editSSGClusterInputSslHostname = new RequiredTextField("editSSGClusterDialog_sslHostname", new Model(""));
        final RequiredTextField editSSGClusterInputAdminPort = new RequiredTextField("editSSGClusterDialog_adminPort", new Model(""));
        Form editSSGClusterForm = new JsonDataResponseForm("editSSGClusterForm", new AttemptedUpdateAny( EntityType.ESM_SSG_CLUSTER )){
            @Override
            protected Object getJsonResponseData() {
                String editedSSGClusterGuid = (String)editSSGClusterDialogInputId.getConvertedInput();
                String newClusterName = (String)editSSGClusterInputName.getConvertedInput();
                String newSslHostname = (String)editSSGClusterInputSslHostname.getConvertedInput();
                String newAdminPort = (String)editSSGClusterInputAdminPort.getConvertedInput();

                try {
                    logger.fine("Editing SSG Cluster (GUID = "+ editedSSGClusterGuid + ") by changing the name, the ssl hostname, or the admin port.");

                    ssgClusterManager.editByGuid(editedSSGClusterGuid, newClusterName, newSslHostname, newAdminPort);
                    return null;    // No response object expected if successful.
                } catch (Exception e) { // e could be FindException, UpdateException, DuplicateHostnameException, or DuplicateObjectException.
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            SsgCluster editedCluster = ssgClusterManager.findByGuid(editedSSGClusterGuid);
                            EnterpriseFolder parentFolder = editedCluster.getParentFolder();
                            String errorMsg = "A cluster with the name '" + newClusterName + "' already exists in the folder '"
                                + parentFolder.getName() + "'.<br/>Please specify a different name.";
                            return new JSONException(new DuplicateObjectException(errorMsg, e));
                        } catch (FindException e1) {
                            return new JSONException(e1);
                        }
                    } else if (ExceptionUtils.causedBy(e, DuplicateHostnameException.class)) {
                        String errorMsg = "A cluster with the hostname (" + newSslHostname + ") already exists in the enterprise tree." +
                            "<br/>Please specify a different hostname.";
                        return new JSONException(new DuplicateObjectException(errorMsg, e));
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

        final HiddenField moveSSGClusterDialogEntityId = new HiddenField("moveSSGClusterDialog_entityId", new Model(""));
        final HiddenField moveSSGClusterDialogDestFolderId = new HiddenField("moveSSGClusterDialog_destFolderId", new Model(""));
        final Form moveSSGClusterForm = new JsonDataResponseForm("moveSSGClusterForm", new AttemptedUpdateAny(EntityType.ESM_ENTERPRISE_FOLDER)){
            @Override
            protected Object getJsonResponseData() {
                String entityId = (String)moveSSGClusterDialogEntityId.getConvertedInput();
                String destSSGClusterId = (String)moveSSGClusterDialogDestFolderId.getConvertedInput();
                try {
                    logger.fine("Moving SSG Cluster (GUID = "+ entityId + ") into folder with GUID = " + destSSGClusterId + ".");
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
