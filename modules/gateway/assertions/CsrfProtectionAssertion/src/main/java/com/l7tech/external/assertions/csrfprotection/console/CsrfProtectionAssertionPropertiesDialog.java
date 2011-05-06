package com.l7tech.external.assertions.csrfprotection.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.csrfprotection.CsrfProtectionAssertion;
import com.l7tech.external.assertions.csrfprotection.HttpParameterType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * User: njordan
 * Date: 17-Feb-2011
 */
public class CsrfProtectionAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<CsrfProtectionAssertion> {
    private JPanel mainPanel;
    private JCheckBox enableDoubleSubmitCookieCheckBox;
    private JLabel cookieNameLabel;
    private JTextField cookieNameField;
    private JLabel parameterNameLabel;
    private JTextField httpParameterNameField;
    private JComboBox httpParameterTypeComboBox;
    private JCheckBox enableHttpRefererValidationCheckBox;
    private JLabel refererOptionsLabel;
    private JCheckBox allowEmptyValuesCheckBox;
    private JLabel validDomainsLabel;
    private JComboBox validDomainsComboBox;
    private JList validDomainsList;
    private DefaultListModel validDomainsListModel;
    private JButton addDomainButton;
    private JButton editDomainButton;
    private JButton removeDomainButton;

    public CsrfProtectionAssertionPropertiesDialog(Frame parent, CsrfProtectionAssertion assertion) {
        super(CsrfProtectionAssertion.class, parent, assertion.getPropertiesDialogTitle(), true);

        initComponents();
        setData(assertion);
    }

