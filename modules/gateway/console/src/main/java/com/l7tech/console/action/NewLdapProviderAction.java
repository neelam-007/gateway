package com.l7tech.console.action;


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
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The <code>NewLdapProviderAction</code> action adds the new provider.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class NewLdapProviderAction extends NewProviderAction {
    static final Logger log = Logger.getLogger(NewLdapProviderAction.class.getName());

    private LdapIdentityProviderConfig ldapConfig;

    public NewLdapProviderAction() {
        super(null);
    }
    
    public NewLdapProviderAction(AbstractTreeNode node) {
        super(node);
    }

    public NewLdapProviderAction(LdapIdentityProviderConfig ldapConfig) {
        super(null);
        this.ldapConfig = ldapConfig;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Create LDAP Identity Provider";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "Create a new LDAP Identity Provider";
    }

    /**
     * specify the resource name for this action
     */
    @Override
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
    @Override
    protected void performAction() {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                LdapIdentityProviderConfigPanel configPanel = (
                  new LdapIdentityProviderConfigPanel(new LdapGroupMappingPanel(new LdapUserMappingPanel(new LdapAdvancedConfigurationPanel(new LdapCertificateSettingsPanel(null)))), true));


                Frame f = TopComponents.getInstance().getTopParent();
                Wizard w = new CreateIdentityProviderWizard(f, configPanel, ldapConfig);
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
            final IdentityProviderConfig iProvider = (IdentityProviderConfig)w.getWizardInput();

            if (iProvider != null) {

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
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
                        if (header == null) {
                            removeEntityListener(listener);
                        } else {
                            fireEventProviderAdded(header);
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
        @Override
        public void entityAdded(final EntityEvent ev) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
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

                        final AbstractTreeNode newChildNode = TreeNodeFactory.asTreeNode(eh, null);
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
}
