package com.l7tech.console.panels;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import javax.swing.*;
import javax.wsdl.Binding;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.extensions.soap.SOAPBinding;

import static com.l7tech.util.CollectionUtils.toSet;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;
import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Functions.map;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;
import static com.l7tech.util.Option.somes;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.PublishServiceWizard.ServiceAndAssertion;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.uddi.WsdlPortInfo;
import com.l7tech.util.ExceptionUtils;

/**
 * Service panel is the first stage for the Publish Service wizard.
 *
 * @author Emil Marceta, $Author$
 * @version $Revision$
 */
public class ServicePanel extends WizardStepPanel {
    private WsdlPortInfo wsdlPortInfo;

    //- PUBLIC

    /**
     * Creates new form ServicePanel
     */
    public ServicePanel() {
        super(null);
        initComponents();
    }

    /**
     *
     */
    @Override
    public String getDescription() {
        return "Specify the location for either the WSDL of the service to publish or " +
               "a WSIL document that contains a link to that WSDL.";
    }

    @Override
    public boolean canAdvance() {
        return wsdlLocationPanel.isLocationValid();
    }

    @Override
    public boolean canFinish() {
        return wsdlLocationPanel.isLocationValid();
    }

    /**
     * Attempt to resolve the WSDL.  Returns true if we have a valid one, or false otherwise.
     * Will pester the user with a dialog box if the WSDL could not be fetched.
     * @return true iff. the WSDL URL in the URL: text field was downloaded successfully.
     */
    @Override
    public boolean onNextButton() {
        if (TopComponents.getInstance().isConnectionLost()) {
            DialogDisplayer.showMessageDialog(this, "The connection to the Gateway was lost", "Connection Lost", JOptionPane.ERROR_MESSAGE, null, null);
            return false;
        }
        boolean res = processWsdlLocation();
        if (res) {
            // test for soap bindings
            Collection<Binding> bindings = wsdl.getBindings();
            if (bindings != null) {
                for ( Binding binding : bindings ) {
                    //noinspection unchecked
                    java.util.List<ExtensibilityElement> bindingEels = (java.util.List<ExtensibilityElement>)binding.getExtensibilityElements();
                    for (ExtensibilityElement eel : bindingEels) {
                        // weird second part if to avoid class path conflict when running from idea where the cp is different than at runtime
                        if ((eel instanceof SOAPBinding || eel instanceof SOAP12Binding) && !eel.getClass().getName().equals("com.idoox.wsdl.extensions.soap12.SOAP12Binding")) {
                            return res;
                        }
                    }
                }
            }
            // tell user soap 1.2 is not supported as of yet
            DialogDisplayer.showMessageDialog(getOwner(), "This WSDL does not contain a supported SOAP Binding",
                                                          "No Supported SOAP Binding", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }
        return res;
    }

    @Override
    public void storeSettings(Object settings) throws IllegalStateException {
        if (!(settings instanceof ServiceAndAssertion)) {
            throw new IllegalArgumentException();
        }
        try {
            PublishServiceWizard.ServiceAndAssertion
              sa = (PublishServiceWizard.ServiceAndAssertion)settings;
            sa.setWsdlPortInfo(wsdlPortInfo);
            PublishedService publishedService = sa.getService();

            publishedService.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(serviceDocuments) );
            publishedService.setName(service.getName());
            publishedService.setWsdlUrl(service.getWsdlUrl());
            publishedService.setWsdlXml(service.getWsdlXml());
            publishedService.setDefaultRoutingUrl( sa.isServiceControlRequired() ? wsdlPortInfo.getAccessPointURL() : null);

            sa.getServiceDocuments().clear();
            sa.getServiceDocuments().addAll(serviceDocuments);

            if (sa.getRoutingAssertion() == null) {
                try {
                    URL url = publishedService.serviceUrl();
                    if ( url != null )
                        sa.setRoutingAssertion( new HttpRoutingAssertion( url.toExternalForm() ) );
                } catch ( WSDLException we ) {
                    logger.warning( "Error determining URL for routing assertion '"+ ExceptionUtils.getMessage( we )+"'." );
                } catch ( MalformedURLException mue ) {
                    logger.warning( "Error determining URL for routing assertion '"+ ExceptionUtils.getMessage( mue )+"'." );
                }
                if (sa.getAssertion() != null && sa.getAssertion().getChildren().isEmpty() && sa.getRoutingAssertion() != null) {
                    sa.getAssertion().addChild(sa.getRoutingAssertion());
                }
            }

            final Unary<Option<String>, String> pathMapper = getUriPathMapper();
            if ( wsdlPortInfo != null ) {
                sa.setCustomUriOptions( toSet( pathMapper.call( wsdlPortInfo.getAccessPointURL() ).toList() ) );
            } else if ( wsdl != null ) {
                final Set<String> uris = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
                uris.addAll( somes( map( wsdl.getServiceURIs(), pathMapper ) ) );
                sa.setCustomUriOptions( uris );
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "Error storing settings.", e); // this used to do e.printStackTrace() this is slightly better.
        }
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Web Service Description";
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServicePanel.class.getName());

