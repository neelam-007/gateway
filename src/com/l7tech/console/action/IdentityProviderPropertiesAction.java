package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.ProviderNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultTreeModel;
import java.util.EventListener;
import java.util.logging.Level;

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
        return "Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Edit Identity Provider properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
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

                JFrame f = TopComponents.getInstance().getMainWindow();
                EntityHeader header = ((EntityHeaderNode)node).getEntityHeader();
                IdentityProviderConfig iProvider = null;
                if (header.getOid() != -1) {

                    try {
                        iProvider =
                          getIdentityAdmin().findIdentityProviderConfigByPrimaryKey(header.getOid());

                        WizardStepPanel configPanel = null;
                        Wizard w = null;

                        if (iProvider.type() == IdentityProviderType.INTERNAL || iProvider.type() == IdentityProviderType.LDAP) {

                            if (iProvider.type() == IdentityProviderType.LDAP) {
                                configPanel = new LdapIdentityProviderConfigPanel(new LdapGroupMappingPanel(new LdapUserMappingPanel(null)), false);
                            } else {
                                configPanel = new InternalIdentityProviderConfigPanel(null, true);
                            }

                            w = new EditIdentityProviderWizard(f, configPanel, iProvider);

                        } else if (iProvider.type() == IdentityProviderType.FEDERATED) {
                             configPanel = new FederatedIPGeneralPanel(
                                               new FederatedIPSamlPanel(
                                               new FederatedIPTrustedCertsPanel(null)));
                             w = new EditFederatedIPWizard(f, configPanel, (FederatedIdentityProviderConfig) iProvider);


                        } else {
                            throw new RuntimeException("Unsupported Identity Provider Type: " + iProvider.type().toString());
                        }

                        w.addWizardListener(wizardListener);

                        // register itself to listen to the updateEvent
                        addEntityListener(entityListener);

                        w.pack();

                        if (iProvider.type() == IdentityProviderType.FEDERATED) {
                            w.setSize(850, 500);
                        } else {
                            w.setSize(780, 560);
                        }

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
     *
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

    private WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard has finished.
         *
         * @param we the event describing the wizard finish
         */
        public void wizardFinished(WizardEvent we) {

            // update the provider
            Wizard w = (Wizard)we.getSource();
            final IdentityProviderConfig iProvider = (IdentityProviderConfig)w.getCollectedInformation();

            if (iProvider != null  && iProvider.type() != IdentityProviderType.INTERNAL) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {

                        try {
                            getIdentityAdmin().saveIdentityProviderConfig(iProvider);

                            // update the node name in the identity provider tree
                            EntityHeader header = ((EntityHeaderNode)node).getEntityHeader();
                            header.setName(iProvider.getName());
                            header.setType(EntityType.ID_PROVIDER_CONFIG);
                            fireEventProviderUpdated(header);

                        } catch (Exception e) {
                            ErrorManager.getDefault().notify(Level.WARNING, e, "Error updating the identity provider.");
                        }
                    }
                });
            }
        }

    };

    private IdentityAdmin getIdentityAdmin() {
        return Registry.getDefault().getIdentityAdmin();
    }

    EntityListener entityListener = new EntityListenerAdapter() {
        public void entityUpdated(EntityEvent ev) {
            if (tree == null) {
                log.warning("Internal: tree has not been set.");
                return;
            }
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            model.nodeChanged(node);
        }
    };
}
