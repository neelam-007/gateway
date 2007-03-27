/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;
import com.l7tech.service.PublishedService;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.EventListener;
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
    private JLabel xmlMessages;
    private JList serverCertList;
    private JButton newServerCertButton;

    private final BridgeRoutingAssertion assertion; // live copy of assertion -- do not write to it until Ok pressed
    private BridgeRoutingAssertion lastRoutingProperties = null; // copy last confirmed by HTTP dialog; all except policy XML is up-to-date

    private final EventListenerList listenerList = new EventListenerList();

    public BridgeRoutingAssertionPropertiesDialog(final Frame owner, BridgeRoutingAssertion a, final PublishedService service) {
        super(owner, true);
        setTitle("Bridge Routing Assertion Properties");
        this.assertion = a;

        setContentPane(rootPanel);

        buttonHttpProperties.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Make a temporary copy for the routing assertion dialog, and remember the changes only if it was Ok'ed
                // (but do not actually copy them into the live assertion until the outer dialog is Ok'ed)  (Bug #3617)
                final BridgeRoutingAssertion editingCopy;
                try {
                    if (lastRoutingProperties == null) lastRoutingProperties = cloneAssertion(assertion);
                    editingCopy = cloneAssertion(lastRoutingProperties);
                } catch (IOException e1) {
                    throw new RuntimeException(e1); // can't happen, but just in case, pass up to error handler
                }

                final HttpRoutingAssertionDialog httpDialog = new HttpRoutingAssertionDialog(owner, editingCopy, service);
                httpDialog.setModal(true);
                httpDialog.pack();

                Utilities.centerOnScreen(httpDialog);
                DialogDisplayer.display(httpDialog, new Runnable() {
                    public void run() {
                        if (httpDialog.isConfirmed())
                            lastRoutingProperties = editingCopy;
                    }
                });
            }
        });

        Utilities.equalizeButtonSizes(new AbstractButton[] { buttonOk, buttonCancel });

        buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copyViewToModel();
                fireEventAssertionChanged(assertion);
                BridgeRoutingAssertionPropertiesDialog.this.dispose();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BridgeRoutingAssertionPropertiesDialog.this.dispose();
            }
        });

        ActionListener updateEnableStates = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateEnableStates();
            }
        };

        rbPolicyAutoDisco.addActionListener(updateEnableStates);
        rbPolicyManual.addActionListener(updateEnableStates);

        rbServerCertAuto.addActionListener(updateEnableStates);
        rbServerCertManual.addActionListener(updateEnableStates);

        Utilities.enableGrayOnDisabled(serverCertList);

        // TODO reenable when it is ready
        rbServerCertAuto.setSelected(true);
        rbServerCertManual.setEnabled(false);
        serverCertList.setEnabled(false);
        newServerCertButton.setEnabled(false);

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

    private static BridgeRoutingAssertion cloneAssertion(BridgeRoutingAssertion assertion) throws IOException {
        return (BridgeRoutingAssertion)WspReader.getDefault().parseStrictly(WspWriter.getPolicyXml(assertion));
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
        // Copy in any edited routing assertion properties, if any
        // Do this first so any stale/bogus BRA-specific properties get overwritten by the rest of this method
        if (lastRoutingProperties != null) assertion.copyFrom(lastRoutingProperties);

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
        listenerList.add(PolicyListener.class, listener);
    }

    private void fireEventAssertionChanged(final Assertion a) {
        if (a == null) return;
        if (a.getParent() == null || a.getParent().getChildren() == null) return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int[] indices = new int[a.getParent().getChildren().indexOf(a)];
                PolicyEvent event = new PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                for (EventListener listener : listeners) {
                    ((PolicyListener) listener).assertionsChanged(event);
                }
            }
        });
    }
}
