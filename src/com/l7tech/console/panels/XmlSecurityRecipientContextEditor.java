/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jan 18, 2005<br/>
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.MainWindow;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;

/**
 * Corresponding GUI for the {@link com.l7tech.console.action.EditXmlSecurityRecipientContextAction} action.
 *
 * @author flascelles@layer7-tech.com
 */
public class XmlSecurityRecipientContextEditor extends JDialog {
    private JPanel mainPanel;
    private JButton assignCertButton;
    private JTextField certSubject;
    private JComboBox actorComboBox;
    private JRadioButton specificRecipientRradio;
    private JRadioButton defaultRadio;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JLabel label2;
    private JLabel label1;
    private JPanel detailsPanel;

    private SecurityHeaderAddressable assertion;
    // key string (actor), value X509Certificate (recipient)
    private HashMap xmlSecRecipientsFromOtherAssertions = new HashMap();
    private String locallyDefinedActor;
    private X509Certificate locallyDefinedRecipient;
    private boolean assertionChanged = false;

    public XmlSecurityRecipientContextEditor(Frame owner, SecurityHeaderAddressable assertion) {
        super(owner, true);
        this.assertion = assertion;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("XML Security Recipient Context");
        ButtonGroup bg = new ButtonGroup();
        bg.add(specificRecipientRradio);
        bg.add(defaultRadio);
        final MainWindow mw = TopComponents.getInstance().getMainWindow();
        String ssghostname = mw.ssgURL();
        int pos = ssghostname.lastIndexOf(':');
        if (pos > 4) {
            ssghostname = ssghostname.substring(0, pos);
        }
        defaultRadio.setText("Default (" + ssghostname + ")");
        setActionListeners();
        setInitialValues();
        enableSpecificControls();
    }

