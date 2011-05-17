package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListenerAdapter;
import com.l7tech.console.panels.CertSearchPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
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

public class NonSoapVerifyElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapVerifyElementAssertion> {

    private static final Logger logger = Logger.getLogger(NonSoapVerifyElementAssertionPropertiesDialog.class.getName());

    // For now, we have nothing to configure except the XPath, so we just let our superclass handle everything
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel contentPane;
    private JRadioButton certSelectRadioButton;
    private JRadioButton certLookupRadioButton;
    private JTextField lookupCertificateTextField;
    private JTextField selectedCertificateNameTextField;
    private JTextField selectedCertificateSubjectTextField;
    private JTextField selectedCertificateIssuerTextField;
    private JButton selectButton;
    private JCheckBox keyInfoOverrideCheckBox;
    private JRadioButton certExpectKeyInfoRadioButton;
    private JCheckBox customIdAttrCheckBox;
    private JList customIdAttrList;  // TODO replace with nice table, but not until we addIdAttrButton to use a nice custom add dialog instead of JOptionPane
    private JButton addIdAttrButton;
    private JButton removeIdAttrButton;

    private DefaultListModel customIdAttrListModel;

    private long selectedVerifyCertificateOid;

    public NonSoapVerifyElementAssertionPropertiesDialog(Window owner, NonSoapVerifyElementAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
        getXpathExpressionLabel().setText("dsig:Signature element(s) to verify XPath:");

        // create extra panel components -- copied from WsSecurityAssertion
        getControlsBelowXpath().setLayout(new BorderLayout());
        getControlsBelowXpath().add(createExtraPanel(), BorderLayout.CENTER);
    }

    protected JPanel createExtraPanel() {

        RunOnChangeListener stateUpdateListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                updateState();
            }
        });

        keyInfoOverrideCheckBox.addActionListener(stateUpdateListener);
        certExpectKeyInfoRadioButton.addActionListener(stateUpdateListener);
        certSelectRadioButton.addActionListener(stateUpdateListener);
        certLookupRadioButton.addActionListener(stateUpdateListener);

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
                updateState();
            }
        });

        return contentPane;
    }

    private void populateDefaultCustomIdAttrList() {
        for (FullQName defaultIdAttr : NonSoapVerifyElementAssertion.DEFAULT_ID_ATTRS) {
            customIdAttrListModel.addElement(defaultIdAttr);
        }
    }

    private void doSelectRecipientTrustedCertificate() {
        CertSearchPanel sp = new CertSearchPanel(this, false, true);
        sp.addCertListener(new CertListenerAdapter() {
            @Override
            public void certSelected(final CertEvent ce) {
                TrustedCert cert = ce.getCert();
                selectedVerifyCertificateOid = cert.getOid();
                selectedCertificateNameTextField.setText(cert.getName());
                selectedCertificateSubjectTextField.setText(cert.getCertificate().getSubjectDN().toString());
                selectedCertificateIssuerTextField.setText(cert.getCertificate().getIssuerDN().toString());
                selectedCertificateNameTextField.setCaretPosition(0);
                selectedCertificateSubjectTextField.setCaretPosition(0);
                selectedCertificateIssuerTextField.setCaretPosition(0);
                updateState();
            }
        });
        sp.pack();
        Utilities.centerOnScreen(sp);
        DialogDisplayer.display(sp);
    }

    @Override
    public void setData(NonSoapVerifyElementAssertion assertion) {
        super.setData(assertion);

        if (assertion.getVerifyCertificateOid() > 0L) {
            certSelectRadioButton.setSelected(true);
            keyInfoOverrideCheckBox.setSelected(assertion.isIgnoreKeyInfo());
            selectedVerifyCertificateOid = assertion.getVerifyCertificateOid();

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

        } else if (assertion.getVerifyCertificateName() != null && assertion.getVerifyCertificateName().length() > 0) {
            certLookupRadioButton.setSelected(true);
            selectedVerifyCertificateOid = -1;
            lookupCertificateTextField.setText(assertion.getVerifyCertificateName());
            keyInfoOverrideCheckBox.setSelected(assertion.isIgnoreKeyInfo());

        } else {
            certExpectKeyInfoRadioButton.setSelected(true);
            selectedVerifyCertificateOid = -1;
            lookupCertificateTextField.setText(null);
            keyInfoOverrideCheckBox.setSelected(false);
        }

        FullQName[] attrs = assertion.getCustomIdAttrs();
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

        updateState();
    }

    @Override
    public NonSoapVerifyElementAssertion getData(NonSoapVerifyElementAssertion assertion) throws ValidationException {
        assertion = super.getData(assertion);

        if (certSelectRadioButton.isSelected()) {
            assertion.setVerifyCertificateOid(selectedVerifyCertificateOid);
            assertion.setIgnoreKeyInfo(keyInfoOverrideCheckBox.isSelected());

        } else if (certLookupRadioButton.isSelected()) {
            assertion.setVerifyCertificateName(lookupCertificateTextField.getText().trim());
            assertion.setVerifyCertificateOid(-1);
            assertion.setIgnoreKeyInfo(keyInfoOverrideCheckBox.isSelected());

        } else {
            assertion.setVerifyCertificateName(null);
            assertion.setVerifyCertificateOid(-1);
        }

        if (customIdAttrCheckBox.isSelected() && customIdAttrList.getModel().getSize() > 0) {
            Collection<FullQName> ids = new ArrayList<FullQName>();

            ListModel mod = customIdAttrListModel;
            final int modSize = mod.getSize();
            for (int i = 0; i < modSize; i++) {
                ids.add((FullQName) mod.getElementAt(i));
            }

            assertion.setCustomIdAttrs(ids.toArray(new FullQName[ids.size()]));
        } else {
            assertion.setCustomIdAttrs(null);
        }

        return assertion;
    }

    /**
     * Update the state of any/all components based on UI events
     */
    @Override
    protected void updateState() {
        keyInfoOverrideCheckBox.setEnabled(certSelectRadioButton.isSelected() || certLookupRadioButton.isSelected());
        lookupCertificateTextField.setEnabled(certLookupRadioButton.isSelected());
        selectButton.setEnabled(certSelectRadioButton.isSelected());

        boolean customIds = customIdAttrCheckBox.isSelected();
        customIdAttrList.setEnabled(customIds);
        removeIdAttrButton.setEnabled(customIds);
        addIdAttrButton.setEnabled(customIds);

        boolean canOk =
                (certSelectRadioButton.isSelected() && !selectedCertificateNameTextField.getText().isEmpty()) ||
                        (certLookupRadioButton.isSelected() && !lookupCertificateTextField.getText().isEmpty()) ||
                        (certExpectKeyInfoRadioButton.isSelected()) && inputsValid();

        if (customIds && customIdAttrList.getModel().getSize() < 1)
            canOk = false;

        getOkButton().setEnabled(!isReadOnly() && canOk);
    }
}
