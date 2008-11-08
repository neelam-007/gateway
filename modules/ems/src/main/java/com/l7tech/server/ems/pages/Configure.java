package com.l7tech.server.ems.pages;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.enterprise.*;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.mortbay.util.ajax.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
@SuppressWarnings({"UnusedDeclaration"})
@NavigationPage(page="Configure",pageIndex=100,section="ManageGateways",pageUrl="Configure.html")
public class Configure extends EmsPage  {

    private static final Logger logger = Logger.getLogger(Configure.class.getName());
    private static final Object SUCCESS_ACK = new Object();

    @SpringBean
    private EnterpriseFolderManager enterpriseFolderManager;

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    public Configure() {
        JsonInteraction interaction = new JsonInteraction("actiondiv", "jsonUrl", new JsonDataProvider(){
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

                for (SsgCluster childCluster : ssgClusterManager.findChildrenOfFolder(folder)) {
                    nodes.add(childCluster);
                }
            }
        });

        final HiddenField addFolderDialogInputParentId = new HiddenField("addFolderDialog_parentId", new Model(""));
        final RequiredTextField addFolderInputName = new RequiredTextField("addFolderDialog_name", new Model(""));
        Form addFolderForm = new JsonDataResponseForm("addFolderForm"){
            @Override
            protected Object getJsonResponseData() {
                try {
                    String newFolderName = addFolderInputName.getModelObjectAsString();
                    String parentGuid = addFolderDialogInputParentId.getModelObjectAsString();
                    logger.info("Adding folder \"" + newFolderName + "\" (parent folder GUID = " + parentGuid + ").");

                    enterpriseFolderManager.create(newFolderName, parentGuid);
                    return SUCCESS_ACK;
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        addFolderForm.add(addFolderDialogInputParentId);
        addFolderForm.add(addFolderInputName);

        final HiddenField deleteFolderDialogInputId = new HiddenField("deleteFolderDialog_id", new Model(""));
        Form deleteFolderForm = new JsonDataResponseForm("deleteFolderForm"){
            @Override
            protected Object getJsonResponseData() {
                try {
                    String deletedFolderGuid = deleteFolderDialogInputId.getModelObjectAsString();
                    logger.info("Deleting folder (GUID = "+ deletedFolderGuid + ").");

                    enterpriseFolderManager.deleteByGuid(deletedFolderGuid);
                    return SUCCESS_ACK;
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
                try {
                    logger.info("Adding SSG Cluster \""+ addSSGClusterInputName.getModelObjectAsString() + "\" (parent folder GUID = "+ addSSGClusterDialogInputParentId.getModelObjectAsString() + ").");
                    ssgClusterManager.create(
                            addSSGClusterInputName.getModelObjectAsString(),
                            addSSGClusterInputHostName.getModelObjectAsString(),
                            Integer.parseInt(addSSGClusterInputPort.getModelObjectAsString()),
                            addSSGClusterDialogInputParentId.getModelObjectAsString());
                    return SUCCESS_ACK;
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };
        addSSGClusterForm.add(addSSGClusterDialogInputParentId);
        addSSGClusterForm.add(addSSGClusterInputName);
        addSSGClusterForm.add(addSSGClusterInputHostName);
        addSSGClusterForm.add(addSSGClusterInputPort);

        add(addFolderForm);
        add(deleteFolderForm);
        add(addSSGClusterForm);
        add(interaction);
    }
}