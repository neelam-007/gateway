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
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.TextUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.event.WizardEvent;

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
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Corresponding GUI for the {@link com.l7tech.console.action.EditXmlSecurityRecipientContextAction} action.
 *
 * @author flascelles@layer7-tech.com
 */
public class XmlSecurityRecipientContextEditor extends JDialog {
    private static final Logger logger = Logger.getLogger( XmlSecurityRecipientContextEditor.class.getName() );

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
    private boolean readonly;

    public XmlSecurityRecipientContextEditor(Frame owner, SecurityHeaderAddressable assertion, boolean readonly) {
        super(owner, true);
        this.assertion = assertion;
        this.readonly = readonly;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Change WSS Recipient");
        ButtonGroup bg = new ButtonGroup();
        bg.add(specificRecipientRradio);
        bg.add(defaultRadio);
        String ssghostname = TopComponents.getInstance().ssgURL().getHost();
        defaultRadio.setText("Default (" + ssghostname + ")");
        setActionListeners();
        setInitialValues();
        enableSpecificControls();

        okButton.setEnabled(!readonly);
    }

    private void setActionListeners() {
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableSpecificControls();
            }
        };
        specificRecipientRradio.addActionListener(al);
        defaultRadio.addActionListener(al);

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
                    String selecteditem = (String)actorComboBox.getSelectedItem();
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
                Frame f = TopComponents.getInstance().getTopParent();
                final Wizard wizard = new AddCertificateWizard(f, panel1);

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
                            String msg = "The cert " + dn + " is already associated to an actor.";
                            // Preset the max columns of the output message
                            int maxcol = 80;
                            // Figure out how many lines there are if using the above maxcol
                            int maxline = msg.length() / maxcol + (msg.length() % maxcol > 0? 1 : 0);
                            JOptionPane.showMessageDialog(assignCertButton, TextUtils.wrapString(msg, maxcol, maxline, "\n"));
                            return false;
                        }
                        return true;
                    }

                    public void checkFinishButtonActivation() {
                        boolean enabled = panel3.getCapturedValue().trim().length() != 0;
                        wizard.getButtonFinish().setEnabled(enabled);
                    }
                });

                wizard.setTitle("New WSS Recipient Wizard");
                wizard.addWizardListener(new WizardListener() {
                    public void wizardSelectionChanged(WizardEvent e) {}
                    public void wizardFinished(WizardEvent e) {
                        String maybeNewActor = panel3.getCapturedValue();
                        X509Certificate maybeNewCert = panel2.getCert();
                        locallyDefinedRecipient = maybeNewCert;
                        certSubject.setText(locallyDefinedRecipient.getSubjectDN().getName());
                        locallyDefinedActor = maybeNewActor;
                        ((DefaultComboBoxModel)actorComboBox.getModel()).addElement(locallyDefinedActor);
                        (actorComboBox.getModel()).setSelectedItem(locallyDefinedActor);
                        xmlSecRecipientsFromOtherAssertions.put(locallyDefinedActor, locallyDefinedRecipient);
                    }
                    public void wizardCanceled(WizardEvent e) {}
                });
                wizard.pack();
                Utilities.centerOnScreen(wizard);
                DialogDisplayer.display(wizard, new Runnable() {
                    public void run() {
                        XmlSecurityRecipientContextEditor.this.pack();
                    }
                });
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
        populateRecipientsFromAssertionTree(inlineIncludes(root));
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
                }
                certSubject.setText(locallyDefinedRecipient.getSubjectDN().getName());
                ((DefaultComboBoxModel)actorComboBox.getModel()).addElement(locallyDefinedActor);
                if (!xmlSecRecipientsFromOtherAssertions.containsKey(locallyDefinedActor)) {
                    xmlSecRecipientsFromOtherAssertions.put(locallyDefinedActor, locallyDefinedRecipient);
                }
                ((DefaultComboBoxModel)actorComboBox.getModel()).setSelectedItem(locallyDefinedActor);
            } else {
                setFirstExistingRecipient();
            }
        }
    }

    private Assertion inlineIncludes( final Assertion subject ) {
        Assertion assertion = subject;

        try {
            assertion = Registry.getDefault().getPolicyPathBuilderFactory().makePathBuilder().inlineIncludes( subject, new HashSet<String>(), true );
        } catch (InterruptedException e) {
            // fallback to policy without includes
            logger.log( Level.WARNING, "Error inlining inlcluded policy fragments.", e );
        } catch (PolicyAssertionException e) {
            // fallback to policy without includes
            logger.log( Level.WARNING, "Error inlining inlcluded policy fragments.", e );
        }

        return assertion;
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
            assignCertButton.setEnabled(!readonly);
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
            if (actorComboBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(okButton, "You must choose an existing recipient or define a new one.",
                                              "Invalid Selection", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String actor = (String) actorComboBox.getSelectedItem();
            X509Certificate cert;
            if (actor.equals(locallyDefinedActor)) {
                cert = locallyDefinedRecipient;
            } else {
                cert = (X509Certificate)xmlSecRecipientsFromOtherAssertions.get(actor);
            }
            XmlSecurityRecipientContext newRecipientContext;
            try {
                newRecipientContext = new XmlSecurityRecipientContext( actor, HexUtils.encodeBase64(cert.getEncoded(), true) );
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