    // local service copy
    private PublishedService service = new PublishedService();
    private Collection<ServiceDocument> serviceDocuments = new ArrayList<ServiceDocument>();
    private WsdlLocationPanel wsdlLocationPanel;
    private Wsdl wsdl;

    /**
     * This method is called from within the constructor to initialize the form.
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        wsdlLocationPanel = new WsdlLocationPanel(getOwner(), logger,
                true, SearchUddiDialog.uddiEnabled());
        wsdlLocationPanel.addPropertyListener(new PropertyChangeListener(){
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                notifyListeners();
            }
        }, "wsdlUrl");
        add(wsdlLocationPanel);
    }

   /**
     * Attempt to resolve the WSDL.  Returns true if we have a valid one, or false otherwise.
     * Will pester the user with a dialog box if the WSDL could not be fetched.
     *
     * @return true iff. the WSDL URL in the URL: text field was downloaded successfully.
     */
    private boolean processWsdlLocation() {
        boolean processed = false;
        notifyListeners();

        final String wsdlUrl = wsdlLocationPanel.getWsdlUrl(true);
        try {
            wsdl = wsdlLocationPanel.getWsdl();
            wsdlPortInfo = wsdlLocationPanel.getWsdlPortInfo();
            if (wsdl != null) {
                final String resolvedDoc = wsdlLocationPanel.getWsdlContent(0);

                final String serviceName = wsdl.getServiceName();
                // if service name not obtained service name is WSDL URL
                if (serviceName == null || "".equals(serviceName)) {
                    service.setName(wsdlUrl);
                } else {
                    service.setName(serviceName);
                }
                serviceDocuments.clear();

                for (int i=1; i<wsdlLocationPanel.getWsdlCount(); i++) {
                    ServiceDocument sd = new ServiceDocument();
                    sd.setUri(wsdlLocationPanel.getWsdlUri(i)); 
                    sd.setContentType("text/xml");
                    sd.setType("WSDL-IMPORT");
                    sd.setContents(wsdlLocationPanel.getWsdlContent(i));
                    serviceDocuments.add(sd);
                }

                service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(serviceDocuments) );
                service.setWsdlUrl(wsdlUrl);
                service.setWsdlXml(resolvedDoc);
                processed = true;
                notifyListeners();
            }
        } catch (MalformedURLException e1) {
            logger.log(Level.INFO, "Could not parse URL.", e1); // this used to do e.printStackTrace() this is slightly better.
            JOptionPane.showMessageDialog(getOwner(),
              "Illegal URL string '" + wsdlUrl + "'\n",
              "Error",
              JOptionPane.ERROR_MESSAGE);
        } catch (IOException e1) {
            logger.log(Level.INFO, "IO Error.", e1); // this used to do e.printStackTrace() this is slightly better.
            JOptionPane.showMessageDialog(getOwner(),
                                          "Unable to parse the WSDL at location '" + wsdlUrl +
                                          "'\n",
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        }

        return processed;
    }

    private Unary<Option<String>, String> getUriPathMapper() {
        return new Unary<Option<String>,String>() {
            @Override
            public Option<String> call( final String uri ) {
                if ( uri != null ) {
                    try {
                        return some( new URI( uri ).getPath() );
                    } catch ( URISyntaxException e ) {
                        logger.log( Level.INFO, "Error processing custom uri option: " + getMessage(e), getDebugException(e) );
                    }
                }
                return none();
            }
        };
    }
}
