/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jan 24, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.util.TopComponents;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.FilterDocument;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * A dialog to view/edit the routing URI parameter of a soap web service.
 *
 * @author flascelles@layer7-tech.com
 */
public class SoapServiceRoutingURIEditor extends JDialog {
    private JTextField uriField;
    private JRadioButton customURIRadio;
    private JRadioButton noURIRadio;
    private JButton okbutton;
    private JButton cancelbutton;
    private JButton helpbutton;
    private JPanel mainPanel;
    private JEditorPane routingURL;

    private PublishedService subject;
    private boolean subjectAffected = false;
    private String ssgURL;


    public SoapServiceRoutingURIEditor(Frame owner, PublishedService svc) {
        super(owner, true);
        subject = svc;
        initialize();
    }

    public SoapServiceRoutingURIEditor(Dialog owner, PublishedService svc) {
        super(owner, true);
        subject = svc;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Web Service Routing URI");

        subjectAffected = false;
        ButtonGroup bg = new ButtonGroup();
        uriField.setDocument(new FilterDocument(128, null));
        bg.add(customURIRadio);
        bg.add(noURIRadio);
        setActionListeners();
        setInitialValues();
        enableSpecificControls();
        updateURL();
    }

    private void setInitialValues() {
        String hostname = TopComponents.getInstance().ssgURL().getHost();
        // todo, we need to be able to query gateway to get port instead of assuming default
        ssgURL = "http://" + hostname + ":8080";        

        String existinguri = subject.getRoutingUri();
        if (subject.isInternal()) {
            noURIRadio.setEnabled(false);
            customURIRadio.setSelected(true);
            uriField.setEnabled(true);
            uriField.setText(existinguri);
        } else {
            if (existinguri == null) {
                noURIRadio.setSelected(true);
                customURIRadio.setSelected(false);
            } else {
                noURIRadio.setSelected(false);
                customURIRadio.setSelected(true);
                uriField.setText(existinguri);
            }
        }
    }

    private void updateURL() {
        String currentValue = null;
        if (customURIRadio.isSelected()) {
            currentValue = uriField.getText();
        }
        String urlvalue;
        if (currentValue == null || currentValue.length() < 1) {
            urlvalue = ssgURL + "/ssg/soap";
        } else {
            if (currentValue.startsWith("/")) {
                urlvalue = ssgURL + currentValue;
            } else {
                urlvalue = ssgURL + "/" + currentValue;
            }
        }

        routingURL.setText("<html><a href=\"" + urlvalue + "\">" + urlvalue + "</a></html>");
    }

    private void setActionListeners() {
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableSpecificControls();
            }
        };
        customURIRadio.addActionListener(al);
        noURIRadio.addActionListener(al);

        okbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        helpbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

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

                updateURL();
            }

            @Override
            public void keyTyped(KeyEvent e) {}
        });

        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        Utilities.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

    }

    private void enableSpecificControls() {
        if (customURIRadio.isSelected()) {
            uriField.setEnabled(true);
            updateURL();
        } else {
            uriField.setEnabled(false);
            updateURL();
        }
    }

    private void help() {
        Actions.invokeHelp(SoapServiceRoutingURIEditor.this);
    }

    private void cancel() {
        SoapServiceRoutingURIEditor.this.dispose();
    }

    public boolean wasSubjectAffected() {
        return subjectAffected;
    }

    private void ok() {
        String currentValue = null;
        if (customURIRadio.isSelected()) {
            currentValue = uriField.getText();
            if (currentValue == null) {
                currentValue = "";
            }
            if (!currentValue.startsWith("/")) {
                currentValue = "/" + currentValue;
                uriField.setText(currentValue);
            }
        }
        if (currentValue == null) {
            subject.setRoutingUri(null);
        } else if (currentValue.length() < 2) {
            JOptionPane.showMessageDialog(this, "URI cannot be empty");
            return;
        } else if (currentValue.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
            JOptionPane.showMessageDialog(this, "URI cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX);
            return;
        } else {
            try {
                new URL(ssgURL + currentValue);
            } catch (MalformedURLException e) {
                JOptionPane.showMessageDialog(this, ssgURL + currentValue + " is not a valid URL");
                return;
            }
            subject.setRoutingUri(currentValue);
        }
        subjectAffected = true;
        SoapServiceRoutingURIEditor.this.dispose();
    }
}
