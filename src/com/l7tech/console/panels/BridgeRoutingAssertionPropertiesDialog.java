/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;
import com.l7tech.service.PublishedService;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allows properties of a {@link com.l7tech.policy.assertion.BridgeRoutingAssertion} to be edited.
 */
public class BridgeRoutingAssertionPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(BridgeRoutingAssertionPropertiesDialog.class.getName());

    private JPanel rootPanel;
    private JRadioButton rbPolicyAutoDisco;
    private JRadioButton rbPolicyManual;
    private JTextArea policyXmlText;
    private JButton buttonCancel;
    private JButton buttonOk;
    private JButton buttonHttpProperties;
    private JRadioButton rbServerCertAuto;
    private JRadioButton rbServerCertManual;

    private final HttpRoutingAssertionDialog httpDialog;
    private final BridgeRoutingAssertion assertion;
    private JLabel xmlMessages;
    private JButton serverCertChangeButton;
    private JLabel serverCertLabel;

    public BridgeRoutingAssertionPropertiesDialog(Frame owner, BridgeRoutingAssertion a, PublishedService service) {
        super(owner, true);
        setTitle("Bridge Routing Assertion Properties");
        this.assertion = a;

        setContentPane(rootPanel);

        httpDialog = new HttpRoutingAssertionDialog(owner, a, service);
        httpDialog.setModal(true);
        httpDialog.pack();
        Utilities.centerOnScreen(httpDialog);

        buttonHttpProperties.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DialogDisplayer.display(httpDialog);
            }
        });

        Utilities.equalizeButtonSizes(new AbstractButton[] { buttonOk, buttonCancel });

        buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copyViewToModel();

                httpDialog.fireEventAssertionChanged(assertion);
                BridgeRoutingAssertionPropertiesDialog.this.dispose();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                httpDialog.dispose();
                BridgeRoutingAssertionPropertiesDialog.this.dispose();
            }
        });

        ActionListener updateEnableStates = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateEnableStates();
            }
        };

        ButtonGroup bg2 = new ButtonGroup();
        bg2.add(rbPolicyAutoDisco);
        bg2.add(rbPolicyManual);
        rbPolicyAutoDisco.addActionListener(updateEnableStates);
        rbPolicyManual.addActionListener(updateEnableStates);

        ButtonGroup bg3 = new ButtonGroup();
        bg3.add(rbServerCertAuto);
        bg3.add(rbServerCertManual);
        rbServerCertAuto.addActionListener(updateEnableStates);
        rbServerCertManual.addActionListener(updateEnableStates);

        // TODO reenable when it is ready
        rbServerCertManual.setEnabled(false);
        serverCertChangeButton.setEnabled(false);
        serverCertLabel.setText(" ");

        Utilities.enableGrayOnDisabled(policyXmlText);

        final Utilities.DefaultContextMenuFactory cmf =
                new Utilities.DefaultContextMenuFactory() {
                    public JPopupMenu createContextMenu(final JTextComponent tc) {
                        final JPopupMenu m = super.createContextMenu(tc);
                        if (tc.isEditable()) {
                            m.add(new JSeparator());
                            JMenuItem reformXml = new JMenuItem("Reformat All XML");
                            reformXml.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    String xml = tc.getText();
                                    try {
                                        tc.setText(XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(xml)));
                                        xmlMessages.setText("");
                                    } catch (IOException e1) {
                                        logger.log(Level.SEVERE, "Unable to reformat XML", e1); // Can't happen
                                    } catch (SAXException e1) {
                                        logger.log(Level.SEVERE, "Unable to reformat XML", e1); // Oh well
                                        xmlMessages.setText("XML is not well-formed");
                                    }
                                }
                            });
                            m.add(reformXml);
                        }
                        return m;
                    }
                };
        policyXmlText.addMouseListener(Utilities.createContextMenuMouseListener(policyXmlText, cmf));

        copyModelToView();
        updateEnableStates();
    }

    /** Update the GUI state to reflect the policy assertion settings. */
    private void copyModelToView() {
        // Update GUI state
        String policyXml = assertion.getPolicyXml();
        rbPolicyAutoDisco.setSelected(policyXml == null);
        rbPolicyManual.setSelected(policyXml != null);
        policyXmlText.setText(policyXml != null ? policyXml : "");

        // TODO String serverCert = assertion.getServerCertBase64();
        // TODO rbServerCertAuto.setSelected(serverCert == null);
        // TODO rbServerCertManual.setSelected(serverCert != null);
        // TODO serverCertText.setText(serverCert != null ? serverCert : "");
    }

    /** Update the policy assertion settings to reflect the GUI state. */
    private void copyViewToModel() {
        // Populate policy assertion settings
        if (rbPolicyManual.isSelected())
            assertion.setPolicyXml(policyXmlText.getText());
        else
            assertion.setPolicyXml(null);

        // TODO if (rbServerCertManual.isSelected())
// TODO             assertion.setServerCertBase64(serverCertText.getText());
// TODO         else
// TODO             assertion.setServerCertBase64(null);
    }

    private void updateEnableStates() {
        policyXmlText.setEnabled(rbPolicyManual.isSelected());
    }

    public void addPolicyListener(PolicyListener listener) {
        httpDialog.addPolicyListener(listener);
    }

}
