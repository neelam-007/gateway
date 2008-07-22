package com.l7tech.console.action;

import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.event.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.IdentityHeader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */

public class NewFederatedIdentityProviderAction extends NewProviderAction {
    static final Logger log = Logger.getLogger(NewFederatedIdentityProviderAction.class.getName());

    private FederatedIdentityProviderConfig fipConfig;  //holds the configuration information for federated identity provider
    private HashMap<String, String> userUpdateMap = null;
    private HashMap<String, String> groupUpdateMap = null;

    public NewFederatedIdentityProviderAction(AbstractTreeNode node) {
        super(node);
        this.fipConfig = null;
    }

    /**
     * Constructor that will accept a pre-config federated identity provider config object.
     *
     * @param node
     * @param fipConfig
     */
    public NewFederatedIdentityProviderAction(AbstractTreeNode node,
                                              FederatedIdentityProviderConfig fipConfig,
                                              HashMap<String, String> userUpdateMap,
                                              HashMap<String, String> groupUpdateMap)
    {
        super(node);
        this.fipConfig = fipConfig;
        this.userUpdateMap = userUpdateMap;
        this.groupUpdateMap = groupUpdateMap;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create Federated Identity Provider";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create a new Federated Identity Provider";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/CreateIdentityProvider16x16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                Frame f = TopComponents.getInstance().getTopParent();

                WizardStepPanel importUsersStepPanel = null;
                if(fipConfig != null && ((fipConfig.getImportedGroups() != null && fipConfig.getImportedGroups().size() > 0) ||
                        (fipConfig.getImportedUsers() != null && fipConfig.getImportedUsers().size() > 0)))
                {
                    importUsersStepPanel = new FederatedIPImportUsersPanel(null);
                }
                FederatedIPGeneralPanel configPanel = new FederatedIPGeneralPanel(new FederatedIPTrustedCertsPanel(new IdentityProviderCertificateValidationConfigPanel(importUsersStepPanel)));
                CreateFederatedIPWizard w = new CreateFederatedIPWizard(f, configPanel, fipConfig); //pass federated identity provider config
                //FederatedIdentityProviderWindow w = new FederatedIdentityProviderWindow(f);

                // register itself to listen to the addEvent
                w.addWizardListener(wizardListener);

                // register itself to listen to the addEvent
                addEntityListener(listener);

                w.pack();
                Utilities.centerOnScreen(w);
                DialogDisplayer.display(w);
            }
        });

    }

    private WizardListener wizardListener = new WizardAdapter() {
        @Override
        public void wizardCanceled(WizardEvent e) {
            removeEntityListener(listener);
        }

        /**
         * Invoked when the wizard has finished.
         *
         * @param we the event describing the wizard finish
         */
        @Override
        public void wizardFinished(WizardEvent we) {

            // update the provider
            Wizard w = (Wizard)we.getSource();
            final FederatedIdentityProviderConfig iProvider = (FederatedIdentityProviderConfig)w.getWizardInput();

            if (iProvider != null) {

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        EntityHeader header = new EntityHeader();
                        header.setName(iProvider.getName());
                        header.setType(EntityType.ID_PROVIDER_CONFIG);
                        try {
                            header.setOid(getIdentityAdmin().saveIdentityProviderConfig(iProvider));
                            // Refresh permission cache so that newly created IdP is usable
                            Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                        } catch (DuplicateObjectException doe) {
                            String msg = "An Identity Provider with the same name already exists.\nThe new Identity Provider has not been saved.";
                            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                                          msg,
                                                          "Error Saving Identity Provider",
                                                          JOptionPane.WARNING_MESSAGE);
                            header = null;
                        } catch (Exception e) {
                            ErrorManager.getDefault().notify(Level.WARNING, e, "Error saving the new identity provider: " + header.getName());
                            header = null;
                        }

                        if(header != null) {
                            try {
                                HashMap<String, FederatedUser> userOidMap = new HashMap<String, FederatedUser>();
                                if(iProvider.getImportedUsers() != null) {
                                    // Import the users
                                    for(FederatedUser user : iProvider.getImportedUsers().values()) {
                                        String oldOid = user.getId();
                                        user.setOid(FederatedUser.DEFAULT_OID);
                                        String newOid = getIdentityAdmin().saveUser(header.getOid(), user, null);
                                        user.setOid(Long.parseLong(newOid));

                                        userOidMap.put(oldOid, user);
                                        if(userUpdateMap != null) {
                                            userUpdateMap.put(oldOid, newOid);
                                        }
                                    }
                                }

                                if(iProvider.getImportedGroups() != null) {
                                    // Import the groups
                                    for(FederatedGroup group : iProvider.getImportedGroups().values()) {
                                        String oldOid = group.getId();
                                        group.setOid(FederatedGroup.DEFAULT_OID);

                                        Set<IdentityHeader> memberHeaders = new HashSet<IdentityHeader>();
                                        if(!(group instanceof VirtualGroup)) {
                                            if(iProvider.getImportedGroupMembership() != null) {
                                                Set<String> members = iProvider.getImportedGroupMembership().get(oldOid);
                                                if(members != null) {
                                                    for(String memberOid : members) {
                                                        if(userOidMap.containsKey(memberOid)) {
                                                            FederatedUser user = userOidMap.get(memberOid);
                                                            memberHeaders.add(new IdentityHeader(header.getOid(), user.getId(), EntityType.USER, user.getName(), null));
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        String newOid = getIdentityAdmin().saveGroup(header.getOid(), group, memberHeaders);
                                        group.setOid(Long.parseLong(newOid));

                                        if(groupUpdateMap != null) {
                                            groupUpdateMap.put(oldOid, newOid);
                                        }
                                    }
                                }
                            } catch(Exception e) {
                                String msg = "An error occurred while importing the users and groups.";
                                JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                                          msg,
                                                          "Error Importing Users/Groups",
                                                          JOptionPane.WARNING_MESSAGE);
                            }

                            fireEventProviderAdded(header);
                        } else {
                            removeEntityListener(listener);
                        }
                    }
                });
            }
        }

    };

    private EntityListener listener = new EntityListenerAdapter() {
        /**
         * Fired when an new entity is added.
         *
         * @param ev event describing the action
         */
        public void entityAdded(final EntityEvent ev) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    EntityHeader eh = (EntityHeader)ev.getEntity();
                    IdentityProvidersTree tree = (IdentityProvidersTree)TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
                    if (tree == null) {
                        log.log(Level.WARNING, "Unable to reach the identity tree.");
                        return;
                    }
                    if (node == null) {
                        node = tree.getRootNode();
                    }
                    if (tree.hasBeenExpanded(new TreePath(node.getPath()))) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

                        final AbstractTreeNode newChildNode = TreeNodeFactory.asTreeNode(eh);
                        model.insertNodeInto(newChildNode, node, node.getInsertPosition(newChildNode));
                        TreeNode[] nodePath = model.getPathToRoot(newChildNode);
                        if (nodePath != null) {
                            tree.setSelectionPath(new TreePath(nodePath));
                        }
                    }
                    removeEntityListener(listener);
                }
            });
        }
    };

    public void onLogoff(LogonEvent e) {
        super.onLogoff(e);
        node = null;
    }
}
