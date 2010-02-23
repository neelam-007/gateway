/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.uddi.WsdlPortInfo;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The wizard that drives the use case of publishing SOAP services.
 */
public class PublishServiceWizard extends Wizard {
    private boolean completedBundle;
    /**
     * the bag of service and assertions that his wizard collects
     */
    static class ServiceAndAssertion {
        /**
         * @return the service
         */
        public PublishedService getService() {
            return service;
        }

        public Collection<ServiceDocument> getServiceDocuments() {
            return serviceDocuments;
        }

        public CompositeAssertion getAssertion() {
            return assertions;
        }

        public void setAssertion(CompositeAssertion assertion) {
            this.assertions = assertion;
        }

        public void setRoutingAssertion(RoutingAssertion ra) {
            routingAssertion = ra;
        }

        public RoutingAssertion getRoutingAssertion() {
            return routingAssertion;
        }

        public String getServiceURI() {
            if(routingAssertion != null && routingAssertion instanceof HttpRoutingAssertion) {
                return ((HttpRoutingAssertion)routingAssertion).getProtectedServiceUrl(); 
            } else {
                try {
                    URL url = service.serviceUrl();
                    if ( url != null )
                        return url.toExternalForm();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL", e);
                } catch (MalformedURLException e) {
                    logger.log(Level.WARNING, "Error accessing service URL", e);
                }
            }
            return null;
        }

        public boolean isSharedPolicy() {
            return sharedPolicy;
        }

        public void setSharedPolicy(boolean sharedPolicy) {
            this.sharedPolicy = sharedPolicy;
        }

        public WsdlPortInfo getWsdlPortInfo() {
            return wsdlPortInfo;
        }

        public void setWsdlPortInfo(WsdlPortInfo wsdlPortInfo) {
            this.wsdlPortInfo = wsdlPortInfo;
        }

        public boolean isServiceControlRequired() {
            return  wsdlPortInfo != null &&
                    ResourceUtils.isSameResource(wsdlPortInfo.getWsdlUrl(),service.getWsdlUrl()) &&
                    wsdlPortInfo.getAccessPointURL() != null &&
                    wsdlPortInfo.getWsdlPortBinding() != null &&
                    wsdlPortInfo.getWsdlPortName() != null &&
                    wsdlPortInfo.getWsdlServiceName() != null &&
                    wsdlPortInfo.isWasWsdlPortSelected();
        }

        private boolean sharedPolicy = false;
        private RoutingAssertion routingAssertion;
        private PublishedService service = new PublishedService();
        private Collection<ServiceDocument> serviceDocuments = new ArrayList<ServiceDocument>();
        private CompositeAssertion assertions = new AllAssertion();
        /**
         * If the service was created from UDDI, then this will be non null;
         */
        private WsdlPortInfo wsdlPortInfo;
    }

    private ServiceAndAssertion saBundle = new ServiceAndAssertion();
    private static final Logger logger = Logger.getLogger(PublishServiceWizard.class.getName());

    private EventListenerList localListenerList = new EventListenerList();

    public static PublishServiceWizard getInstance(Frame parent) {
        ServicePanel panel1 = new ServicePanel();
        if (Registry.getDefault().getLicenseManager().isAuthenticationEnabled()) {
            ServiceResolutionPanel panel2 = new ServiceResolutionPanel();
            IdentityProviderWizardPanel panel3 = new IdentityProviderWizardPanel(true);
            ProtectedServiceWizardPanel panel4 = new ProtectedServiceWizardPanel();
            panel1.setNextPanel(panel2);
            panel2.setNextPanel(panel3);
            panel3.setNextPanel(panel4);
        }
        return new PublishServiceWizard(parent, panel1);
    }

