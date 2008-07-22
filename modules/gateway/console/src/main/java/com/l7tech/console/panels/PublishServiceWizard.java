/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;

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
                    Wsdl wsdl = service.parsedWsdl();
                    if (wsdl != null) return wsdl.getServiceURI();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL", e);
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

        private boolean sharedPolicy = false;
        private RoutingAssertion routingAssertion;
        private PublishedService service = new PublishedService();
        private Collection<ServiceDocument> serviceDocuments = new ArrayList();
        private CompositeAssertion assertions = new AllAssertion();
    }

    private ServiceAndAssertion saBundle = new ServiceAndAssertion();
    private static final Logger logger = Logger.getLogger(PublishServiceWizard.class.getName());

    private EventListenerList localListenerList = new EventListenerList();

    public static PublishServiceWizard getInstance(Frame parent) {
        ServicePanel panel1 = new ServicePanel();
        if (Registry.getDefault().getLicenseManager().isAuthenticationEnabled()) {
            IdentityProviderWizardPanel panel2 = new IdentityProviderWizardPanel(true);
            ProtectedServiceWizardPanel panel3 = new ProtectedServiceWizardPanel();
            panel1.setNextPanel(panel2);
            panel2.setNextPanel(panel3);
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
            public void wizardSelectionChanged(WizardEvent e) {
                // dont care
            }
            public void wizardFinished(WizardEvent e) {
                completedBundle = false;
                completeTask();
            }
            public void wizardCanceled(WizardEvent e) {
                // dont care
            }
        });
        getButtonHelp().addActionListener(new ActionListener() {
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
            long oid = Registry.getDefault().getServiceManager().savePublishedServiceWithDocuments(saBundle.getService(), saBundle.getServiceDocuments());
            saBundle.service.setOid(oid);
            Registry.getDefault().getSecurityProvider().refreshPermissionCache();

            PublishServiceWizard.this.notify(new ServiceHeader(saBundle.service));
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
                    SoapServiceRoutingURIEditor dlg = new SoapServiceRoutingURIEditor(this, saBundle.getService());
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
            } else {
                logger.log(Level.WARNING, "Cannot publish service as is", e);
                JOptionPane.showMessageDialog(null,
                  "Unable to save the service '" + saBundle.service.getName() + "'\n",
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
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
        ArrayList children = new ArrayList();
        for (Object o : oom.getChildren()) {
            if (o != null && o instanceof Assertion) {
                children.add(o);
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
