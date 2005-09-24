/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.gui.widgets;

import com.l7tech.common.License;
import com.l7tech.common.util.DateUtils;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Panel that displays License details.
 */
public class LicensePanel extends JPanel {
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

    private JLabel licenseIdField;
    private JLabel descriptionField;
    private JLabel issuerField;
    private JLabel licenseeField;
    private JLabel contactEmailField;
    private JLabel startField;
    private JLabel expiresField;
    private JPanel grantsPanel;

    private boolean validLicense = false;

    private final JLabel defaultLabel = new JLabel("blah");

    private JLabel[] licenseLabels = new JLabel[] {
            licenseIdLabel,
            descriptionLabel,
            issuerLabel,
            licenseeLabel,
            contactEmailLabel,
            startLabel,
            expiresLabel,
            grantsLabel,
    };

    private JLabel[] licenseFields = new JLabel[] {
            licenseIdField,
            descriptionField,
            issuerField,
            licenseeField,
            contactEmailField,
            startField,
            expiresField,
    };

    public LicensePanel(String gatewayName) {
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
                issuerLabel,
                licenseeLabel,
                contactEmailLabel,
                startLabel,
                expiresLabel,
                grantsLabel,
        });
        setLicense(null);
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
     * Display information about the specified license in the panel.
     *
     * @param license the license to display, or null to display "No license".
     */
    public void setLicense(License license) {
        hideErrorFields();
        if (license == null) {
            setLicenseFieldsVisible(false);
            statusField.setText("None Installed");
            statusField.setOpaque(defaultLabel.isOpaque());
            statusField.setForeground(defaultLabel.getForeground());
            statusField.setBackground(defaultLabel.getBackground());
            statusField.setBorder(null);
            validLicense = false;
            return;
        }

        setLicenseFieldsVisible(true);
        statusField.setText("      Valid      ");
        statusField.setOpaque(true);
        statusField.setForeground(Color.BLACK);
        statusField.setBackground(new Color(128, 255, 128)); // bright green
        statusField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        validLicense = true;

        licenseIdField.setText(String.valueOf(license.getId()));
        descriptionField.setText(n(license.getDescription()));
        final X509Certificate issuer = license.getTrustedIssuer();
        issuerField.setText(issuer == null ? "<None -- no trusted license signature>" : n(issuer.getSubjectDN().getName()));
        licenseeField.setText(n(license.getLicenseeName()));
        contactEmailField.setText(n(license.getLicenseeContactEmail()));
        displayStartDate(startField, license);
        displayExpiryDate(license);

        grantsPanel.removeAll();
        grantsPanel.setLayout(new BoxLayout(grantsPanel, BoxLayout.Y_AXIS));
        final String grants = license.getGrants();
        grantsPanel.add(new JScrollPane(new WrappingLabel(grants, (grants.length() / 70) + 1),
                                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    }

    private void displayExpiryDate(License license) {
        final Date date = license.getExpiryDate();
        String m = DateUtils.makeRelativeDateMessage(date, false);
        m = m != null && m.length() > 0 ? " (" + m + ")" : m;
        expiresField.setText(n(date == null ? null : date.toString()) + m);
    }

    /** Clears license fields and sets their visibility. */
    private void setLicenseFieldsVisible(boolean visibility) {
        for (int i = 0; i < licenseLabels.length; i++) {
            JLabel licenseLabel = licenseLabels[i];
            licenseLabel.setVisible(visibility);
        }

        for (int i = 0; i < licenseFields.length; i++) {
            JLabel licenseField = licenseFields[i];
            licenseField.setVisible(visibility);
            licenseField.setText("");
        }

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

        setLicenseFieldsVisible(false);

        licenseErrorsLabel.setVisible(true);
        statusField.setText("  INVALID LICENSE  ");
        statusField.setOpaque(true);
        statusField.setForeground(Color.WHITE);
        statusField.setBackground(new Color(255, 92, 92)); // dark red
        statusField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        validLicense = false;

        WrappingLabel errorsLabel = new WrappingLabel(errorText, (errorText.length() / 68) + 1);
        errorsLabel.setContextMenuEnabled(true);
        errorsLabel.setContextMenuAutoSelectAll(true);
        JScrollPane sp = new JScrollPane(errorsLabel,
                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        licenseErrorsPanel.add(sp);
    }

    /** @return true if this panel is displaying a valid license. */
    public boolean isValidLicense() {
        return validLicense;
    }
}
