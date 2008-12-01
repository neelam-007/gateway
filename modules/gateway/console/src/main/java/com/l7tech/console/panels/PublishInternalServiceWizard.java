package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.util.ExceptionUtils;

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
public class PublishInternalServiceWizard extends Wizard {
    private static final Logger logger = Logger.getLogger(PublishInternalServiceWizard.class.getName());

    private ServiceTemplateHolder templateHolder;

    private EventListenerList localListenerList = new EventListenerList();

    public static PublishInternalServiceWizard getInstance (Frame parent) {
        return new PublishInternalServiceWizard(parent, getSteps());
    }

    private PublishInternalServiceWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);

        setTitle("Publish Internal Service Wizard");
        ServiceAdmin svcManager = Registry.getDefault().getServiceManager();
        Set<ServiceTemplate> templates = svcManager.findAllTemplates();
        templateHolder = new ServiceTemplateHolder(templates);
        wizardInput = templateHolder;

        addWizardListener(new WizardListener() {
            public void wizardSelectionChanged(WizardEvent e) {
                // dont care
            }
            public void wizardFinished(WizardEvent e) {
                try {
                    completeTask(null);
                } catch (MalformedURLException e1) {
                    throw new RuntimeException(e1);
                }
            }
            public void wizardCanceled(WizardEvent e) {
                // dont care
            }
        });
        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(PublishInternalServiceWizard.this);
            }
        });
    }

    private void completeTask(PublishedService service) throws MalformedURLException {

        ServiceTemplate toSave = templateHolder.getSelectedTemplate();
        if (toSave == null) return;

        if (service == null) {
            service = new PublishedService();
            service.setFolder(TopComponents.getInstance().getRootNode().getFolder());

            service.setName(toSave.getName());
            service.getPolicy().setXml(toSave.getDefaultPolicyXml());
            service.setRoutingUri(toSave.getDefaultUriPrefix());

            service.setSoap(true);
            service.setInternal(true);
            service.setWsdlXml(toSave.getWsdlXml());
            service.setWsdlUrl(toSave.getWsdlUrl());

            service.setDisabled(false);
        }

        try {
            long oid = Registry.getDefault().getServiceManager().savePublishedServiceWithDocuments(service, toSave.getServiceDocuments());
            service.setOid(oid);
            Registry.getDefault().getSecurityProvider().refreshPermissionCache();

            PublishInternalServiceWizard.this.notify(new ServiceHeader(service));
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                logger.log(Level.WARNING, "Cannot publish service as is (duplicate)");
                String msg = "This Web service cannot be saved as is because its resolution\n" +
                             "parameters (SOAPAction, namespace, and possibly routing URI)\n" +
                             "are already used by an existing published service.\n\nWould " +
                             "you like to publish this service using a different routing URI?";
                int answer = JOptionPane.showConfirmDialog(null, msg, "Service Resolution Conflict", JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    // get new routing URI
                    SoapServiceRoutingURIEditor dlg = new SoapServiceRoutingURIEditor(this, service);
                    dlg.pack();
                    Utilities.centerOnScreen(dlg);
                    dlg.setVisible(true);
                    if (dlg.wasSubjectAffected()) {
                        completeTask(service);
                    } else {
                        logger.info("Service publication aborted.");
                    }
                } else {
                    logger.info("Service publication aborted.");
                }
            } else {
                logger.log(Level.WARNING, "Cannot publish service as is", e);
                JOptionPane.showMessageDialog(null,
                  "Unable to save the service '" + service.getName() + "'\n",
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    public static WizardStepPanel getSteps() {
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
     * notfy the listeners
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
