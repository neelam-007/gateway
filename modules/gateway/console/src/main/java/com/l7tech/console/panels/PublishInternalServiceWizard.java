package com.l7tech.console.panels;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.*;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
    User: megery
 */
public class PublishInternalServiceWizard extends Wizard<PublishInternalServiceWizard.ServiceTemplateHolder> {
    private static final Logger logger = Logger.getLogger(PublishInternalServiceWizard.class.getName());

    private EventListenerList localListenerList = new EventListenerList();
    private Option<Folder> folder = Option.none();

    public static PublishInternalServiceWizard getInstance (Frame parent) {
        return new PublishInternalServiceWizard(parent, getSteps());
    }

    private PublishInternalServiceWizard(Frame parent, WizardStepPanel<PublishInternalServiceWizard.ServiceTemplateHolder> panel) {
        super(parent, panel, buildServiceTemplateHolder());
        setTitle("Publish Internal Service Wizard");

        addWizardListener(new WizardAdapter() {
            @Override
            public void wizardFinished(WizardEvent e) {
                try {
                    completeTask(null);
                } catch (MalformedURLException e1) {
                    throw new RuntimeException(e1);
                }
            }
        });
        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(PublishInternalServiceWizard.this);
            }
        });
    }

    private static ServiceTemplateHolder buildServiceTemplateHolder() {
        ServiceAdmin svcManager = Registry.getDefault().getServiceManager();
        Set<ServiceTemplate> templates = svcManager.findAllTemplates();
        return new ServiceTemplateHolder(templates);
    }

    private void completeTask(PublishedService service) throws MalformedURLException {

        ServiceTemplate toSave = wizardInput.getSelectedTemplate();
        if (toSave == null) return;

        if (service == null) {
            service = new PublishedService();
            service.setFolder(folder.orSome(TopComponents.getInstance().getRootNode().getFolder()));

            service.setName(toSave.getName());
            service.getPolicy().setXml(toSave.getDefaultPolicyXml());
            service.setRoutingUri(toSave.getDefaultUriPrefix());

            service.setSoap(toSave.isSoap());
            service.setInternal(true);
            service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(toSave.getServiceDocuments()) );
            service.setWsdlUrl(toSave.getServiceDescriptorUrl());
            service.setWsdlXml(toSave.getServiceDescriptorXml());

            service.setDisabled(false);
        }

        final Frame parent = TopComponents.getInstance().getTopParent();
        final PublishedService newService = service;

       //check if service is SOAP
        if(service.isSoap()) {
            PublishServiceWizard.saveServiceWithResolutionCheck( parent, service, toSave.getServiceDocuments(), new Functions.UnaryVoidThrows<Long,Exception>(){
                @Override
                public void call( final Long oid ) throws Exception {
                    newService.setOid(oid);
                    Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                    Thread.sleep(1000);
                    PublishInternalServiceWizard.this.notify(new ServiceHeader(newService));
                }
            }, new Functions.UnaryVoid<Exception>(){
                @Override
                public void call( final Exception e ) {
                    logger.log(Level.WARNING, "Cannot publish service as is", e);
                    JOptionPane.showMessageDialog(null,
                      "Unable to save the service '" + newService.getName() + "'\n",
                      "Error",
                      JOptionPane.ERROR_MESSAGE);
                }
            });
        }
        else {
            saveNonSoapServiceWithResolutionCheck(parent, service);
        }
    }

    /**
     * performs service URI resolution check and publishes the service
     * @param parent The parent for any dialogs (may be null)
     * @param service  The service to be saved (required)
     */
    private void saveNonSoapServiceWithResolutionCheck(final Frame parent, final PublishedService service) {
        try {
            // set supported http methods
            service.setHttpMethods(EnumSet.of(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE));

            final Runnable saver = new Runnable(){
                @Override
                public void run() {
                    try {
                        long oid = Registry.getDefault().getServiceManager().savePublishedService(service);
                        Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                        service.setOid(oid);
                        Thread.sleep(1000);
                        PublishInternalServiceWizard.this.notify(new ServiceHeader(service));
                    } catch ( Exception e ) {
                        handlePublishServiceError(parent, service, e);
                    }
                }
            };
            //check the service URI resolution conflict
            if ( ServicePropertiesDialog.hasResolutionConflict( service, null ) ) {
                final String message =
                      "Resolution parameters conflict for service '" + service.getName() + "'\n" +
                      "because an existing service is already using the URI " + service.getRoutingUri() + "\n\n" +
                      "Would you like to publish this service using a different routing URI?";
                DialogDisplayer.showConfirmDialog(parent, message, "Service Resolution Conflict", JOptionPane.YES_NO_CANCEL_OPTION, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(final int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            // get new routing URI
                            final SoapServiceRoutingURIEditor dlg = new SoapServiceRoutingURIEditor(parent, service);
                            DialogDisplayer.display(dlg, new Runnable() {
                                @Override
                                public void run() {
                                    if (dlg.wasSubjectAffected()) {
                                        saveNonSoapServiceWithResolutionCheck(parent, service);
                                    } else {
                                        saver.run();
                                    }
                                }
                            });
                        } else if (option == JOptionPane.NO_OPTION) {
                            saver.run();
                        }
                    }
                });
            } else {
                saver.run();
            }
        } catch (Exception e) {
            handlePublishServiceError(parent, service, e);
        }
    }

    private void handlePublishServiceError(final Frame parent, final PublishedService service, final Exception e) {
        final String message = "Unable to save the service '" + service.getName() + "'\n";
        logger.log( Level.INFO, message, e);
        JOptionPane.showMessageDialog(parent,
          message,
          "Error",
          JOptionPane.ERROR_MESSAGE);
    }

    public static WizardStepPanel<PublishInternalServiceWizard.ServiceTemplateHolder> getSteps() {
        return new InternalServiceSelectionPanel();
    }

    /**
     * add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        localListenerList.add(EntityListener.class, listener);
    }

    /**
     * remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    public void removeEntityListener(EntityListener listener) {
        localListenerList.remove(EntityListener.class, listener);
    }

    /**
     * Set the folder to use for the service.
     *
     * @param folder The folder to use
     */
    public void setFolder( @NotNull final Folder folder ) {
        this.folder = Option.some( folder );
    }

    /**
     * notify the listeners
     *
     * @param header
     */
    private void notify(EntityHeader header) {
        EntityEvent event = new EntityEvent(this, header);
        EntityListener[] listeners = localListenerList.getListeners(EntityListener.class);
        for (EntityListener listener : listeners) {
            listener.entityAdded(event);
        }
    }

    public static class ServiceTemplateHolder {
        Set<ServiceTemplate> allTemplates;
        ServiceTemplate selectedTemplate;
        
        public ServiceTemplateHolder(Set<ServiceTemplate> templates) {
            this.allTemplates = templates;
        }

        public Set<ServiceTemplate> getAllTemplates() {
            return allTemplates;
        }

        public void setAllTemplates(Set<ServiceTemplate> allTemplates) {
            this.allTemplates = allTemplates;
        }

        public ServiceTemplate getSelectedTemplate() {
            return selectedTemplate;
        }

        public void setSelectedTemplate(ServiceTemplate selectedTemplate) {
            this.selectedTemplate = selectedTemplate;
        }
    }
}
