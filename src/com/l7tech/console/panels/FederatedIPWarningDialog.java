package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.event.WizardEvent;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.EventListener;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class FederatedIPWarningDialog extends JDialog {

    private JPanel mainPanel;
    private JPanel msgPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel itemsPanel;
    private JLabel headerLabel1;
    private JLabel headerLabel2;
    private JLabel headerLabel3;

    private EventListenerList listenerList = new EventListenerList();

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.FederatedIdentityProviderDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(FederatedIPWarningDialog.class.getName());

    public FederatedIPWarningDialog(JDialog parent, JPanel itemsPanel) {
        super(parent, resources.getString("no.trusted.cert.warning.dialog.title"), true);
        this.itemsPanel = itemsPanel;
        initialize();
        pack();
        Utilities.centerOnScreen(this);
    }

    private void initialize() {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        msgPanel.add(itemsPanel);

        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fireWizardFinished();
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fireWizardCanceled();
                dispose();
            }
        });

        addWindowListener(new WindowAdapter() { 
            public void windowClosing(WindowEvent e) {
                 fireWizardCanceled();
                 dispose();
             }
        });

        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton});
        headerLabel1.setFont(new java.awt.Font("Dialog", 1, 12));
        headerLabel2.setFont(new java.awt.Font("Dialog", 1, 12));
        headerLabel3.setFont(new java.awt.Font("Dialog", 1, 12));
    }

    /**
     * add the WizardListener
     *
     * @param listener the WizardListener
     */
    public void addWizardListener(WizardListener listener) {
        listenerList.add(WizardListener.class, listener);
    }

    /**
     * remove the the WizardListener
     *
     * @param listener the WizardListener
     */
    public void removeWizardListener(WizardListener listener) {
        listenerList.remove(WizardListener.class, listener);
    }

    /**
     * Notifies all the listeners that the wizard has been canceled
     */
    private void fireWizardCanceled() {
        EventListener[] listeners = listenerList.getListeners(WizardListener.class);
        WizardEvent we = new WizardEvent(this, WizardEvent.CANCELED);
        for (int i = 0; i < listeners.length; i++) {
            ((WizardListener)listeners[i]).wizardCanceled(we);
        }
    }

    /**
     * Notifies all the listeners that the wizard has been canceled
     */
    private void fireWizardFinished() {
        EventListener[] listeners = listenerList.getListeners(WizardListener.class);
        WizardEvent we = new WizardEvent(this, WizardEvent.FINISHED);
        for (int i = 0; i < listeners.length; i++) {
            ((WizardListener)listeners[i]).wizardFinished(we);
        }
    }

}