    @Override
    protected void initComponents() {
        enableDoubleSubmitCookieCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                enableDisableDoubleSubmitCookieControls();
            }
        });

        httpParameterTypeComboBox.setModel(new DefaultComboBoxModel(new HttpParameterType[] {HttpParameterType.GET, HttpParameterType.POST, HttpParameterType.GET_AND_POST}));

        enableHttpRefererValidationCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                enableDisableHttpRefererControls();
            }
        });

        validDomainsComboBox.setModel(new DefaultComboBoxModel(new String[] {"Current Domain", "List of Trusted Domains"}));
        validDomainsComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                enableDisableHttpRefererControls();
            }
        });

        validDomainsListModel = new DefaultListModel();
        validDomainsList.setModel(validDomainsListModel);
        validDomainsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        addDomainButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                String value = JOptionPane.showInputDialog(CsrfProtectionAssertionPropertiesDialog.this, "Enter a Domain Value", "");
                if(value != null) {
                    validDomainsListModel.addElement(value);
                }
            }
        });

        editDomainButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(validDomainsList.getSelectedIndex() == -1) {
                    return;
                }

                String value = (String)validDomainsList.getSelectedValue();
                value = JOptionPane.showInputDialog(CsrfProtectionAssertionPropertiesDialog.this, "Enter a Domain Value", value);
                if(value != null) {
                    validDomainsListModel.set(validDomainsList.getSelectedIndex(), value);
                }
            }
        });

        removeDomainButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(validDomainsList.getSelectedIndex() == -1) {
                    return;
                }

                validDomainsListModel.remove(validDomainsList.getSelectedIndex());
            }
        });

        super.initComponents();
    }

    private void enableDisableDoubleSubmitCookieControls() {
        cookieNameLabel.setEnabled(enableDoubleSubmitCookieCheckBox.isSelected());
        cookieNameField.setEnabled(enableDoubleSubmitCookieCheckBox.isSelected());
        parameterNameLabel.setEnabled(enableDoubleSubmitCookieCheckBox.isSelected());
        httpParameterNameField.setEnabled(enableDoubleSubmitCookieCheckBox.isSelected());
        httpParameterTypeComboBox.setEnabled(enableDoubleSubmitCookieCheckBox.isSelected());
    }

    private void enableDisableHttpRefererControls() {
        refererOptionsLabel.setEnabled(enableHttpRefererValidationCheckBox.isSelected());
        allowEmptyValuesCheckBox.setEnabled(enableHttpRefererValidationCheckBox.isSelected());
        validDomainsLabel.setEnabled(enableHttpRefererValidationCheckBox.isSelected());
        validDomainsComboBox.setEnabled(enableHttpRefererValidationCheckBox.isSelected());
        validDomainsList.setEnabled(enableHttpRefererValidationCheckBox.isSelected() && validDomainsComboBox.getSelectedIndex() > 0);
        addDomainButton.setEnabled(enableHttpRefererValidationCheckBox.isSelected() && validDomainsComboBox.getSelectedIndex() > 0);
        editDomainButton.setEnabled(enableHttpRefererValidationCheckBox.isSelected() && validDomainsComboBox.getSelectedIndex() > 0);
        removeDomainButton.setEnabled(enableHttpRefererValidationCheckBox.isSelected() && validDomainsComboBox.getSelectedIndex() > 0);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    public void setData(CsrfProtectionAssertion assertion) {
        enableDoubleSubmitCookieCheckBox.setSelected(assertion.isEnableDoubleSubmitCookieChecking());
        cookieNameField.setText(assertion.getCookieName() == null ? "" : assertion.getCookieName());
        httpParameterNameField.setText(assertion.getParameterName() == null ? "" : assertion.getParameterName());
        httpParameterTypeComboBox.setSelectedItem(assertion.getParameterType() == null ? HttpParameterType.POST : assertion.getParameterType());
        enableDisableDoubleSubmitCookieControls();

        enableHttpRefererValidationCheckBox.setSelected(assertion.isEnableHttpRefererChecking());
        allowEmptyValuesCheckBox.setSelected(assertion.isAllowMissingOrEmptyReferer());
        validDomainsComboBox.setSelectedIndex(assertion.isOnlyAllowCurrentDomain() ? 0 : 1);
        validDomainsListModel.removeAllElements();
        for(String trustedDomain : assertion.getTrustedDomains()) {
            validDomainsListModel.addElement(trustedDomain);
        }
        enableDisableHttpRefererControls();
    }

    @Override
    public CsrfProtectionAssertion getData(CsrfProtectionAssertion assertion) throws ValidationException {
        if(!enableDoubleSubmitCookieCheckBox.isSelected() && !enableHttpRefererValidationCheckBox.isSelected()) {
            throw new ValidationException("Neither double submit cookie or HTTP-Referer validation is enabled.");
        }

        if(enableDoubleSubmitCookieCheckBox.isSelected()) {
            if(cookieNameField.getText().trim().isEmpty()) {
                throw new ValidationException("The Cookie Name field was empty.");
            }

            if(httpParameterNameField.getText().trim().isEmpty()) {
                throw new ValidationException("The HTTP Parameter field was empty.");
            }
        }

        if(enableHttpRefererValidationCheckBox.isSelected() && validDomainsComboBox.getSelectedIndex() == 1 && validDomainsListModel.size() == 0) {
            throw new ValidationException("The list of trusted domains is empty.");
        }

        assertion.setEnableDoubleSubmitCookieChecking(enableDoubleSubmitCookieCheckBox.isSelected());
        assertion.setCookieName(cookieNameField.getText().trim());
        assertion.setParameterName(httpParameterNameField.getText().trim());
        assertion.setParameterType((HttpParameterType)httpParameterTypeComboBox.getSelectedItem());

        assertion.setEnableHttpRefererChecking(enableHttpRefererValidationCheckBox.isSelected());
        assertion.setAllowMissingOrEmptyReferer(allowEmptyValuesCheckBox.isSelected());
        assertion.setOnlyAllowCurrentDomain(validDomainsComboBox.getSelectedIndex() == 0 ? true : false);
        java.util.List<String> domains;
        if (validDomainsListModel == null) {
            domains = new ArrayList<String>();
        } else {
            domains = new ArrayList<String>(validDomainsListModel.size());
            for(int i = 0;i < validDomainsListModel.size();i++) {
                domains.add((String)validDomainsListModel.getElementAt(i));
            }
        }
        assertion.setTrustedDomains(domains);

        return assertion;
    }
}
