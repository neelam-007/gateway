package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import org.mortbay.util.ajax.JSON;
import org.apache.wicket.model.Model;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.HiddenField;

import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 */
@SuppressWarnings({"UnusedDeclaration"})
@NavigationPage(page="Configure",pageIndex=100,section="ManageGateways",pageUrl="Configure.html")
public class Configure extends EmsPage  {

    private static final Logger logger = Logger.getLogger(Configure.class.getName());

    public Configure() {
        JsonInteraction interaction = new JsonInteraction("actiondiv", "jsonUrl", new JsonDataProvider(){
            public Object getData() {
                logger.log(Level.INFO, "Providing data.");

                JSONEnterpriseTreeNode[] nodes = new JSONEnterpriseTreeNode[2];
                nodes[0] = new JSONEnterpriseTreeNode();
                nodes[0].id = "3d7fa710-334c-11dd-bd11-0800200c9a66";
                nodes[0].parentId = null;
                nodes[0].type = "enterpriseFolder";
                nodes[0].name = "Layer 7 Technologies"; // from license ?
                nodes[0].accessStatus = true;
                nodes[0].movable = false;

                nodes[1] = new JSONEnterpriseTreeNode();
                nodes[1].id = "40f62d36-4146-4f4b-bdd6-14489526b1a6";
                nodes[1].parentId = "3d7fa710-334c-11dd-bd11-0800200c9a66";
                nodes[1].type = "enterpriseFolder";
                nodes[1].name = "Test Folder 1";
                nodes[1].ancestors = new String[]{"Layer 7 Technologies"};
                nodes[1].accessStatus = true;

                return nodes;
            }
        });

        final HiddenField addFolderDialogInputParentId = new HiddenField("addFolderDialog_parentId", new Model(""));
        final RequiredTextField addFolderInputName = new RequiredTextField("addFolderDialog_name", new Model(""));
        Form addFolderForm = new Form("addFolderForm"){
            @Override
            protected void onSubmit() {
                logger.info("Add folder for name '"+addFolderInputName.getModelObjectAsString()+"', parent '"+addFolderDialogInputParentId.getModelObjectAsString()+"'.");
            }
        };
        addFolderForm.add( addFolderDialogInputParentId );
        addFolderForm.add( addFolderInputName );

        add(addFolderForm);
        add(interaction);
    }

    private static final class JSONEnterpriseTreeNode implements JSON.Convertible {
        private String id;  //always 	 string 	 must be a GUID
        private String parentId;  //always 	string 	must be a GUID; null if this is root
        private String type;  //always 	string 	an l7.EnterpriseTreeTable.ENTITY value
        private String name;  //always 	string 	Note: remember to set HTML lang attribute correctly
        private String[] ancestors = new String[0];  //if having dashboard column 	array of ancestor's names, ordered from topmost to immediate parent
        private boolean movable = true;  //always 	boolean 	true if movable and deletable; default is true
        private String version;  //if entity is an SSG Cluster, an SSG Node, or a service policy 	string
        private String onlineStatus;  //always 	string 	an l7.EnterpriseTreeTable.SSG_CLUSTER_ONLINE_STATE value for an SSG Cluster; an l7.EnterpriseTreeTable.SSG_NODE_ONLINE_STATE for an SSG Node
        private boolean trustStatus;  //always 	boolean 	true if trust has been established
        private boolean accessStatus;  //always 	boolean 	true if access account has been set for an SSG Cluster; true if access role has been granted for an SSG Node
        private String sslHostName;  //if having details column and entity is an SSG Cluster 	string
        private String adminPort;  //if having details column and entity is an SSG Cluster 	string
        private String ipAddress;  //f having details column and entity is an SSG Cluster or SSG Node 	string
        private String[] dbHosts;  //if having details column and entity is an SSG Cluster 	array of strings
        private String selfHostName;  //if having details column and entity is an SSG Node 	string
        //private String monitoredProperties;  //if having monitoring columns and entity is an SSG Cluster or SSG Node 	object literal 	for an SSG Cluster the possible properties are l7.EnterpriseTreeTable.SSG_CLUSTER_MONITORING_PROPERTY values; for an SSG Node the possible properties are l7.EnterpriseTreeTable.SSG_NODE_MONITORING_PROPERTY values; their property values are object literals with 3 properties:

        public String getId() {
            return id;
        }

        public String getParentId() {
            return parentId;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String[] getAncestors() {
            return ancestors;
        }

        public boolean isMovable() {
            return movable;
        }

        public String getVersion() {
            return version;
        }

        public String getOnlineStatus() {
            return onlineStatus;
        }

        public boolean isTrustStatus() {
            return trustStatus;
        }

        public boolean isAccessStatus() {
            return accessStatus;
        }

        public String getSslHostName() {
            return sslHostName;
        }

        public String getAdminPort() {
            return adminPort;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public String[] getDbHosts() {
            return dbHosts;
        }

        public String getSelfHostName() {
            return selfHostName;
        }

        public void toJSON(JSON.Output output) {
            JSONEnterpriseTreeNode node = this;
            output.add( "id", node.getId() );
            output.add( "parentid", node.getParentId() );
            output.add( "type", node.getType());
            output.add( "name", node.getName());
            output.add( "ancestors", node.getAncestors() );
            output.add( "movable", node.isMovable() );
            output.add( "version", node.getVersion() );
            output.add( "onlineStatus", node.getOnlineStatus() );
            output.add( "trustStatus", node.isTrustStatus() );
            output.add( "accessStatus", node.isAccessStatus() );
        }

        public void fromJSON(Map map) {
            throw new UnsupportedOperationException("Mapping fom JSON not supported.");
        }
    }
}