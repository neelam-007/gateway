package com.l7tech.console.panels;

import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListenerAdapter;
import com.l7tech.console.util.QnameValidator;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.xml.XmlElementVerifierConfig;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FullQName;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import static com.l7tech.console.panels.AssertionPropertiesOkCancelSupport.ValidationException;

/**
 * Panel for editing an XmlElementVerifierConfig.
 */
public class XmlElementVerifierConfigPanel extends ValidatedPanel<XmlElementVerifierConfig> {
    private static final Logger logger = Logger.getLogger(XmlElementVerifierConfigPanel.class.getName());

    private JRadioButton certSelectRadioButton;
    private JTextField selectedCertificateNameTextField;
    private JTextField selectedCertificateSubjectTextField;
    private JTextField selectedCertificateIssuerTextField;
    private JButton selectButton;
    private JCheckBox keyInfoOverrideCheckBox;
    private JRadioButton certExpectKeyInfoRadioButton;
    private JRadioButton certLookupRadioButton;
    private JTextField lookupCertificateTextField;
    private JRadioButton certVariableRadioButton;
    private TargetVariablePanel certVariableNameField;
    private JCheckBox customIdAttrCheckBox;
    private JList customIdAttrList;
    private JButton addIdAttrButton;
    private JButton removeIdAttrButton;
    private JPanel contentPane;

    private final XmlElementVerifierConfig model;
    private DefaultListModel customIdAttrListModel;
    private Goid selectedVerifyCertificateOid;

    public XmlElementVerifierConfigPanel(XmlElementVerifierConfig model) {
        super("model");
        this.model = model;
        certVariableNameField.setValueWillBeWritten(false);
        init();
        setData(model);
    }

    @Override
    protected XmlElementVerifierConfig getModel() {
        return model;
    }

