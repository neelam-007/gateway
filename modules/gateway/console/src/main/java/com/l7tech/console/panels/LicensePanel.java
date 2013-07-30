/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.gateway.common.License;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.WrappingLabel;
import com.l7tech.util.DateUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

/**
 * Panel that displays License details.
 */
public class LicensePanel extends JPanel {
    public static final String DEFAULT_STATUS_NONE = "None Installed";
    public static final String DEFAULT_STATUS_INVALID = "  INVALID LICENSE  ";
    public static final String DEFAULT_STATUS_UNSIGNED = "    Unsigned    ";
    public static final String DEFAULT_STATUS_VALID = "      Valid      ";

    private JPanel rootPanel;
    private JLabel ssgField;
    private JLabel statusLabel;
    private JLabel statusField;
    private JLabel licenseErrorsLabel;
    private JPanel licenseErrorsPanel;

    private JLabel licenseIdLabel;
    private JLabel descriptionLabel;
    private JLabel issuerLabel;
    private JLabel licenseeLabel;
    private JLabel contactEmailLabel;
    private JLabel startLabel;
    private JLabel expiresLabel;
    private JLabel grantsLabel;
    private JLabel eulaLabel;

    private JLabel licenseIdField;
    private JLabel descriptionField;
    private JLabel issuerField;
    private JLabel licenseeField;
    private JLabel contactEmailField;
    private JLabel startField;
    private JLabel expiresField;
    private JPanel grantsPanel;
    private JLabel eulaField;
    private JButton eulaButton;
    private JLabel licAttrLabel;
    private JTextArea attrTextArea;

    private String statusNone = DEFAULT_STATUS_NONE;
    private String statusInvalid = DEFAULT_STATUS_INVALID;
    private String statusUnsigned = DEFAULT_STATUS_UNSIGNED;
    private String statusValid = DEFAULT_STATUS_VALID;

    private final boolean showEulaInfo;

    private License license = null;
    private boolean validLicense = false;

    private final JLabel defaultLabel = new JLabel("blah");

    private JLabel[] licenseLabels = new JLabel[] {
            licenseIdLabel,
            descriptionLabel,
            licAttrLabel,
            issuerLabel,
            licenseeLabel,
            contactEmailLabel,
            startLabel,
            expiresLabel,
            grantsLabel,
    };

    private JComponent[] licenseFields = new JComponent[] {
            licenseIdField,
            descriptionField,
            attrTextArea,
            issuerField,
            licenseeField,
            contactEmailField,
            startField,
            expiresField,
    };

    public LicensePanel(String gatewayName, boolean showEulaInfo) {
        this.showEulaInfo = showEulaInfo;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(rootPanel);
        init();
        ssgField.setText(gatewayName);
    }

    private void init() {
        Utilities.equalizeComponentWidth(new JComponent[] {
                statusLabel,
                licenseErrorsLabel,
                licenseIdLabel,
                descriptionLabel,
                licAttrLabel,
                issuerLabel,
                licenseeLabel,
                contactEmailLabel,
                startLabel,
                expiresLabel,
                grantsLabel,
                eulaLabel
        });
        if (!showEulaInfo) {
            eulaLabel.setVisible(false);
            eulaField.setVisible(false);
            eulaButton.setVisible(false);
        }

        setLicense(null);
    }

    /**
     * Add an AcitonListener to invoke if the "View EULA" button is pressed.
     *
     * @param listener  listener to invoke. Required.
     */
    public void addEulaButtonActionListener(ActionListener listener) {
        eulaButton.addActionListener(listener);
    }

    /**
     * Remove a previously-added ActionListener from the "View EULA" button.
     *
     * @param listener  the listener to remove.  Required.
     */
    public void removeEulaButtonActionListener(ActionListener listener) {
        eulaButton.removeActionListener(listener);
    }

    /**
     * Get the status message displayed when the license is null.
     * @return the status message displayed when the license is null.
     */
    public String getStatusNone() {
        return statusNone;
    }

    /**
     * Change the status message displayed when the license is null.
     * @param statusNone the new status message to display when the license is null.
     */
    public void setStatusNone(String statusNone) {
        this.statusNone = statusNone;
    }

    /**
     * Get the status message displayed when a license error message is set.
     * @return the status message displayed when a license error message is set.
     */
    public String getStatusInvalid() {
        return statusInvalid;
    }

    /**
     * Change the status message displayed when a license error message is set.
     * @param statusInvalid the new status message displayed when a license error message is set.
     */
    public void setStatusInvalid(String statusInvalid) {
        this.statusInvalid = statusInvalid;
    }

    /**
     * Get the status message displayed when a license is valid but is not signed.
     * @return the status message displayed when a license is valid but is not signed.
     */
    public String getStatusUnsigned() {
        return statusUnsigned;
    }

    /**
     * Change the status message displayed when a license is valid but is not signed.
     * @param statusUnsigned the new status message displayed when a license is valid but is not signed.
     */
    public void setStatusUnsigned(String statusUnsigned) {
        this.statusUnsigned = statusUnsigned;
    }

    /**
     * Get the status message displayed when a license is valid and properly signed.
     * @return the status message displayed when a license is valid and properly signed.
     */
    public String getStatusValid() {
        return statusValid;
    }

    /**
     * Set the status message displayed when a license is valid and properly signed.
     * @param statusValid the new status message displayed when a license is valid and properly signed.
     */
    public void setStatusValid(String statusValid) {
        this.statusValid = statusValid;
    }

    private String n(String s) {
        return s == null ? "" : s;
    }

    private void displayStartDate(JLabel label, License license) {
        Date date = license.getStartDate();

        if (date == null) {
            label.setText("");
            label.setVisible(false);
            return;
        }

        String m = DateUtils.makeRelativeDateMessage(date, true);
        m = m != null && m.length() > 0 ? " (" + m + ")" : m;
        label.setText(date.toString() + m);
        label.setVisible(true);
    }

