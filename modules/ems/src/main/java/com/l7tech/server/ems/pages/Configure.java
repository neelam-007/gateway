package com.l7tech.server.ems.pages;

import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.gateway.GatewayTrustTokenFactory;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.mortbay.util.ajax.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 *
 */
@SuppressWarnings({"UnusedDeclaration"})
@NavigationPage(page="Configure",pageIndex=100,section="ManageGateways",sectionPage="Configure",pageUrl="Configure.html")
public class Configure extends EmsPage  {

    private static final Logger logger = Logger.getLogger(Configure.class.getName());

    @SpringBean(name="serverConfig")
    private Config config;

    @SpringBean
    GatewayTrustTokenFactory gatewayTrustTokenFactory;

    @SpringBean
    private EnterpriseFolderManager enterpriseFolderManager;

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    public Configure() {
        JsonInteraction interaction = new JsonInteraction("actiondiv", "jsonUrl", new JsonDataProvider(){
            @Override
            public Object getData() {
                try {
                    final List<JSON.Convertible> entities = new ArrayList<JSON.Convertible>();
                    EnterpriseFolder rootFolder = enterpriseFolderManager.findRootFolder();
                    entities.add(rootFolder);
                    addChildren(entities, rootFolder);
                    return entities;
                } catch (FindException e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }

            private void addChildren(final List<JSON.Convertible> nodes, final EnterpriseFolder folder) throws FindException {
                // Display order is alphabetical on name, with folders before clusters.
                for (EnterpriseFolder childFolder : enterpriseFolderManager.findChildFolders(folder)) {
                    nodes.add(childFolder);
                    addChildren(nodes, childFolder);
                }

                for (SsgCluster childCluster : ssgClusterManager.findChildSsgClusters(folder)) {
                    nodes.add(childCluster);

                    // Add SSG nodes
                    for (SsgNode node: new TreeSet<SsgNode>(childCluster.getNodes())) {
                        nodes.add(node);
                    }
                }
            }
        });

        final HiddenField addFolderDialogInputParentId = new HiddenField("addFolderDialog_parentId", new Model(""));
        final RequiredTextField addFolderInputName = new RequiredTextField("addFolderDialog_name", new Model(""));
        Form addFolderForm = new JsonDataResponseForm("addFolderForm"){
            @Override
            protected Object getJsonResponseData() {
                String newFolderName = addFolderInputName.getModelObjectAsString();
                String parentFolderGuid = addFolderDialogInputParentId.getModelObjectAsString();
                try {
                    logger.info("Adding folder \"" + newFolderName + "\" (parent folder GUID = " + parentFolderGuid + ").");
                    //noinspection UnnecessaryLocalVariable
                    final EnterpriseFolder newFolder = enterpriseFolderManager.create(newFolderName, parentFolderGuid);
                    return newFolder;
                } catch (Exception e) {
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            EnterpriseFolder parentFolder = enterpriseFolderManager.findByGuid(parentFolderGuid);
                            String errorMsg = "A child folder must be unique in a parent folder. The folder '"
                                + newFolderName + "' has already existed in the folder '" + parentFolder.getName() + "'.";
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
        Form renameFolderForm = new JsonDataResponseForm("renameFolderForm"){
            @Override
            protected Object getJsonResponseData() {
                String renamedFolderGuid = renameFolderDialogInputId.getModelObjectAsString();
                String newFolderName = renameFolderInputName.getModelObjectAsString();
                try {
                    logger.info("Renaming folder (GUID = "+ renamedFolderGuid + ") with a new name, " + newFolderName);

                    enterpriseFolderManager.renameByGuid(newFolderName, renamedFolderGuid);
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            EnterpriseFolder renamedFolder = enterpriseFolderManager.findByGuid(renamedFolderGuid);
                            EnterpriseFolder parentFolder = renamedFolder.getParentFolder();
                            String errorMsg = "A child folder must be unique in a parent folder. The folder '"
                                + newFolderName + "' has already existed in the folder '" + parentFolder.getName() + "'.";
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
        Form deleteFolderForm = new JsonDataResponseForm("deleteFolderForm"){
            @Override
            protected Object getJsonResponseData() {
                try {
                    String deletedFolderGuid = deleteFolderDialogInputId.getModelObjectAsString();
                    logger.info("Deleting folder (GUID = "+ deletedFolderGuid + ").");

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
        Form addSSGClusterForm = new JsonDataResponseForm("addSSGClusterForm"){
            @Override
            protected Object getJsonResponseData() {
                String newClusterName = addSSGClusterInputName.getModelObjectAsString();
                String parentFolderGuid = addSSGClusterDialogInputParentId.getModelObjectAsString();
                try {
                    logger.info("Adding SSG Cluster \""+ addSSGClusterInputName.getModelObjectAsString() +
                        "\" (parent folder GUID = "+ addSSGClusterDialogInputParentId.getModelObjectAsString() + ").");
                    //noinspection UnnecessaryLocalVariable
                    final SsgCluster newCluster = ssgClusterManager.create(
                            newClusterName,
                            addSSGClusterInputHostName.getModelObjectAsString(),
                            Integer.parseInt(addSSGClusterInputPort.getModelObjectAsString()),
                            parentFolderGuid);
                    return newCluster;
                } catch (Exception e) {
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            EnterpriseFolder parentFolder = enterpriseFolderManager.findByGuid(parentFolderGuid);
                            String errorMsg = "A cluster must be unique in a folder. The cluster '"
                                + newClusterName + "' has already existed in the folder '" + parentFolder.getName() + "'.";
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
        addSSGClusterForm.add(addSSGClusterDialogInputParentId);
        addSSGClusterForm.add(addSSGClusterInputName);
        addSSGClusterForm.add(addSSGClusterInputHostName);
        addSSGClusterForm.add(addSSGClusterInputPort);

        final HiddenField renameSSGClusterDialogInputId = new HiddenField("renameSSGClusterDialog_id", new Model(""));
        final RequiredTextField renameSSGClusterInputName = new RequiredTextField("renameSSGClusterDialog_name", new Model(""));
        Form renameSSGClusterForm = new JsonDataResponseForm("renameSSGClusterForm"){
            @Override
            protected Object getJsonResponseData() {
                String renamedSSGClusterGuid = renameSSGClusterDialogInputId.getModelObjectAsString();
                String newClusterName = renameSSGClusterInputName.getModelObjectAsString();
                try {
                    logger.info("Renaming SSG Cluster (GUID = "+ renamedSSGClusterGuid + ") with a new name, " + newClusterName);

                    ssgClusterManager.renameByGuid(newClusterName, renamedSSGClusterGuid);
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    logger.warning(e.toString());
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        try {
                            SsgCluster renamedCluster = ssgClusterManager.findByGuid(renamedSSGClusterGuid);
                            EnterpriseFolder parentFolder = renamedCluster.getParentFolder();
                            String errorMsg = "A cluster must be unique in a folder. The cluster '"
                                + newClusterName + "' has already existed in the folder '" + parentFolder.getName() + "'.";
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
        Form deleteSSGClusterForm = new JsonDataResponseForm("deleteSSGClusterForm"){
            @Override
            protected Object getJsonResponseData() {
                try {
                    String guid = deleteSSGClusterDialogInputId.getModelObjectAsString();
                    logger.info("Deleting SSG Cluster (GUID = "+ guid + ").");

                    ssgClusterManager.deleteByGuid(guid);
                    return null;    // No response object expected if successful.
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        deleteSSGClusterForm.add(deleteSSGClusterDialogInputId );

        final Form getGatewayTrustServletInputsForm = new JsonDataResponseForm("getGatewayTrustServletInputsForm"){
            @Override
            protected Object getJsonResponseData() {
                try {
                    logger.info("Responding to request for trust token.");
                    final String token = gatewayTrustTokenFactory.getTrustToken();
                    return new JSON.Convertible() {
                        @Override
                        public void toJSON(JSON.Output output) {
                            output.add("token", token);
                        }

                        @Override
                        public void fromJSON(Map map) {
                            throw new UnsupportedOperationException("Mapping from JSON not supported.");
                        }
                    };
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        deleteSSGClusterForm.add(deleteSSGClusterDialogInputId );

        add(addFolderForm);
        add(renameFolderForm);
        add(deleteFolderForm);
        add(addSSGClusterForm);
        add(renameSSGClusterForm);
        add(deleteSSGClusterForm);
        add(getGatewayTrustServletInputsForm);
        add(interaction);
    }
}