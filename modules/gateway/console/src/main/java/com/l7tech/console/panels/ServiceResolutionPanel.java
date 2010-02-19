package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.FilterDocument;

import javax.swing.*;
import java.awt.event.*;
import java.util.ResourceBundle;

/**
 * A wizard step panel configures a service resolution path.
 *
 * @author ghuang
 */
public class ServiceResolutionPanel extends WizardStepPanel {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.ServiceResolutionPanel");
    private static final String STD_PORT = ":8080";
    private static final String STD_PORT_DISPLAYED = ":[port]";

    private JPanel mainPanel;
    private JRadioButton noURIRadio;
    private JRadioButton customURIRadio;
    private JTextField uriField;
    private JEditorPane ssgRoutingUrlPane;

    private String ssgUrl;
    private PublishServiceWizard.ServiceAndAssertion subject;

    public ServiceResolutionPanel() {
        super(null);
        initialize();
    }

    @Override
    public String getDescription() {
        return resources.getString("step.description");
    }

    @Override
    public String getStepLabel() {
        return resources.getString("step.label");
    }

    @Override
    public boolean onNextButton() {
        if (subject != null) {
            subject.setRoutingUrl(null);
            if (customURIRadio.isSelected()) {
                String routingUrl = uriField.getText().trim();
                if (! routingUrl.isEmpty()) {
                    subject.setRoutingUrl(routingUrl);
                }
            }
        }
        return true;
    }

    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof PublishServiceWizard.ServiceAndAssertion) {
            subject = (PublishServiceWizard.ServiceAndAssertion)settings;
        }
    }

    private void initialize() {
        String hostname = TopComponents.getInstance().ssgURL().getHost();
        ssgUrl = "http://" + hostname + STD_PORT;

        // By default, the radio button 'No resolution path' is on, so uriField is disabled.
        uriField.setEnabled(false);

        ActionListener toggleUriField = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                uriField.setEnabled(customURIRadio.isSelected());
                updateUrl();
            }
        };
        noURIRadio.addActionListener(toggleUriField);
        customURIRadio.addActionListener(toggleUriField);

        uriField.setDocument(new FilterDocument(128, null));
        uriField.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                //always start with "/" for URI except an empty uri.
                String uri = uriField.getText();
                if (uri != null && !uri.isEmpty() && !uri.startsWith("/")) {
                    uri = "/" + uri.trim();
                    uriField.setText(uri);
                }

                updateUrl();
            }

            @Override
            public void keyTyped(KeyEvent e) {}
        });
        uriField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {}
            @Override
            public void focusLost(FocusEvent e) {
                if (customURIRadio.isSelected()) {
                    String url = updateUrl();
                    if (url != null && !url.startsWith("/")) url = "/" + url;
                    uriField.setText(url);
                }
            }
        });

        // The ssgRoutingUrlPane should be initialized.
        updateUrl();

        add(mainPanel);
    }

    private String updateUrl() {
        String currentValue = null;
        if (customURIRadio.isSelected()) {
            currentValue = uriField.getText();
        }
        String urlValue;
        if (currentValue != null) {
            currentValue = currentValue.trim();
            String cvWithoutSlashes = currentValue.replace("/", "");
            if (cvWithoutSlashes.length() <= 0) {
                currentValue = null;
            }
        }
        if (currentValue == null || currentValue.length() < 1) {
            urlValue = ssgUrl + "/ssg/soap";
        } else {
            if (currentValue.startsWith("/")) {
                urlValue = ssgUrl + currentValue;
            } else {
                urlValue = ssgUrl + "/" + currentValue;
            }
        }

        String tmp = urlValue.replace(STD_PORT, STD_PORT_DISPLAYED);
        tmp = tmp.replace("http://", "http(s)://");
        ssgRoutingUrlPane.setText("<html><a href=\"" + urlValue + "\">" + tmp + "</a></html>");
        ssgRoutingUrlPane.setCaretPosition(0);
        return currentValue;
    }
}