    /**
     * Get the last license that was displayed, if a valid license is currently being displayed.
     *
     * @return the last license passed to setLicense(), or null if we are displaying an error message or an empty license.
     */
    public License getLicense() {
        return this.license;
    }

    /**
     * Display information about the specified license in the panel.
     *
     * @param license the license to display, or null to display "No license".
     */
    public void setLicense(License license) {
        this.license = license;

        hideErrorFields();
        if (license == null) {
            setLicenseFieldsVisible(false);
            statusField.setText(statusNone);
            statusField.setOpaque(defaultLabel.isOpaque());
            statusField.setForeground(defaultLabel.getForeground());
            statusField.setBackground(defaultLabel.getBackground());
            statusField.setBorder(null);
            validLicense = false;
            return;
        }

        setLicenseFieldsVisible(true);
        if (license.isValidSignature()) {
            // Valid and signed
            statusField.setText(statusValid);
            statusField.setOpaque(true);
            statusField.setForeground(Color.BLACK);
            statusField.setBackground(new Color(128, 255, 128)); // bright green
            statusField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        } else {
            // Valid, but not signed
            statusField.setText(statusUnsigned);
            statusField.setOpaque(true);
            statusField.setForeground(Color.BLACK);
            statusField.setBackground(new Color(240, 255, 240)); // very light green, almost white
            statusField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        }
        validLicense = true;

        licenseIdField.setText(String.valueOf(license.getId()));
        descriptionField.setText(n(license.getDescription()));
        final X509Certificate issuer = license.getTrustedIssuer();
        issuerField.setText(issuer == null ? "<No trusted issuer signature>" : n(issuer.getSubjectDN().getName()));
        licenseeField.setText(n(license.getLicenseeName()));
        contactEmailField.setText(n(license.getLicenseeContactEmail()));
        displayStartDate(startField, license);
        displayExpiryDate(license);
        displayLicenseAttributes(license);

        grantsPanel.removeAll();
        grantsPanel.setLayout(new BoxLayout(grantsPanel, BoxLayout.Y_AXIS));
        final String grants = license.getGrants();
        grantsPanel.add(new JScrollPane(new WrappingLabel(grants, (grants.length() / 70) + 2),
                                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        boolean customEula = null != license.getEulaText();
        String eula = customEula ? "<Custom>" : null;
        if (eula == null) eula = "<None>";
        eulaField.setText(eula);
    }

    private void displayExpiryDate( License license) {
        final Date date = license.getExpiryDate();
        String m = DateUtils.makeRelativeDateMessage(date, false);
        m = m != null && m.length() > 0 ? " (" + m + ")" : m;
        expiresField.setText(n(date == null ? null : date.toString()) + m);
    }

    /**
     * Form a displaying message containing all selected license attributes and display
     * it in the attribute text area in the license panel.
     * @param license: the license containing all license infomation.
     */
    private void displayLicenseAttributes(License license) {
        Set<String> attrList = license.getAttributes();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String attr: attrList) {
            sb.append(attr);
            if (i++ < attrList.size() - 1) {
                sb.append("\n");
            }
        }
        attrTextArea.setFont(licAttrLabel.getFont());
        attrTextArea.setText(sb.toString());
    }

    /* Clears license fields and sets their visibility. */
    private void setLicenseFieldsVisible(boolean visibility) {
        for (JLabel licenseLabel : licenseLabels) {
            licenseLabel.setVisible(visibility);
        }

        for (JComponent licenseField : licenseFields) {
            licenseField.setVisible(visibility);
            if (licenseField instanceof JLabel) {
                ((JLabel)licenseField).setText("");
            } else if (licenseField instanceof JTextArea) {
                ((JTextArea)licenseField).setText("");
            }
        }

        boolean eulaVis = visibility && showEulaInfo;
        eulaLabel.setVisible(eulaVis);
        eulaField.setVisible(eulaVis);
        eulaField.setText("");
        eulaButton.setVisible(eulaVis);

        grantsPanel.removeAll();
        grantsPanel.setVisible(visibility);
    }

    /** Clears error fields and hides them. */
    private void hideErrorFields() {
        licenseErrorsPanel.removeAll();
        licenseErrorsPanel.setLayout(new BoxLayout(licenseErrorsPanel, BoxLayout.Y_AXIS));
        licenseErrorsLabel.setVisible(false);
    }

    /**
     * Display information about the specified error.
     * @param errorText error text to display, or null to hide error text fields.
     */
    public void setLicenseError(String errorText) {
        licenseErrorsPanel.removeAll();
        licenseErrorsPanel.setLayout(new BoxLayout(licenseErrorsPanel, BoxLayout.Y_AXIS));

        // Caller probably meant to reset view
        if (errorText == null) {
            setLicense(null);
            return;
        }

        this.license = null;

        setLicenseFieldsVisible(false);

        licenseErrorsLabel.setVisible(true);
        statusField.setText(statusInvalid);
        statusField.setOpaque(true);
        statusField.setForeground(Color.WHITE);
        statusField.setBackground(new Color(255, 92, 92)); // dark red
        statusField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        validLicense = false;

        WrappingLabel errorsLabel = new WrappingLabel(errorText, (errorText.length() / 70) + 2);
        errorsLabel.setContextMenuEnabled(true);
        errorsLabel.setContextMenuAutoSelectAll(true);
        JScrollPane sp = new JScrollPane(errorsLabel,
                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        licenseErrorsPanel.add(sp);
        licenseErrorsPanel.validate();
    }

    /** @return true if this panel is displaying a valid license. */
    public boolean isValidLicense() {
        return validLicense;
    }
}
