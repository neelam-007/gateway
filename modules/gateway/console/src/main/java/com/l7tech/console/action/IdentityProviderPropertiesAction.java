package com.l7tech.console.action;

import com.l7tech.console.event.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.IdentityProviderNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.EventListener;
import java.util.logging.Level;

import static com.l7tech.objectmodel.EntityType.ID_PROVIDER_CONFIG;

/**
 * The <code>IdentityProviderPropertiesAction</code> edits the
 * identity provider.
 *
 * @author Emil Marceta
 */
public class IdentityProviderPropertiesAction extends NodeAction {
    private EventListenerList listenerList = new EventListenerList();

    public IdentityProviderPropertiesAction(IdentityProviderNode nodeIdentity) {
        super(nodeIdentity, LIC_AUTH_ASSERTIONS, null);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Properties";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "Edit Identity Provider properties";
    }

    /**
     * specify the resource name for this action
     */
    @Override
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
    @Override
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                EntityHeader header = ((EntityHeaderNode)node).getEntityHeader();
                IdentityProviderConfig iProvider;
                if (!Goid.isDefault(header.getGoid()) ) {
                    try {
                        iProvider = getIdentityAdmin().findIdentityProviderConfigByID(header.getGoid());

                        if ( iProvider == null ) {
                            handleProviderDeleted(header);
                        } else {
                            edit( iProvider );
                        }

                    } catch (Exception e1) {
                        ErrorManager.getDefault().
                          notify(Level.WARNING, e1, "Error retrieving provider.");
                    }
                }
            }
        });
    }

    private void edit( final IdentityProviderConfig iProvider ) {
        final Frame f = TopComponents.getInstance().getTopParent();
        final WizardStepPanel configPanel;
        final Wizard w;

        if (iProvider.type() == IdentityProviderType.INTERNAL || iProvider.type() == IdentityProviderType.LDAP) {
            if (iProvider.type() == IdentityProviderType.LDAP) {
                configPanel = NewLdapProviderAction.buildPanels( false );
            } else {
                configPanel = new InternalIdentityProviderConfigPanel(new IdentityProviderCertificateValidationConfigPanel(null), false);
            }

            w = new EditIdentityProviderWizard(f, configPanel, iProvider);

        } else if (iProvider.type() == IdentityProviderType.FEDERATED) {
            boolean readOnly = !PermissionFlags.get( ID_PROVIDER_CONFIG ).canUpdateSome();
            IdentityProviderCertificateValidationConfigPanel cvPanel = new IdentityProviderCertificateValidationConfigPanel(null,readOnly);
            configPanel = new FederatedIPGeneralPanel(new FederatedIPTrustedCertsPanel(cvPanel, readOnly), readOnly);

            w = new EditFederatedIPWizard(f, configPanel, (FederatedIdentityProviderConfig)iProvider, readOnly);
        } else if (iProvider.type() == IdentityProviderType.BIND_ONLY_LDAP) {
            boolean readOnly = !PermissionFlags.get(ID_PROVIDER_CONFIG).canUpdateSome();
            w = new EditIdentityProviderWizard(f, new BindOnlyLdapGeneralPanel(null, readOnly), iProvider);
        } else if (iProvider.type() == IdentityProviderType.POLICY_BACKED) {
            boolean readOnly = !PermissionFlags.get(ID_PROVIDER_CONFIG).canUpdateSome();
            w = new EditIdentityProviderWizard(f, new PolicyBackedIdentityGeneralPanel(null, readOnly), iProvider);
        } else {
            throw new RuntimeException("Unsupported Identity Provider Type: " + iProvider.type().toString());
        }

        w.addWizardListener(wizardListener);

        // register itself to listen to the updateEvent
        addEntityListener(entityListener);

        w.pack();
        Utilities.centerOnScreen( w );
        DialogDisplayer.display(w, new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        TopComponents.getInstance().refreshIdentityProvidersTree();
                    }
                });
            }
        });
    }

    private void handleProviderDeleted( final EntityHeader header ) {
        DialogDisplayer.showMessageDialog(
                TopComponents.getInstance().getTopParent(),
                "The Identity Provider '"+ TextUtils.truncStringMiddleExact( header.getName(), 60 )+"' is no longer available.",
                "Identity Provider Removed",
                JOptionPane.WARNING_MESSAGE,
                new Runnable(){
            @Override
            public void run() {
                final DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.removeNodeFromParent( node );
                fireEventProviderRemoved( header );
            }
        } );
    }

    /**
     * notify the listeners that the entity has been removed
     */
    private void fireEventProviderRemoved(EntityHeader header) {
        final EntityEvent event = new EntityEvent(this, header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (EventListener listener : listeners) {
            ((EntityListener) listener).entityRemoved(event);
        }
    }

    /**
     * notify the listeners that the entity has been updated
     */
    private void fireEventProviderUpdated(EntityHeader header) {
        EntityEvent event = new EntityEvent(this, header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (EventListener listener : listeners) {
            ((EntityListener) listener).entityUpdated(event);
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

    static void showDuplicateProviderWarning() {
        String msg = "An Identity Provider with the same name already exists.";
        JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                msg,
                "Error Saving Identity Provider",
                JOptionPane.WARNING_MESSAGE);

    }

    private  WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard has finished.
         *
         * @param we the event describing the wizard finish
         */
        @Override
        public void wizardFinished(WizardEvent we) {
            // update the provider
            final Wizard w = (Wizard)we.getSource();
            final IdentityProviderConfig iProvider = (IdentityProviderConfig)w.getWizardInput();

            if (iProvider != null) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getIdentityAdmin().saveIdentityProviderConfig(iProvider);

                            // update the node name in the identity provider tree
                            EntityHeader header = ((EntityHeaderNode)node).getEntityHeader();
                            header.setName(iProvider.getName());
                            header.setType(EntityType.ID_PROVIDER_CONFIG);
                            fireEventProviderUpdated(header);

                        } catch (Exception e) {
                            if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                                showDuplicateProviderWarning();
                                edit( iProvider );
                            } else {
                                ErrorManager.getDefault().notify(Level.WARNING, e, "Error updating the identity provider.");
                            }
                        }
                    }
                });
            }
        }

    };

    private IdentityAdmin getIdentityAdmin() {
        return Registry.getDefault().getIdentityAdmin();
    }

    private EntityListener entityListener = new EntityListenerAdapter() {
        @Override
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