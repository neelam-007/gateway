package com.l7tech.console.action;


import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.Locator;
import com.l7tech.console.event.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ProviderNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SaveException;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.EventListener;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The <code>NewProviderAction</code> action adds the new provider.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class NewProviderAction extends NodeAction {
    static final Logger log = Logger.getLogger(NewUserAction.class.getName());
    private EventListenerList listenerList = new EventListenerList();

    public NewProviderAction(AbstractTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create Identity Provider";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create a new External Identity Provider";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/providers16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                LdapIdentityProviderConfigPanel configPanel = (
                            new LdapIdentityProviderConfigPanel (
                                new LdapGroupMappingPanel (
                                    new LdapUserMappingPanel(null)
                ), true));


                JFrame f = TopComponents.getInstance().getMainWindow();
                Wizard w = new CreateIdentityProviderWizard(f, configPanel);
                w.addWizardListener(wizardListener);

                // register itself to listen to the addEvent
                addEntityListener(listener);

                w.pack();
                w.setSize(780, 560);
                Utilities.centerOnScreen(w);
                w.setVisible(true);

            }
        });

    }

    private WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard has finished.
         *
         * @param we the event describing the wizard finish
         */
        public void wizardFinished(WizardEvent we) {

            // update the provider
            Wizard w = (Wizard) we.getSource();
            final IdentityProviderConfig iProvider = (IdentityProviderConfig) w.getCollectedInformation();

            if (iProvider != null) {

                SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                EntityHeader header = new EntityHeader();
                                header.setName(iProvider.getName());
                                header.setType(EntityType.ID_PROVIDER_CONFIG);
                                try {
                                    header.setOid(getProviderConfigManager().save(iProvider));
                                    fireEventProviderAdded(header);

                                } catch (SaveException e) {
                                    ErrorManager.getDefault().notify(Level.WARNING, e, "Error saving the new identity provider: " + header.getName());
                                }
                            }
                        });
            }
        }

    };

    /**
     * notfy the listeners that the entity has been added
     * @param header
     */
    private void fireEventProviderAdded(EntityHeader header) {
        EntityEvent event = new EntityEvent(this, header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener) listeners[i]).entityAdded(event);
        }
    }

    /**
      * add the EntityListener
      *
      * @param listener the EntityListener
      */
     public void addEntityListener(EntityListener listener) {
         listenerList.add(EntityListener.class, listener);
     }

     /**
      * remove the the EntityListener
      *
      * @param listener the EntityListener
      */
     public void removeEntityListener(EntityListener listener) {
         listenerList.remove(EntityListener.class, listener);
     }


    private IdentityProviderConfigManager getProviderConfigManager()
            throws RuntimeException {
        IdentityProviderConfigManager ipc =
                (IdentityProviderConfigManager) Locator.
                getDefault().lookup(IdentityProviderConfigManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }

        return ipc;
    }

    private EntityListener listener = new EntityListenerAdapter() {
        /**
         * Fired when an new entity is added.
         *
         * @param ev event describing the action
         */
        public void entityAdded(final EntityEvent ev) {
            if (node == null) {
                log.fine("Parent node has not been set - skipping notificaiton.");
                return;
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    EntityHeader eh = (EntityHeader)ev.getEntity();
                    JTree tree = (JTree)TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
                    if (tree == null) {
                        log.log(Level.WARNING, "Unable to reach the identity tree.");
                        return;
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
                }
            });
        }
    };
}