    private void setActionListeners() {
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableSpecificControls();
            }
        };
        specificRecipientRradio.addActionListener(al);
        defaultRadio.addActionListener(al);

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

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        actorComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selecteditem = (String)e.getItem();
                    X509Certificate selectedcert =
                            (X509Certificate)xmlSecRecipientsFromOtherAssertions.get(selecteditem);
                    if (selectedcert == null) {
                        if (selecteditem != locallyDefinedActor) {
                            throw new RuntimeException("actor selected seems to have no matching cert");
                        } else {
                            selectedcert = locallyDefinedRecipient;
                        }
                    }
                    certSubject.setText(selectedcert.getSubjectDN().getName());
                }
            }
        });

        assignCertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                final RecipientSecurityHeaderWizardStep panel3 = new RecipientSecurityHeaderWizardStep(null);
                final CertDetailsPanel panel2 = new CertDetailsPanel(panel3);
                final CertImportMethodsPanel panel1 = new CertImportMethodsPanel(panel2, true);

                panel3.setValidator(new RecipientSecurityHeaderWizardStep.Validator() {
                    public boolean checkData() {
                        // make sure this data does not already exist in another assertion
                        String maybeNewActor = panel3.getCapturedValue();
                        X509Certificate maybeNewCert = panel2.getCert();
                        if (xmlSecRecipientsFromOtherAssertions.containsKey(maybeNewActor)) {
                            JOptionPane.showMessageDialog(assignCertButton, "The actor value " + maybeNewActor +
                                                                            " is already associated to a " +
                                                                            "recipient cert.");
                            return false;
                        } else if (xmlSecRecipientsFromOtherAssertions.containsValue(maybeNewCert)) {
                            String dn = maybeNewCert.getSubjectDN().getName();
                            JOptionPane.showMessageDialog(assignCertButton, "The cert " + dn +
                                                                            " is already associated to an actor.");
                            return false;
                        }
                        return true;
                    }
                });

                JFrame f = TopComponents.getInstance().getMainWindow();
                Wizard w = new AddCertificateWizard(f, panel1);
                w.setTitle("Define new XML security recipient");

                w.addWizardListener(new WizardListener() {
                    public void wizardSelectionChanged(WizardEvent e) {}
                    public void wizardFinished(WizardEvent e) {
                        String maybeNewActor = panel3.getCapturedValue();
                        X509Certificate maybeNewCert = panel2.getCert();
                        locallyDefinedRecipient = maybeNewCert;
                        certSubject.setText(locallyDefinedRecipient.getSubjectDN().getName());
                        locallyDefinedActor = maybeNewActor;
                        ((DefaultComboBoxModel)actorComboBox.getModel()).addElement(locallyDefinedActor);
                        ((DefaultComboBoxModel)actorComboBox.getModel()).setSelectedItem(locallyDefinedActor);

                    }
                    public void wizardCanceled(WizardEvent e) {}
                });
                w.pack();
                w.setSize(800, 560);
                Utilities.centerOnScreen(w);
                w.setVisible(true);
            }
        });
    }

    private void populateRecipientsFromAssertionTree(Assertion toInspect) {
        if (toInspect == assertion) {
            return; // skip us
        } else if (toInspect instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)toInspect;
            for (Iterator i = ca.children(); i.hasNext();) {
                Assertion a = (Assertion)i.next();
                populateRecipientsFromAssertionTree(a);
            }
        } else if (toInspect instanceof SecurityHeaderAddressable) {
            SecurityHeaderAddressable xsecass = (SecurityHeaderAddressable)toInspect;
            if (!xsecass.getRecipientContext().localRecipient()) {
                String existingactor = xsecass.getRecipientContext().getActor();
                if (!xmlSecRecipientsFromOtherAssertions.containsKey(existingactor)) {
                    X509Certificate existingcert = null;
                    try {
                        existingcert = CertUtils.decodeCert(HexUtils.decodeBase64(
                                                            xsecass.getRecipientContext().getBase64edX509Certificate(), true));
                    } catch (CertificateException e) {
                        throw new RuntimeException(e); // should not happen
                    } catch (IOException e) {
                        throw new RuntimeException(e); // should not happen
                    }
                    xmlSecRecipientsFromOtherAssertions.put(existingactor, existingcert);
                }
            }
        }
    }

    private void setInitialValues() {

        // get to root of policy
        Assertion root = (Assertion)assertion;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        populateRecipientsFromAssertionTree(root);
        for (Iterator i = xmlSecRecipientsFromOtherAssertions.keySet().iterator(); i.hasNext();) {
            String actorvalue = (String)i.next();
            ((DefaultComboBoxModel)actorComboBox.getModel()).addElement(actorvalue);
        }

        if (assertion.getRecipientContext().localRecipient()) {
            specificRecipientRradio.setSelected(false);
            defaultRadio.setSelected(true);
            setFirstExistingRecipient();
        } else {
            specificRecipientRradio.setSelected(true);
            defaultRadio.setSelected(false);
            if (!xmlSecRecipientsFromOtherAssertions.containsKey(assertion.getRecipientContext().getActor())) {
                locallyDefinedActor = assertion.getRecipientContext().getActor();
                try {
                    locallyDefinedRecipient = CertUtils.decodeCert(
                                                HexUtils.decodeBase64(
                                                        assertion.getRecipientContext().getBase64edX509Certificate(), true));

                } catch (CertificateException e) {
                    throw new RuntimeException(e); // should not happen
                } catch (IOException e) {
                    throw new RuntimeException(e); // should not happen
                }
                certSubject.setText(locallyDefinedRecipient.getSubjectDN().getName());
                ((DefaultComboBoxModel)actorComboBox.getModel()).addElement(locallyDefinedActor);
                ((DefaultComboBoxModel)actorComboBox.getModel()).setSelectedItem(locallyDefinedActor);
            } else {
                setFirstExistingRecipient();
            }
        }
    }

    private void setFirstExistingRecipient() {
        String initialActorValue = null;
        for (Iterator i = xmlSecRecipientsFromOtherAssertions.keySet().iterator(); i.hasNext();) {
            initialActorValue = (String)i.next();
            break;
        }
        if (initialActorValue != null) {
            X509Certificate initialcertvalue = (X509Certificate)xmlSecRecipientsFromOtherAssertions.
                                                                    get(initialActorValue);
            certSubject.setText(initialcertvalue.getSubjectDN().getName());
            ((DefaultComboBoxModel)actorComboBox.getModel()).setSelectedItem(initialActorValue);
        }
    }

    private void enableSpecificControls() {
        if (specificRecipientRradio.isSelected()) {
            assignCertButton.setEnabled(true);
            certSubject.setEnabled(true);
            actorComboBox.setEnabled(true);
            label2.setEnabled(true);
            label1.setEnabled(true);
            TitledBorder border = ((TitledBorder)detailsPanel.getBorder());
            border.setTitleColor(Color.BLACK);
            detailsPanel.repaint();
        } else {
            assignCertButton.setEnabled(false);
            certSubject.setEnabled(false);
            actorComboBox.setEnabled(false);
            label2.setEnabled(false);
            label1.setEnabled(false);
            TitledBorder border = ((TitledBorder)detailsPanel.getBorder());
            border.setTitleColor(Color.GRAY);
            detailsPanel.repaint();
        }
    }

    private void ok() {
        // remember value
        if (specificRecipientRradio.isSelected()) {
            XmlSecurityRecipientContext newRecipientContext = new XmlSecurityRecipientContext();
            newRecipientContext.setActor((String)actorComboBox.getSelectedItem());
            X509Certificate cert = null;
            if (newRecipientContext.getActor().equals(locallyDefinedActor)) {
                cert = locallyDefinedRecipient;
            } else {
                cert = (X509Certificate)xmlSecRecipientsFromOtherAssertions.get(newRecipientContext.getActor());
            }
            try {
                newRecipientContext.setBase64edX509Certificate(HexUtils.encodeBase64(cert.getEncoded(), true));
            } catch (CertificateEncodingException e) {
                throw new RuntimeException("could not encode cert", e);
            }
            assertion.setRecipientContext(newRecipientContext);
        } else {
            assertion.setRecipientContext(XmlSecurityRecipientContext.getLocalRecipient());
        }
        assertionChanged = true;
        // split!
        XmlSecurityRecipientContextEditor.this.dispose();
    }

    /**
     * @return true if the assertion was changed by this dialog
     */
    public boolean hasAssertionChanged() {
        return assertionChanged;
    }

    private void help() {
        Actions.invokeHelp(XmlSecurityRecipientContextEditor.this);
    }

    private void cancel() {
        XmlSecurityRecipientContextEditor.this.dispose();
    }
}
