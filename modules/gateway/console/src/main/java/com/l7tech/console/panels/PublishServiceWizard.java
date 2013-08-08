package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.uddi.WsdlPortInfo;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.CollectionUtils.toSet;
import static java.util.Collections.emptySet;

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

        @NotNull
        public Option<Folder> getFolder() {
            return folder;
        }

        public void setFolder( @NotNull final Option<Folder> folder ) {
            this.folder = folder;
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

        public Set<String> getCustomUriOptions() {
            return customUriOptions;
        }

        public void setCustomUriOptions( final Set<String> customUriOptions ) {
            this.customUriOptions = toSet( customUriOptions );
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
        private Option<Folder> folder = Option.none();
        private Set<String> customUriOptions = emptySet();
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
        addWizardListener(new WizardAdapter() {
            @Override
            public void wizardFinished(WizardEvent e) {
                completedBundle = false;
                completeTask();
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
            saBundle.service.setFolder(saBundle.getFolder().orSome(TopComponents.getInstance().getRootNode().getFolder()));

            final WsdlPortInfo wsdlPortInfo = saBundle.getWsdlPortInfo();
            final PublishedService newService = saBundle.getService();
            newService.setDefaultRoutingUrl( saBundle.isServiceControlRequired() ? wsdlPortInfo.getAccessPointURL() : null);
            newService.setRoutingUri(saBundle.service.getRoutingUri());

            final Frame parent = TopComponents.getInstance().getTopParent();
            final Collection<ServiceDocument> serviceDocuments = saBundle.getServiceDocuments();
            saveServiceWithResolutionCheck( parent, newService, serviceDocuments, new Functions.UnaryVoidThrows<Goid,Exception>(){
                @Override
                public void call( final Goid goid ) throws Exception {
                    saBundle.service.setGoid(goid);
                    Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                    Thread.sleep(1000);
                    PublishServiceWizard.this.notify(new ServiceHeader(saBundle.service));

                    //was the service created from UDDI, if the WSDL url still matches what was saved, then
                    //record this
                    if( saBundle.isServiceControlRequired() ){
                        UDDIServiceControl uddiServiceControl = new UDDIServiceControl(goid, Goid.parseGoid(wsdlPortInfo.getUddiRegistryId()),
                                wsdlPortInfo.getBusinessEntityKey(), wsdlPortInfo.getBusinessEntityName(),
                                wsdlPortInfo.getBusinessServiceKey(), wsdlPortInfo.getBusinessServiceName(),
                                wsdlPortInfo.getWsdlServiceName(), wsdlPortInfo.getWsdlPortName(), wsdlPortInfo.getWsdlPortBinding(),
                                wsdlPortInfo.getWsdlPortBindingNamespace(), true);
                        uddiServiceControl.setSecurityZone(service.getSecurityZone());

                        try {
                            Registry.getDefault().getUDDIRegistryAdmin().saveUDDIServiceControlOnly(uddiServiceControl, wsdlPortInfo.getAccessPointURL(), wsdlPortInfo.getLastUddiMonitoredTimeStamp());
                        } catch (Exception e) {
                            final String msg = "Error: " + ExceptionUtils.getMessage(e);
                            logger.log(Level.WARNING, msg, e);
                            DialogDisplayer.showMessageDialog(parent, msg, "Cannot put WSDL under UDDI control", JOptionPane.ERROR_MESSAGE, null);
                        }
                    }
                }
            }, new Functions.UnaryVoid<Exception>(){
                @Override
                public void call( final Exception e ) {
                    handlePublishError( e );
                }
            } );
        } catch (Exception e) {
            handlePublishError( e );
        }
    }

    private void handlePublishError( final Exception e ) {
        logger.log( Level.WARNING, "Cannot publish service as is", e);
        DialogDisplayer.showMessageDialog(null,
          "Unable to save the service '" + saBundle.service.getName() + "'\n",
          "Error",
          JOptionPane.ERROR_MESSAGE, null);
    }

    /**
     * Save a published service with a resolution check.
     *
     * <p>This is for SOAP services. Any exception thrown in the success
     * callback will be dispatched to the error callback.</p>
     * 
     * @param parent The parent for any dialogs (may be null)
     * @param newService  The service to be saved (required)
     * @param newServiceDocuments (may be null)
     * @param callback The callback for success
     * @param errorCallback The callback for errors
     */
    public static void saveServiceWithResolutionCheck( final Frame parent,
                                                       final PublishedService newService,
                                                       final Collection<ServiceDocument> newServiceDocuments,
                                                       final Functions.UnaryVoidThrows<Goid,Exception> callback,
                                                       final Functions.UnaryVoid<Exception> errorCallback ) {
            if ( ServicePropertiesDialog.hasResolutionConflict( newService, newServiceDocuments ) ) {
                String msg = "The resolution parameters (SOAPAction, namespace, and possibly\n" +
                             "routing URI) for this Web service are already used by an existing\n" +
                             "published service.\n\nWould you like to publish this service using" +
                             " a different routing URI?";
                DialogDisplayer.showConfirmDialog(parent, msg, "Service Resolution Conflict", JOptionPane.YES_NO_CANCEL_OPTION, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            // get new routing URI
                            final SoapServiceRoutingURIEditor dlg = new SoapServiceRoutingURIEditor(parent, newService);
                            DialogDisplayer.display( dlg, new Runnable(){
                                @Override
                                public void run() {
                                    if ( dlg.wasSubjectAffected() ) {
                                        saveServiceWithResolutionCheck( parent, newService, newServiceDocuments, callback, errorCallback );
                                    } else {
                                        savePublishedService( newService, newServiceDocuments, callback, errorCallback );
                                    }
                                }
                            } );
                        } else if (option == JOptionPane.NO_OPTION){
                            savePublishedService( newService, newServiceDocuments, callback, errorCallback );
                        }
                    }
                });
            } else {
                savePublishedService( newService, newServiceDocuments, callback, errorCallback );
            }
        }

    private static void savePublishedService( final PublishedService newService,
                                              final Collection<ServiceDocument> newServiceDocuments,
                                              final Functions.UnaryVoidThrows<Goid, Exception> callback,
                                              final Functions.UnaryVoid<Exception> errorCallback ) {
        try {
            Goid goid = Registry.getDefault().getServiceManager().savePublishedServiceWithDocuments(newService, newServiceDocuments);
            callback.call( goid );
        } catch ( Exception e ) {
            errorCallback.call(e);
        }
    }

    /**
     * Set the folder to use for the service.
     *
     * @param folder The folder to use (required)
     */
    public void setFolder( @NotNull final Folder folder ) {
        saBundle.setFolder( Option.some(folder) );
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
     * assertion tree.
     * If the root composite has no children return null.
     *
     * @param oom the input composite assertion
     * @return the composite assertion with pruned children
     *         or null
     */
    private CompositeAssertion
      pruneEmptyCompositeAssertions(CompositeAssertion oom) {
        // fla, added, i haven't found how, but the wizard somehow populates all children with null elements
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
}
