package com.l7tech.console.panels;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.console.action.Actions;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>JDialog</code> wizard that drives the publish service
 * use case.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
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
        private CompositeAssertion assertions = new AllAssertion();
    }

    private ServiceAndAssertion saBundle = new ServiceAndAssertion();
    private static final Logger logger = Logger.getLogger(PublishServiceWizard.class.getName());

    private EventListenerList localListenerList = new EventListenerList();

    public static PublishServiceWizard getInstance(Frame parent) {
        ServicePanel panel1 = new ServicePanel();
        IdentityProviderWizardPanel panel2 = new IdentityProviderWizardPanel(true);
        ProtectedServiceWizardPanel panel3 = new ProtectedServiceWizardPanel();
        panel1.setNextPanel(panel2);
        panel2.setNextPanel(panel3);
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
        try {
            if (!completedBundle) {
                // routing assertion?
                if (saBundle.getRoutingAssertion() != null) {
                    if (saBundle.isSharedPolicy()) {
                        java.util.List ass = new ArrayList();
                        ass.addAll(saBundle.getAssertion().getChildren());
                        ass.add(saBundle.getRoutingAssertion());
                        saBundle.getAssertion().setChildren(ass);
                    } else {

                        for (java.util.Iterator it =
                          saBundle.getAssertion().getChildren().iterator(); it.hasNext();) {
                            Assertion a = (Assertion)it.next();
                            if (a instanceof AllAssertion) {
                                AllAssertion aa = (AllAssertion)a;
                                java.util.List ass = new ArrayList();
                                ass.addAll(aa.getChildren());
                                ass.add(saBundle.getRoutingAssertion().clone());
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
                saBundle.getService().setPolicyXml(bo.toString());
            } else {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                WspWriter.writePolicy(new TrueAssertion(), bo); // means no policy
            }
            long oid =
              Registry.getDefault().getServiceManager().savePublishedService(saBundle.getService());
            EntityHeader header = new EntityHeader();
            header.setType(EntityType.SERVICE);
            header.setName(saBundle.service.getName());
            header.setOid(oid);
            PublishServiceWizard.this.notify(header);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot publish service as is", e);
            if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
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
                    dlg.show();
                    if (dlg.wasSubjectAffected()) {
                        completeTask();
                    } else {
                        logger.info("Service publication aborted.");
                    }
                } else {
                    logger.info("Service publication aborted.");
                }
            } else {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                  "Unable to save the service '" + saBundle.service.getName() + "'\n",
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
            }
            return;
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
        if (oom.getChildren().isEmpty()) return null;
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
        EventListener[] listeners = localListenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener)listeners[i]).entityAdded(event);
        }
    }

    /**
     * @deprecated do not use -- this is here only for the benefit of the PublishServiceWizardTest
     */
    public void setWsdlUrl(String newUrl) {
        //((ServicePanel)panels[0]).setWsdlUrl(newUrl);
    }

}
