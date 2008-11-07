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
        Form addFolderForm = new Form("addFolderForm"){
            @Override
            protected void onSubmit() {
                logger.info("Adding folder \""+ addFolderInputName.getModelObjectAsString() + "\" (parent folder GUID = "+ addFolderDialogInputParentId.getModelObjectAsString() + ").");
                try {
                    enterpriseFolderManager.create(addFolderInputName.getModelObjectAsString(), addFolderDialogInputParentId.getModelObjectAsString());
                } catch (Exception e) {
                    // TODO send back exception in JSON notaion
                    logger.warning(e.toString());
                }
            }
        };
        addFolderForm.add(addFolderDialogInputParentId);
        addFolderForm.add(addFolderInputName);

        final HiddenField deleteFolderDialogInputId = new HiddenField("deleteFolderDialog_id", new Model(""));
        Form deleteFolderForm = new Form("deleteFolderForm"){
            @Override
            protected void onSubmit() {
                logger.info("Deleting folder (GUID = "+ deleteFolderDialogInputId.getModelObjectAsString() + ").");
                try {
                    enterpriseFolderManager.deleteByGuid(deleteFolderDialogInputId.getModelObjectAsString());
                } catch (Exception e) {
                    // TODO send back exception in JSON notaion
                    logger.warning(e.toString());
                }
            }
        };
        deleteFolderForm.add(deleteFolderDialogInputId );

        final HiddenField addSSGClusterDialogInputParentId = new HiddenField("addSSGClusterDialog_parentId", new Model(""));
        final RequiredTextField addSSGClusterInputName = new RequiredTextField("addSSGClusterDialog_name", new Model(""));
        final RequiredTextField addSSGClusterInputHostName = new RequiredTextField("addSSGClusterDialog_hostName", new Model(""));
        final RequiredTextField addSSGClusterInputPort = new RequiredTextField("addSSGClusterDialog_port", new Model(""));
        Form addSSGClusterForm = new Form("addSSGClusterForm"){
            @Override
            protected void onSubmit() {
                logger.info("Adding SSG Cluster \""+ addSSGClusterInputName.getModelObjectAsString() + "\" (parent folder GUID = "+ addSSGClusterDialogInputParentId.getModelObjectAsString() + ").");
                try {
                    ssgClusterManager.create(
                            addSSGClusterInputName.getModelObjectAsString(),
                            addSSGClusterInputHostName.getModelObjectAsString(),
                            Integer.parseInt(addSSGClusterInputPort.getModelObjectAsString()),
                            addSSGClusterDialogInputParentId.getModelObjectAsString());
                } catch (Exception e) {
                    // TODO send back exception in JSON notaion
                    e.printStackTrace();
                    logger.warning(e.toString());
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