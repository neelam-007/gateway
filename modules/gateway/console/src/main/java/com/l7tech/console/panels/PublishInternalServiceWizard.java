package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
    User: megery
 */
public class PublishInternalServiceWizard extends Wizard<PublishInternalServiceWizard.ServiceTemplateHolder> {
    private static final Logger logger = Logger.getLogger(PublishInternalServiceWizard.class.getName());

    private EventListenerList localListenerList = new EventListenerList();

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
            service.setFolder(TopComponents.getInstance().getRootNode().getFolder());

            service.setName(toSave.getName());
            service.getPolicy().setXml(toSave.getDefaultPolicyXml());
            service.setRoutingUri(toSave.getDefaultUriPrefix());

            service.setSoap(true);
            service.setInternal(true);
            service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(toSave.getServiceDocuments()) );
            service.setWsdlUrl(toSave.getWsdlUrl());
            service.setWsdlXml(toSave.getWsdlXml());

            service.setDisabled(false);
        }

        final Frame parent = TopComponents.getInstance().getTopParent();
        final PublishedService newService = service;
        PublishServiceWizard.saveServiceWithResolutionCheck( parent, service, toSave.getServiceDocuments(), new Functions.UnaryVoidThrows<Long,Exception>(){
            @Override
            public void call( final Long oid ) throws Exception {
                newService.setOid(oid);
                Registry.getDefault().getSecurityProvider().refreshPermissionCache();
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
