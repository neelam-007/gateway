package com.l7tech.console.panels;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.ValidatorUtils;
import com.l7tech.gui.FilterDocument;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Wizard panel that allows the publication of a non-soap xml service.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 14, 2004<br/>
 * $Id$<br/>
 */
public class NonSoapServicePanel extends WizardStepPanel {
    /**
     * Creates new form WizardPanel
     */
    public NonSoapServicePanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getStepLabel() {
        return "Service Information";
    }

    public String getDescription() {
        return "Specify the connection and routing information for the non-SOAP application.";
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        // set the prefix based on what host we are connected to
        String hostname = TopComponents.getInstance().ssgURL().getHost();
        String ssgUrl = "http(s)://" + hostname + ":[port]/";
        prefixURL.setText(ssgUrl);

        ssgURLSuffix.setDocument(new FilterDocument(127, null));
    }

    private void bark(Component control, String msg) {
        JOptionPane.showMessageDialog(control, msg, "Information Missing", JOptionPane.WARNING_MESSAGE);
    }

    public boolean onNextButton() {
        // make sure a name is provided
        publishedServiceName = serviceName.getText();
        if (publishedServiceName == null || publishedServiceName.length() < 1) {
            String msg = "Published Service name is missing.";
            logger.info(msg);
            bark(serviceName, msg);
            return false;
        }
        routingURI = null;
        String tmp = ssgURLSuffix.getText();
        if (tmp == null || tmp.length() < 1) {
            String msg = "Routing URI is not complete.";
            logger.info(msg);
            bark(ssgURLSuffix, msg);
            return false;
        }
        if (tmp.startsWith("/")) tmp = tmp.substring(1);
        ssgURLSuffix.setText(tmp);
        if (tmp.length() < 1 || tmp.equals("")) {
            String msg = "Routing URI is not complete.";
            logger.info(msg);
            bark(ssgURLSuffix, msg);
            return false;
        } else if (("/" + tmp).startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
            String msg = "Routing URI cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX;
            logger.info(msg);
            bark(ssgURLSuffix, msg);
            return false;
        } else {
            routingURI = "/" + tmp;
        }
        // check that this is a valid url
        final String testUrl = "http://" + TopComponents.getInstance().ssgURL().getHost() + ":8080/" + tmp;
        try {
            new URL(testUrl);
        } catch (MalformedURLException e) {
            String msg = testUrl + " is not a valid url. " + e.getMessage();
            logger.info(msg);
            bark(ssgURLSuffix, msg);
            return false;
        }
        downstreamURL = targetURL.getText();
        if (downstreamURL == null || downstreamURL.length() < 1) {
            // empty is ok
            downstreamURL = null;
        } else {
            // check that this is a valid url; if it does not contain context variables
            if (!downstreamURL.contains("${")) {
                try {
                    new URL(downstreamURL);
                } catch (MalformedURLException e) {
                    String msg = downstreamURL + " is not a valid url. " + e.getMessage();
                    logger.info(msg);
                    bark(targetURL, msg);
                    return false;
                }
            }
        }
        return true;
    }

    public String getPublishedServiceName() {
        return publishedServiceName;
    }

    public String getRoutingURI() {
        return routingURI;
    }

    public String getDownstreamURL() {
        return downstreamURL;
    }

    private JPanel mainPanel;
    private JTextField serviceName;
    private JTextField targetURL;
    private JTextField ssgURLSuffix;
    private JLabel prefixURL;
    private String publishedServiceName = null;
    private String routingURI = null;
    private String downstreamURL = null;
    private Logger logger = Logger.getLogger(NonSoapServicePanel.class.getName());
}
