package com.l7tech.console.panels;

import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.util.logging.Logger;
import java.util.ResourceBundle;
import java.util.Locale;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertionDialog extends JDialog {
    static final Logger log = Logger.getLogger(RequestSwAAssertionDialog.class.getName());
    private RequestSwAAssertion assertion;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;
    private JComboBox bindingsListComboxBox;
    private JScrollPane operationsScrollPane;
    private JScrollPane multipartScrollPane;
    private EventListenerList listenerList = new EventListenerList();

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RequestSwAPropertiesDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(RequestSwAAssertionDialog.class.getName());

    public RequestSwAAssertionDialog(JFrame parent, RequestSwAAssertion assertion) {
        super(parent, resources.getString("window.title"), true);
        this.assertion = assertion;
        initialize();
        pack();
        Utilities.centerOnScreen(this);
    }

    private void initialize() {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton});

    }

}
