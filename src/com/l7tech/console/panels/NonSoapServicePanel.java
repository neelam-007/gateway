package com.l7tech.console.panels;

import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;
import java.net.URL;
import java.net.MalformedURLException;

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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(3, 3, new Insets(8, 8, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Service name:");
        mainPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("SSG URL:");
        label2.setToolTipText("The URL on the SSG where this will be routed");
        mainPanel.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("Target URL:");
        mainPanel.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        serviceName = new JTextField();
        serviceName.setToolTipText("The name of the service as it appears in the manager");
        mainPanel.add(serviceName, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        targetURL = new JTextField();
        targetURL.setToolTipText("The URL where the SSG will route these xml requests");
        mainPanel.add(targetURL, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        ssgURLSuffix = new JTextField();
        mainPanel.add(ssgURLSuffix, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        prefixURL = new JLabel();
        prefixURL.setText("https://ssg.acme.com:8443/ssg/soap");
        prefixURL.setToolTipText("");
        mainPanel.add(prefixURL, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
    }
}
