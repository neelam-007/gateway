package com.l7tech.console.panels;

import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;

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
    public static final String DEF_PREFIX = "/xml/";
    /**
     * Creates new form WizardPanel
     */
    public NonSoapServicePanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getStepLabel() {
        return "XML Service Information";
    }

    public String getDescription() {
        return "Provide the SecureSpan Gateway basic information about the " +
               "non-soap xml application you are about to publish.<p>" +
               "The field \"Service Name\" lets you define a name to refer to " +
               "this service in the SecureSpan Manager.<p>" +
               "The field \"Target URL\" lets you define an HTTP URL to which " +
               "the SecureSpan Gateway will forward the incoming requests to. " +
               "This can later be changed by editing the resulting policy.<p>The field " +
               "\"SSG URL\" lets you customize the incoming HTTP URL on the SecureSpan Gateway " +
               "where clients will send their XML requests to consume this XML application.";
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        // set the prefix based on what host we are connected to
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        prefixURL.setText(mw.ssgURL() + DEF_PREFIX);
    }

    private void bark(Component control, String msg) {
        JOptionPane.showMessageDialog(control, msg);
    }

    public boolean onNextButton() {
        // make sure a name is provided
        publishedServiceName = serviceName.getText();
        if (publishedServiceName == null || publishedServiceName.length() < 1) {
            String msg = "published service name not set";
            logger.info(msg);
            bark(serviceName, msg);
            return false;
        }
        routingURI = null;
        String tmp = ssgURLSuffix.getText();
        if (tmp == null || tmp.length() < 1) {
            String msg = "must complete the routing URI";
            logger.info(msg);
            bark(ssgURLSuffix, msg);
            return false;
        }
        if (tmp.startsWith("/")) tmp = tmp.substring(1);
        ssgURLSuffix.setText(tmp);
        if (tmp == null || tmp.length() < 1) {
            String msg = "must complete the routing URI";
            logger.info(msg);
            bark(ssgURLSuffix, msg);
            return false;
        } else {
            routingURI = DEF_PREFIX + tmp;
        }
        // check that this is a valid url
        try {
            new URL(prefixURL.getText() + tmp);
        } catch (MalformedURLException e) {
            String msg = prefixURL.getText() + tmp + " is not a valid url. " + e.getMessage();
            logger.info(msg);
            bark(ssgURLSuffix, msg);
            return false;
        }
        downstreamURL = targetURL.getText();
        if (downstreamURL == null || downstreamURL.length() < 1) {
            String msg = "must provide a downstream url";
            logger.info(msg);
            bark(targetURL, msg);
            return false;
        }
        // check that this is a valid url
        try {
            new URL(downstreamURL);
        } catch (MalformedURLException e) {
            String msg = downstreamURL + " is not a valid url. " + e.getMessage();
            logger.info(msg);
            bark(targetURL, msg);
            return false;
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
