/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jan 24, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.service.PublishedService;
import com.l7tech.console.action.Actions;
import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.HashSet;

/**
 * A dialog to view/edit the routing URI parameter of a soap web service.
 *
 * @author flascelles@layer7-tech.com
 */
public class SoapServiceRoutingURIEditor extends JDialog {
    private JTextField uriField;
    private JRadioButton customURIRadio;
    private JRadioButton noURIRadio;
    private JLabel routingURL;
    private JButton okbutton;
    private JButton cancelbutton;
    private JButton helpbutton;
    private JPanel mainPanel;
    private JCheckBox rbMethodPost;
    private JCheckBox rbMethodGet;
    private JCheckBox rbMethodPut;
    private JCheckBox rbMethodDelete;

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
        bg.add(customURIRadio);
        bg.add(noURIRadio);
        setActionListeners();
        setInitialValues();
        enableSpecificControls();
        updateURL();
    }

    private void setInitialValues() {
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        ssgURL = mw.ssgURL();
        if (!ssgURL.startsWith("http://")) {
            ssgURL = "http://" + ssgURL;
        }
        int pos = ssgURL.lastIndexOf(':');
        if (pos > 4) {
            ssgURL = ssgURL.substring(0, pos);
            // todo, we need to be able to query gateway to get port instead of assuming default
            ssgURL = ssgURL + ":8080";
        } else {
            if (ssgURL.endsWith("/") || ssgURL.endsWith("\\")) {
                // todo, we need to be able to query gateway to get port instead of assuming default
                ssgURL = ssgURL.substring(0, ssgURL.length()-1) + ":8080";
            } else {
                // todo, we need to be able to query gateway to get port instead of assuming default
                ssgURL = ssgURL + ":8080";
            }
        }
        String existinguri = subject.getRoutingUri();
        if (existinguri == null) {
            noURIRadio.setSelected(true);
            customURIRadio.setSelected(false);
        } else {
            noURIRadio.setSelected(false);
            customURIRadio.setSelected(true);
            uriField.setText(existinguri.substring(PublishedService.ROUTINGURI_PREFIX.length()));
        }

        rbMethodPost.setSelected(subject.isMethodAllowed("POST"));
        rbMethodGet.setSelected(subject.isMethodAllowed("GET"));
        rbMethodPut.setSelected(subject.isMethodAllowed("PUT"));
        rbMethodDelete.setSelected(subject.isMethodAllowed("DELETE"));
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
            urlvalue = ssgURL + PublishedService.ROUTINGURI_PREFIX + currentValue;
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
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                updateURL();
            }
            public void keyTyped(KeyEvent e) {}
        });

        Actions.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        Actions.setEnterAction(this, new AbstractAction() {
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
        }
        if (currentValue == null || currentValue.length() < 1) {
            subject.setRoutingUri(null);
        } else {
            subject.setRoutingUri(PublishedService.ROUTINGURI_PREFIX + currentValue);
        }
        Set methods = new HashSet();
        if (rbMethodPost.isSelected()) methods.add("POST");
        if (rbMethodGet.isSelected()) methods.add("GET");
        if (rbMethodPut.isSelected()) methods.add("PUT");
        if (rbMethodDelete.isSelected()) methods.add("DELETE");
        subject.setHttpMethods(methods);
        subjectAffected = true;
        SoapServiceRoutingURIEditor.this.dispose();
    }
}
