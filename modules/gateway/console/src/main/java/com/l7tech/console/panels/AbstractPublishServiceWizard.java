package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.logging.PermissionDeniedErrorHandler;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * Parent class for all wizards that publish services.
 */
public abstract class AbstractPublishServiceWizard extends Wizard {
    /**
     * Set the Folder for the service.
     *
     * @param folder The folder to use (required)
     */
    public void setFolder(@NotNull final Folder folder) {
        this.folder = Option.some(folder);
    }

    /**
     * Add an EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * Remove an the EntityListener
     *
     * @param listener the EntityListener
     */
    public void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
    }

    protected Option<Folder> folder = Option.none();

    protected AbstractPublishServiceWizard(@NotNull final Frame parent, @NotNull final WizardStepPanel firstPanel, @NotNull final String title) {
        super(parent, firstPanel);
        setTitle(title);
        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(AbstractPublishServiceWizard.this);
            }
        });
    }

    /**
     * Check existing services for resolution conflicts with the service to publish and displays a confirmation dialog
     * if there is a resolution conflict.
     *
     * @param service   the service to publish.
     * @param onSuccess Runnable to execute if there is no resolution conflict or the user opts to continue regardless of conflict.
     */
    protected void checkResolutionConflict(@NotNull final PublishedService service, @NotNull final Runnable onSuccess) {
        if (ServicePropertiesDialog.hasResolutionConflict(service, null)) {
            final String message =
                    "Resolution parameters conflict for service '" + service.getName() + "'\n" +
                            "because an existing service is already using the URI " + service.getRoutingUri() + "\n\n" +
                            "Do you want to save this service?";
            DialogDisplayer.showConfirmDialog(this, message, "Service Resolution Conflict", JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(final int option) {
                    if (option == JOptionPane.YES_OPTION) {
                        onSuccess.run();
                    }
                }
            });
        } else {
            onSuccess.run();
        }
    }

    /**
     * Check existing services for resolution conflicts with the service to publish and displays a confirmation dialog
     * if there is a resolution conflict.
     * <p/>
     * If there is no conflict or the user decides to continue regardless of conflict, the service will be saved and any
     * listeners will be notified.
     *
     * @param service the service to publish.
     */
    protected void checkResolutionConflictAndSave(@NotNull final PublishedService service) {
        checkResolutionConflict(service, new Runnable() {
            @Override
            public void run() {
                try {
                    Goid goid = Registry.getDefault().getServiceManager().savePublishedService(service);
                    Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                    service.setGoid(goid);
                    Thread.sleep(1000);
                    AbstractPublishServiceWizard.this.notify(new ServiceHeader(service));
                    AbstractPublishServiceWizard.super.finish(null);
                } catch (final Exception e) {
                    if (e instanceof PermissionDeniedException) {
                        PermissionDeniedErrorHandler.showMessageDialog((PermissionDeniedException) e, logger);
                    } else {
                        DialogDisplayer.display(new JOptionPane("Error saving the service '" + service.getName() + "'"), getParent(), "Error", null);
                    }
                }
            }
        });
    }

    protected void notify(@NotNull final EntityHeader header) {
        final EntityEvent event = new EntityEvent(this, header);
        final EntityListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (EntityListener listener : listeners) {
            listener.entityAdded(event);
        }
    }

    private static final Logger logger = Logger.getLogger(AbstractPublishServiceWizard.class.getName());
}