    @Override
    protected void initComponents() {
        add(contentPane, BorderLayout.CENTER);

        RunOnChangeListener stateUpdateListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        });

        keyInfoOverrideCheckBox.addActionListener(stateUpdateListener);
        certExpectKeyInfoRadioButton.addActionListener(stateUpdateListener);
        certSelectRadioButton.addActionListener(stateUpdateListener);
        certLookupRadioButton.addActionListener(stateUpdateListener);
        certVariableRadioButton.addActionListener(stateUpdateListener);

        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                doSelectRecipientTrustedCertificate();
            }
        });
        lookupCertificateTextField.getDocument().addDocumentListener(stateUpdateListener);

        Utilities.enableGrayOnDisabled(addIdAttrButton);
        Utilities.enableGrayOnDisabled(removeIdAttrButton);
        Utilities.enableGrayOnDisabled(customIdAttrList);

        customIdAttrListModel = new DefaultListModel();
        customIdAttrList.setModel(customIdAttrListModel);

        addIdAttrButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DialogDisplayer.showInputDialog(addIdAttrButton, "Please enter an ID attribute name as either NAME or {URI}NAME.", "Enter attribute name", JOptionPane.PLAIN_MESSAGE, null, null, null, new DialogDisplayer.InputListener() {
                    @Override
                    public void reportResult(Object option) {
                        if (option != null) {
                            String optionStr = option.toString();

                            // Avoid adding invalid qname
                            FullQName optionQname;
                            try {
                                QnameValidator.validateQname(optionStr);

                                // Omit prefix, if specified, since it is irrelevant for verification purposes
                                FullQName qn = FullQName.valueOf(optionStr);
                                optionQname = new FullQName(qn.getNsUri(), null, qn.getLocal());
                            } catch (ValidationException e) {
                                DialogDisplayer.showMessageDialog(addIdAttrButton, "Invalid ID attribute: " + e.getMessage(), "Invalid ID Attribute Name", JOptionPane.ERROR_MESSAGE, null);
                                return;
                            } catch (ParseException e) {
                                DialogDisplayer.showMessageDialog(addIdAttrButton, "Invalid ID attribute: " + e.getMessage(), "Invalid ID Attribute Name", JOptionPane.ERROR_MESSAGE, null);
                                return;
                            }

                            // Avoid adding an exact duplicate
                            final int modSize = customIdAttrListModel.getSize();
                            for (int i = 0; i < modSize; i++) {
                                if (optionQname.equals(customIdAttrListModel.getElementAt(i))) {
                                    customIdAttrList.setSelectedIndex(i);
                                    return;
                                }
                            }

                            customIdAttrListModel.addElement(optionQname);
                            customIdAttrList.setSelectedIndex(customIdAttrListModel.size() - 1);
                        }
                    }
                });
            }
        });

        removeIdAttrButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int idx = customIdAttrList.getSelectedIndex();
                if (idx < 0 || idx >= customIdAttrListModel.getSize())
                    return;
                customIdAttrListModel.removeElementAt(idx);
            }
        });

        customIdAttrCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (customIdAttrListModel.getSize() < 1) {
                    populateDefaultCustomIdAttrList();
                }
                enableOrDisableComponents();
            }
        });
    }

    @Override
    public void focusFirstComponent() {
    }

    /**
     * Call to ensure context variables known in the policy are available to the TargetVariablePanel
     * @param assertion the assertion being configured
     * @param previousAssertion the previous assertion
     */
    public void setPolicyPosition(Assertion assertion, Assertion previousAssertion) {
        certVariableNameField.setAssertion(assertion, previousAssertion);
    }

    public void setData(XmlElementVerifierConfig model) {
        if (model.getVerifyCertificateGoid() != null) {
            certSelectRadioButton.setSelected(true);
            keyInfoOverrideCheckBox.setSelected(model.isIgnoreKeyInfo());
            selectedVerifyCertificateOid = model.getVerifyCertificateGoid();

            // fill out the selected cert details
            try {
                TrustedCert certificate = Registry.getDefault().getTrustedCertManager().findCertByPrimaryKey(selectedVerifyCertificateOid);
                selectedCertificateNameTextField.setText(certificate == null ? "<Not Found>" : certificate.getName());
                selectedCertificateSubjectTextField.setText(certificate == null ? "<Not Found>" : certificate.getSubjectDn());
                selectedCertificateIssuerTextField.setText(certificate == null ? "<Not Found>" : certificate.getIssuerDn());
                selectedCertificateNameTextField.setCaretPosition(0);
                selectedCertificateSubjectTextField.setCaretPosition(0);
                selectedCertificateIssuerTextField.setCaretPosition(0);
            } catch (FindException e) {
                logger.warning("Could not find the specified certificate in the trust store. " + ExceptionUtils.getMessage(e));
            }

        } else if (model.getVerifyCertificateName() != null && model.getVerifyCertificateName().length() > 0) {
            certLookupRadioButton.setSelected(true);
            selectedVerifyCertificateOid = PersistentEntity.DEFAULT_GOID;
            lookupCertificateTextField.setText(model.getVerifyCertificateName());
            keyInfoOverrideCheckBox.setSelected(model.isIgnoreKeyInfo());
        } else if (model.getVerifyCertificateVariableName() != null && !model.getVerifyCertificateVariableName().isEmpty()) {
            certVariableRadioButton.setSelected(true);
            certVariableNameField.setVariable(model.getVerifyCertificateVariableName());
            selectedVerifyCertificateOid = PersistentEntity.DEFAULT_GOID;
            lookupCertificateTextField.setText(null);
            keyInfoOverrideCheckBox.setSelected(model.isIgnoreKeyInfo());
        } else {
            certExpectKeyInfoRadioButton.setSelected(true);
            selectedVerifyCertificateOid = PersistentEntity.DEFAULT_GOID;
            lookupCertificateTextField.setText(null);
            keyInfoOverrideCheckBox.setSelected(false);
        }

        FullQName[] attrs = model.getCustomIdAttrs();
        customIdAttrListModel = new DefaultListModel();
        if (attrs == null || attrs.length < 1) {
            customIdAttrCheckBox.setSelected(false);
        } else {
            customIdAttrCheckBox.setSelected(true);
            for (FullQName attr : attrs) {
                customIdAttrListModel.addElement(attr);
            }
        }
        customIdAttrList.setModel(customIdAttrListModel);

        enableOrDisableComponents();
    }

    public XmlElementVerifierConfig getData() throws ValidationException {
        doUpdateModel();
        return model;
    }

    @Override
    protected void doUpdateModel() {
        model.setVerifyCertificateName(null);
        model.setVerifyCertificateVariableName(null);
        model.setVerifyCertificateGoid(null);
        model.setIgnoreKeyInfo(keyInfoOverrideCheckBox.isSelected());

        if (certSelectRadioButton.isSelected()) {
            model.setVerifyCertificateGoid(selectedVerifyCertificateOid);
        } else if (certLookupRadioButton.isSelected()) {
            model.setVerifyCertificateName(lookupCertificateTextField.getText().trim());
        } else if (certVariableRadioButton.isSelected()) {
            model.setVerifyCertificateVariableName(certVariableNameField.getVariable());
        } else {
            model.setIgnoreKeyInfo(false);
        }

        if (customIdAttrCheckBox.isSelected() && customIdAttrList.getModel().getSize() > 0) {
            Collection<FullQName> ids = new ArrayList<FullQName>();

            ListModel mod = customIdAttrListModel;
            final int modSize = mod.getSize();
            for (int i = 0; i < modSize; i++) {
                ids.add((FullQName) mod.getElementAt(i));
            }

            model.setCustomIdAttrs(ids.toArray(new FullQName[ids.size()]));
        } else {
            model.setCustomIdAttrs(null);
        }

        validateModel();
    }

    private void validateModel() {
        if (certSelectRadioButton.isSelected() && selectedCertificateNameTextField.getText().isEmpty())
            throw new ValidationException("A certificate must be selected for Use Selected Certificate.");

        if (certLookupRadioButton.isSelected() && lookupCertificateTextField.getText().isEmpty())
            throw new ValidationException("A certificate name must be provided for Lookup Certificate by Name.");

        if (certVariableRadioButton.isSelected() && !certVariableNameField.isEntryValid())
            throw new ValidationException("A valid certificate variable must be provided for Use Certificate from Context Variable.");

        if (customIdAttrCheckBox.isSelected() && customIdAttrList.getModel().getSize() < 1)
            throw new ValidationException("At least one custom attribute ID must be configured for Recognize Only the Following ID Attributes.");
    }

    private void enableOrDisableComponents() {
        keyInfoOverrideCheckBox.setEnabled(certSelectRadioButton.isSelected() || certLookupRadioButton.isSelected() || certVariableRadioButton.isSelected());
        lookupCertificateTextField.setEnabled(certLookupRadioButton.isSelected());
        selectButton.setEnabled(certSelectRadioButton.isSelected());
        certVariableNameField.setEnabled(certVariableRadioButton.isSelected());

        boolean customIds = customIdAttrCheckBox.isSelected();
        customIdAttrList.setEnabled(customIds);
        removeIdAttrButton.setEnabled(customIds);
        addIdAttrButton.setEnabled(customIds);
    }

    private void populateDefaultCustomIdAttrList() {
        for (FullQName defaultIdAttr : XmlElementVerifierConfig.DEFAULT_ID_ATTRS) {
            customIdAttrListModel.addElement(defaultIdAttr);
        }
    }

    private void doSelectRecipientTrustedCertificate() {
        CertSearchPanel sp = new CertSearchPanel(SwingUtilities.getWindowAncestor(this), false, true);
        sp.addCertListener(new CertListenerAdapter() {
            @Override
            public void certSelected(final CertEvent ce) {
                TrustedCert cert = ce.getCert();
                selectedVerifyCertificateOid = cert.getGoid();
                selectedCertificateNameTextField.setText(cert.getName());
                selectedCertificateSubjectTextField.setText(cert.getCertificate().getSubjectDN().toString());
                selectedCertificateIssuerTextField.setText(cert.getCertificate().getIssuerDN().toString());
                selectedCertificateNameTextField.setCaretPosition(0);
                selectedCertificateSubjectTextField.setCaretPosition(0);
                selectedCertificateIssuerTextField.setCaretPosition(0);
                enableOrDisableComponents();
            }
        });
        sp.pack();
        Utilities.centerOnScreen(sp);
        DialogDisplayer.display(sp);
    }
}
