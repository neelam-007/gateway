package com.l7tech.console.panels;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.ArrayList;
import javax.swing.*;
import javax.wsdl.Port;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.panels.PublishServiceWizard.ServiceAndAssertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceDocument;

/**
 * Service panel is the first stage for the Publish Service wizard.
 *
 * @author Emil Marceta, $Author$
 * @version $Revision$
 */
public class ServicePanel extends WizardStepPanel {

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
    public String getDescription() {
        return "Specify the location for either the WSDL of the service to publish or " +
               "a WSIL document that contains a link to that WSDL.";
    }

    public boolean canAdvance() {
        return wsdlLocationPanel.isLocationValid();
    }

    public boolean canFinish() {
        return wsdlLocationPanel.isLocationValid();
    }

    /**
     * Attempt to resolve the WSDL.  Returns true if we have a valid one, or false otherwise.
     * Will pester the user with a dialog box if the WSDL could not be fetched.
     * @return true iff. the WSDL URL in the URL: text field was downloaded successfully.
     */
    public boolean onNextButton() {
        return processWsdlLocation();
    }

    public void storeSettings(Object settings) throws IllegalStateException {
        if (!(settings instanceof ServiceAndAssertion)) {
            throw new IllegalArgumentException();
        }
        try {
            PublishServiceWizard.ServiceAndAssertion
              sa = (PublishServiceWizard.ServiceAndAssertion)settings;
            PublishedService publishedService = sa.getService();

            publishedService.setName(service.getName());
            publishedService.setWsdlUrl(service.getWsdlUrl());
            publishedService.setWsdlXml(service.getWsdlXml());

            sa.getServiceDocuments().clear();
            sa.getServiceDocuments().addAll(serviceDocuments);

            if (sa.getRoutingAssertion() == null) {
                Port port = wsdl.getSoapPort();
                // bugzilla #3539
                // sa.setRoutingAssertion(new HttpRoutingAssertion());
                if (port != null) {
                    String uri = wsdl.getUriFromPort(port);
                    if (uri != null) {
                        if(uri.startsWith("http") || uri.startsWith("HTTP")) {
                             sa.setRoutingAssertion(new HttpRoutingAssertion(uri));
                        } else {
                            String tmp = Wsdl.extractBaseURI(publishedService.getWsdlUrl()) + uri;
                            if (tmp.startsWith("http") || tmp.startsWith("HTTP")) {
                                 sa.setRoutingAssertion(new HttpRoutingAssertion(tmp));
                            }
                        }
                    }
                }
                if (sa.getAssertion() != null && sa.getAssertion().getChildren().isEmpty()) {
                    sa.getAssertion().addChild(sa.getRoutingAssertion());
                }
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "Error storing settings.", e); // this used to do e.printStackTrace() this is slightly better.
        }
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Web Service Description";
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServicePanel.class.getName());

    // local service copy
    private PublishedService service = new PublishedService();
    private Collection<ServiceDocument> serviceDocuments = new ArrayList();
    private WsdlLocationPanel wsdlLocationPanel;
    private Wsdl wsdl;

    /**
     * This method is called from within the constructor to initialize the form.
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        wsdlLocationPanel = new WsdlLocationPanel(getOwner(), logger,
                true, SearchWsdlDialog.uddiEnabled());
        wsdlLocationPanel.addPropertyListener(new PropertyChangeListener(){
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

        final String wsdlUrl = wsdlLocationPanel.getWsdlUrl();
        try {
            wsdl = wsdlLocationPanel.getWsdl();
            if (wsdl != null) {
                final String resolvedDoc = wsdlLocationPanel.getWsdlContent(0);

                final String serviceName = wsdl.getServiceName();
                // if service name not obtained service name is WSDL URL
                if (serviceName == null || "".equals(serviceName)) {
                    service.setName(wsdlUrl);
                } else {
                    service.setName(serviceName);
                }
                service.setWsdlUrl(wsdlUrl.startsWith("http") ? wsdlUrl : null);
                service.setWsdlXml(resolvedDoc);
                serviceDocuments.clear();

                for (int i=1; i<wsdlLocationPanel.getWsdlCount(); i++) {
                    ServiceDocument sd = new ServiceDocument();
                    sd.setUri(wsdlLocationPanel.getWsdlUri(i)); 
                    sd.setContentType("text/xml");
                    sd.setType("WSDL-IMPORT");
                    sd.setContents(wsdlLocationPanel.getWsdlContent(i));
                    serviceDocuments.add(sd);
                }

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
}