    /**
     * Creates new form PublishServiceWizard
     */
    protected PublishServiceWizard(Frame parent, WizardStepPanel firstPanel) {
        super(parent, firstPanel);
        setTitle("Publish SOAP Web Service Wizard");
        wizardInput = saBundle;
        addWizardListener(new WizardListener() {
            @Override
            public void wizardSelectionChanged(WizardEvent e) {
                // dont care
            }
            @Override
            public void wizardFinished(WizardEvent e) {
                completedBundle = false;
                completeTask();
            }
            @Override
            public void wizardCanceled(WizardEvent e) {
                // dont care
            }
        });
        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(PublishServiceWizard.this);
            }
        });
    }

    private void completeTask() {
        final PublishedService service = saBundle.getService();
        try {
            if (!completedBundle) {
                // routing assertion?
                if (saBundle.getRoutingAssertion() != null) {
                    final List<Assertion> kids = saBundle.getAssertion().getChildren();
                    if (saBundle.isSharedPolicy()) {
                        java.util.List<Assertion> ass = new ArrayList<Assertion>();
                        ass.addAll(kids);
                        ass.add(saBundle.getRoutingAssertion());
                        saBundle.getAssertion().setChildren(ass);
                    } else {
                        for (Assertion a : kids) {
                            if (a instanceof AllAssertion) {
                                AllAssertion aa = (AllAssertion) a;
                                java.util.List<Assertion> ass = new ArrayList<Assertion>();
                                ass.addAll(aa.getChildren());
                                ass.add((Assertion) saBundle.getRoutingAssertion().clone());
                                aa.setChildren(ass);
                            }
                        }
                    }
                }
                saBundle.setAssertion(pruneEmptyCompositeAssertions(saBundle.getAssertion()));
                completedBundle = true;
            }
            if (saBundle.getAssertion() != null) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                WspWriter.writePolicy(saBundle.getAssertion(), bo);
                service.getPolicy().setXml(bo.toString());
                service.getPolicy().setSoap(service.isSoap());
            } else {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                WspWriter.writePolicy(new TrueAssertion(), bo); // means no policy
            }
            saBundle.service.setFolder(TopComponents.getInstance().getRootNode().getFolder());

            final WsdlPortInfo wsdlPortInfo = saBundle.getWsdlPortInfo();
            final PublishedService newService = saBundle.getService();
            newService.setDefaultRoutingUrl( saBundle.isServiceControlRequired() ? wsdlPortInfo.getAccessPointURL() : null);
            newService.setRoutingUri(saBundle.service.getRoutingUri());

            long oid = Registry.getDefault().getServiceManager().savePublishedServiceWithDocuments(newService, saBundle.getServiceDocuments());
            saBundle.service.setOid(oid);
            Registry.getDefault().getSecurityProvider().refreshPermissionCache();

            PublishServiceWizard.this.notify(new ServiceHeader(saBundle.service));

            //was the service created from UDDI, if the WSDL url still matches what was saved, then
            //record this
            if( saBundle.isServiceControlRequired() ){
                UDDIServiceControl uddiServiceControl = new UDDIServiceControl(oid, wsdlPortInfo.getUddiRegistryOid(),
                        wsdlPortInfo.getBusinessEntityKey(), wsdlPortInfo.getBusinessEntityName(),
                        wsdlPortInfo.getBusinessServiceKey(), wsdlPortInfo.getBusinessServiceName(),
                        wsdlPortInfo.getWsdlServiceName(), wsdlPortInfo.getWsdlPortName(), wsdlPortInfo.getWsdlPortBinding(),
                        wsdlPortInfo.getWsdlPortBindingNamespace(), true);

                try {
                    Registry.getDefault().getUDDIRegistryAdmin().saveUDDIServiceControlOnly(uddiServiceControl, wsdlPortInfo.getAccessPointURL(), wsdlPortInfo.getLastUddiMonitoredTimeStamp());
                } catch (Exception e) {
                    final String msg = "Error: " + ExceptionUtils.getMessage(e);
                    logger.log(Level.WARNING, msg, e);
                    DialogDisplayer.showMessageDialog(this, msg, "Cannot put WSDL under UDDI control", JOptionPane.ERROR_MESSAGE, null);
                }
            }
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                logger.log(Level.WARNING, "Cannot publish service as is (duplicate)");
                String msg = "This Web service cannot be saved as is because its resolution\n" +
                             "parameters (SOAPAction, namespace, and possibly routing URI)\n" +
                             "are already used by an existing published service.\n\nWould " +
                             "you like to publish this service using a different routing URI?";
                DialogDisplayer.showConfirmDialog(null, msg, "Service Resolution Conflict", JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            // get new routing URI
                            SoapServiceRoutingURIEditor dlg = new SoapServiceRoutingURIEditor(PublishServiceWizard.this, saBundle.getService());
                            dlg.pack();
                            Utilities.centerOnScreen(dlg);
                            dlg.setVisible(true);
                            if (dlg.wasSubjectAffected()) {
                                completeTask();
                            } else {
                                logger.info("Service publication aborted.");
                            }
                        } else {
                            logger.info("Service publication aborted.");
                        }
                    }
                });
            } else {
                logger.log(Level.WARNING, "Cannot publish service as is", e);
                DialogDisplayer.showMessageDialog(null,
                  "Unable to save the service '" + saBundle.service.getName() + "'\n",
                  "Error",
                  JOptionPane.ERROR_MESSAGE, null);
            }
        }
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
     * Prune empty composite assertions, and return the updated
     * asseriton tree.
     * If the root composite has no children return null.
     *
     * @param oom the input composite assertion
     * @return trhe composite assertion with pruned children
     *         or null
     */
    private CompositeAssertion
      pruneEmptyCompositeAssertions(CompositeAssertion oom) {
        // fla, added, i have't found how, but the wizard somehow populates all children with null elements
        // this causes problems later on since we now support returning an empty policy (all with no children)
        List<Assertion> children = new ArrayList<Assertion>();
        for (Object o : oom.getChildren()) {
            if (o != null && o instanceof Assertion) {
                children.add((Assertion)o);
            }
        }
        oom.setChildren(children);
        if (children.isEmpty()) return oom;
        java.util.Iterator i = oom.preorderIterator();
        for (; i.hasNext();) {
            Assertion a = (Assertion)i.next();
            if (a instanceof CompositeAssertion) {
                CompositeAssertion ca = (CompositeAssertion)a;
                if (ca.getChildren().size() == 0) {
                    i.remove();
                }
            }
        }
        return oom;
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
}
