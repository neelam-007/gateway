package com.l7tech.console.action;

import com.l7tech.console.event.*;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.ProviderNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultTreeModel;
import java.util.*;
import java.util.logging.Level;
import java.awt.*;

/**
 * The <code>IdentityProviderPropertiesAction</code> edits the
 * identity provider.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class IdentityProviderPropertiesAction extends NodeAction {
    private EventListenerList listenerList = new EventListenerList();

    public IdentityProviderPropertiesAction(ProviderNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Provider properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/edit provider properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(
          new Runnable() {
            public void run() {

                JFrame f = TopComponents.getInstance().getMainWindow();
                EntityHeader header = ((EntityHeaderNode) node).getEntityHeader();
                IdentityProviderConfig iProvider = null;
                if (header.getOid() != -1) {

                    try {
                        iProvider =
                                getProviderConfigManager().findByPrimaryKey(header.getOid());

                        WizardStepPanel configPanel = null;

                        if (iProvider.type() == IdentityProviderType.INTERNAL) {
                            configPanel = new InternalIdentityProviderConfigPanel(null, true);

                        } else if (iProvider.type() == IdentityProviderType.LDAP) {

                            configPanel = new LdapIdentityProviderConfigPanel(
                                              new LdapGroupMappingPanel(
                                                  new LdapUserMappingPanel(null)
                                            ), false);
                        }

                        Wizard w = new EditIdentityProviderWizard(f, configPanel, iProvider);
                        w.addWizardListener(wizardListener);

                        // register itself to listen to the updateEvent
                        addEntityListener(entityListener);

                        w.pack();
                        w.setSize(780, 560);
                        Utilities.centerOnScreen(w);
                        w.setVisible(true);

                    } catch (Exception e1) {
                        ErrorManager.getDefault().
                                notify(Level.WARNING, e1, "Error retrieving provider.");
                    }
                }
            }
        });
    }

    /**
     * notfy the listeners that the entity has been updated
     * @param header
     */
    private void fireEventProviderUpdated(EntityHeader header) {
        EntityEvent event = new EntityEvent(this, header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener)listeners[i]).entityUpdated(event);
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
          (IdentityProviderConfigManager)Locator.
          getDefault().lookup(IdentityProviderConfigManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }

        return ipc;
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

            if (iProvider != null && iProvider.type() != IdentityProviderType.INTERNAL) {

                SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                EntityHeader header = ((EntityHeaderNode) node).getEntityHeader();
                                header.setName(iProvider.getName());
                                header.setType(EntityType.ID_PROVIDER_CONFIG);
                                try {
                                    getProviderConfigManager().update(iProvider);
                                    fireEventProviderUpdated(header);

                                } catch (UpdateException e) {
                                    ErrorManager.getDefault().notify(Level.WARNING, e, "Error updating the identity provider.");
                                }
                            }
                        });
            }
        }

    };

    EntityListener entityListener = new EntityListenerAdapter() {
        public void entityUpdated(EntityEvent ev) {
            if (tree == null) {
                log.warning("Internal: tree has not been set.");
                return;
            }

            try {
                tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                Enumeration enumeration = node.children();

                while (enumeration.hasMoreElements()) {
                    AbstractTreeNode n = (AbstractTreeNode)enumeration.nextElement();
                    n.setHasLoadedChildren(false);
                    model.nodeStructureChanged(n);
                }
            } finally {
                tree.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    };
}
